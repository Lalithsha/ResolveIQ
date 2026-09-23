package com.resolveiq.orchestration.application.action;

import com.resolveiq.orchestration.adapter.out.identity.SimulatedIdentityAdapter;
import com.resolveiq.orchestration.adapter.out.payment.SimulatedPaymentAdapter;
import com.resolveiq.orchestration.application.action.policy.ActionPolicyEngine;
import com.resolveiq.orchestration.domain.model.action.*;
import com.resolveiq.orchestration.domain.repository.*;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class ResolutionActionBenchmarkTest {

    @Test
    void reconcilesEverySuccessfulActionAndSuppressesEveryDuplicateRetry() throws Exception {
        int scenarios = 100;
        int reconciled = 0;
        int duplicateRetriesSuppressed = 0;
        int invalidActionsDenied = 0;
        long started = System.nanoTime();

        for (int index = 0; index < scenarios; index++) {
            Outcome outcome = index % 2 == 0 ? runRefund(index) : runUnlock(index);
            reconciled += outcome.reconciled() ? 1 : 0;
            duplicateRetriesSuppressed += outcome.duplicateSuppressed() ? 1 : 0;
        }
        for (int index = 0; index < scenarios; index++) {
            invalidActionsDenied += invalidActionIsDenied(index) ? 1 : 0;
        }

        long durationMs = (System.nanoTime() - started) / 1_000_000;
        assertThat(reconciled).isEqualTo(scenarios);
        assertThat(duplicateRetriesSuppressed).isEqualTo(scenarios);
        assertThat(invalidActionsDenied).isEqualTo(scenarios);

        String reportPath = System.getProperty("resolveiq.benchmark.report");
        if (reportPath != null && !reportPath.isBlank()) {
            String json = """
                {
                  "generatedAtUtc": "%s",
                  "providerMode": "simulated-payment-and-identity-adapters",
                  "successfulExecutionScenarios": %d,
                  "reconciledScenarios": %d,
                  "reconciliationRate": %.2f,
                  "duplicateRetryScenarios": %d,
                  "duplicateRetriesWithoutRepeatedEffect": %d,
                  "duplicateEffectPreventionRate": %.2f,
                  "invalidActionScenarios": %d,
                  "invalidActionsDeniedBeforeApproval": %d,
                  "invalidActionDenialRate": %.2f,
                  "durationMs": %d,
                  "limitations": "Fault-simulation benchmark with mocked persistence repositories; not production traffic or a human approval-time study."
                }
                """.formatted(Instant.now(), scenarios, reconciled, reconciled / (double) scenarios,
                scenarios, duplicateRetriesSuppressed, duplicateRetriesSuppressed / (double) scenarios,
                scenarios, invalidActionsDenied, invalidActionsDenied / (double) scenarios, durationMs);
            Path output = Path.of(reportPath);
            Files.createDirectories(output.toAbsolutePath().getParent());
            Files.writeString(output, json);
        }
    }

    private boolean invalidActionIsDenied(int index) {
        ActionContext context = new ActionContext(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
            UUID.randomUUID(), "BENCHMARK", Set.of());
        ValidationResult validation;
        ActionType actionType;
        if (index < 50) {
            var action = new RefundDuplicateChargeAction(new SimulatedPaymentAdapter(
                mock(SimulatedPaymentRepository.class), mock(SimulatedPaymentOperationRepository.class)));
            var input = new RefundDuplicateChargeAction.RefundInput(
                "customer", "payment", "USD", 2_500L, "DUPLICATE_CHARGE", "original");
            int failure = index % 6;
            CurrentState state = switch (failure) {
                case 0 -> CurrentState.notFound();
                case 2 -> refundState(true, "EUR", 10_000L, "original");
                case 4 -> refundState(true, "USD", 1_000L, "original");
                default -> refundState(failure != 1, "USD", 10_000L, failure == 5 ? "different" : "original");
            };
            if (failure == 3) {
                input = new RefundDuplicateChargeAction.RefundInput(
                    "customer", "payment", "USD", 0L, "DUPLICATE_CHARGE", "original");
            }
            validation = action.validate(context, input, state);
            actionType = ActionType.REFUND_DUPLICATE_CHARGE;
        } else {
            var action = new UnlockAccountAction(new SimulatedIdentityAdapter(
                mock(SimulatedAccountRepository.class), mock(SimulatedAccountOperationRepository.class)));
            var input = new UnlockAccountAction.UnlockInput(UUID.randomUUID(), "recovery", true);
            int failure = index % 4;
            CurrentState state = switch (failure) {
                case 0 -> CurrentState.notFound();
                case 1 -> accountState(false, Instant.now());
                case 2 -> accountState(true, null);
                default -> accountState(true, Instant.now().minusSeconds(25 * 60 * 60));
            };
            validation = action.validate(context, input, state);
            actionType = ActionType.UNLOCK_ACCOUNT;
        }
        var policy = new ActionPolicyEngine().evaluate(
            actionType, validation.riskLevel(), java.util.Map.of(), validation.isValid(), validation.reasonCodes());
        return !validation.isValid() && policy.decision() == PolicyDecision.DENIED;
    }

    private CurrentState refundState(boolean settled, String currency, long available, String duplicateOf) {
        return CurrentState.found("1", java.util.Map.of(
            "isSettled", settled, "currency", currency, "availableRefundableCents", available,
            "duplicateOfReference", duplicateOf));
    }

    private CurrentState accountState(boolean locked, Instant verifiedAt) {
        return CurrentState.found("1", java.util.Map.of(
            "isLocked", locked, "lastVerifiedIdentityAt", verifiedAt == null ? "" : verifiedAt.toString()));
    }

    private Outcome runRefund(int index) {
        UUID tenantId = UUID.randomUUID();
        String paymentReference = "pay_bench_" + index;
        String key = "refund_retry_" + index;
        SimulatedPayment payment = new SimulatedPayment(
            tenantId, "customer_" + index, paymentReference, "USD", 10_000L, true, "pay_original_" + index);
        SimulatedPaymentRepository payments = mock(SimulatedPaymentRepository.class);
        SimulatedPaymentOperationRepository operations = mock(SimulatedPaymentOperationRepository.class);
        AtomicReference<SimulatedPaymentOperation> recorded = new AtomicReference<>();
        when(payments.findByTenantIdAndPaymentReference(tenantId, paymentReference)).thenReturn(Optional.of(payment));
        when(payments.findByTenantIdAndPaymentReferenceWithLock(tenantId, paymentReference)).thenReturn(Optional.of(payment));
        when(payments.save(any())).thenAnswer(call -> call.getArgument(0));
        when(operations.findByTenantIdAndProviderIdempotencyKey(tenantId, key))
            .thenAnswer(call -> Optional.ofNullable(recorded.get()));
        when(operations.save(any())).thenAnswer(call -> {
            SimulatedPaymentOperation operation = call.getArgument(0);
            recorded.set(operation);
            return operation;
        });

        RefundDuplicateChargeAction action = new RefundDuplicateChargeAction(new SimulatedPaymentAdapter(payments, operations));
        ActionContext context = new ActionContext(tenantId, UUID.randomUUID(), UUID.randomUUID(),
            UUID.randomUUID(), "BENCHMARK", Set.of());
        var input = new RefundDuplicateChargeAction.RefundInput(
            "customer_" + index, paymentReference, "USD", 2_500L, "DUPLICATE_CHARGE", "pay_original_" + index);
        ExecutionResult first = action.execute(context, input, key);
        ReconciliationResult reconciliation = action.reconcile(context, input, first);
        ExecutionResult retry = action.execute(context, input, key);

        verify(payments, times(1)).save(any());
        return new Outcome(reconciliation.isMatch(), first.providerReference().equals(retry.providerReference())
            && payment.getRefundedAmountCents() == 2_500L);
    }

    private Outcome runUnlock(int index) {
        UUID tenantId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        String key = "unlock_retry_" + index;
        SimulatedAccount account = new SimulatedAccount(
            tenantId, userId, "benchmark-" + index + "@example.invalid", true, "SECURITY_LOCK", Instant.now());
        SimulatedAccountRepository accounts = mock(SimulatedAccountRepository.class);
        SimulatedAccountOperationRepository operations = mock(SimulatedAccountOperationRepository.class);
        AtomicReference<SimulatedAccountOperation> recorded = new AtomicReference<>();
        when(accounts.findByTenantIdAndUserId(tenantId, userId)).thenReturn(Optional.of(account));
        when(accounts.findByTenantIdAndUserIdWithLock(tenantId, userId)).thenReturn(Optional.of(account));
        when(accounts.save(any())).thenAnswer(call -> call.getArgument(0));
        when(operations.findByTenantIdAndProviderIdempotencyKey(tenantId, key))
            .thenAnswer(call -> Optional.ofNullable(recorded.get()));
        when(operations.save(any())).thenAnswer(call -> {
            SimulatedAccountOperation operation = call.getArgument(0);
            recorded.set(operation);
            return operation;
        });

        UnlockAccountAction action = new UnlockAccountAction(new SimulatedIdentityAdapter(accounts, operations));
        ActionContext context = new ActionContext(tenantId, UUID.randomUUID(), UUID.randomUUID(),
            UUID.randomUUID(), "BENCHMARK", Set.of());
        var input = new UnlockAccountAction.UnlockInput(userId, "Verified benchmark recovery", true);
        ExecutionResult first = action.execute(context, input, key);
        ReconciliationResult reconciliation = action.reconcile(context, input, first);
        ExecutionResult retry = action.execute(context, input, key);

        verify(accounts, times(1)).save(any());
        return new Outcome(reconciliation.isMatch(), first.providerReference().equals(retry.providerReference())
            && !account.isLocked());
    }

    private record Outcome(boolean reconciled, boolean duplicateSuppressed) {}
}
