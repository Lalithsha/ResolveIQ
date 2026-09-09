package com.resolveiq.orchestration.application.action;

import com.resolveiq.orchestration.adapter.out.payment.SimulatedPaymentAdapter;
import com.resolveiq.orchestration.domain.model.action.ActionType;
import com.resolveiq.orchestration.domain.model.action.RiskLevel;
import com.resolveiq.orchestration.domain.model.action.SimulatedPayment;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;

@Component
public class RefundDuplicateChargeAction implements ResolutionAction<RefundDuplicateChargeAction.RefundInput, RefundDuplicateChargeAction.RefundOutput> {

    public record RefundInput(
            String customerAccountId,
            String paymentReference,
            String currency,
            long amountCents,
            String reasonCode,
            String duplicateOfReference
    ) {}

    public record RefundOutput(
            String providerReference,
            String status,
            long refundedAmountCents,
            long totalRefundedToDateCents,
            Instant timestamp
    ) {}

    private final SimulatedPaymentAdapter paymentAdapter;

    public RefundDuplicateChargeAction(SimulatedPaymentAdapter paymentAdapter) {
        this.paymentAdapter = paymentAdapter;
    }

    @Override
    public ActionType type() {
        return ActionType.REFUND_DUPLICATE_CHARGE;
    }

    @Override
    public Class<RefundInput> inputType() {
        return RefundInput.class;
    }

    @Override
    public String extractTarget(RefundInput input) {
        return input.paymentReference();
    }

    @Override
    public CurrentState fetchCurrentState(ActionContext context, RefundInput input) {
        Optional<SimulatedPayment> paymentOpt = paymentAdapter.lookupPayment(context.tenantId(), input.paymentReference());
        if (paymentOpt.isEmpty()) {
            return CurrentState.notFound();
        }
        SimulatedPayment payment = paymentOpt.get();
        return CurrentState.found(String.valueOf(payment.getVersion()), Map.of(
                "paymentReference", payment.getPaymentReference(),
                "customerAccountId", payment.getCustomerAccountId(),
                "currency", payment.getCurrency(),
                "originalAmountCents", payment.getOriginalAmountCents(),
                "refundedAmountCents", payment.getRefundedAmountCents(),
                "availableRefundableCents", payment.getAvailableRefundableAmountCents(),
                "isSettled", payment.isSettled(),
                "duplicateOfReference", payment.getDuplicateOfReference() != null ? payment.getDuplicateOfReference() : ""
        ));
    }

    @Override
    public ValidationResult validate(ActionContext context, RefundInput input, CurrentState state) {
        if (!state.exists()) {
            return ValidationResult.invalid("PAYMENT_NOT_FOUND", "Authoritative payment ledger does not contain reference: " + input.paymentReference());
        }

        Map<String, Object> attrs = state.attributes();
        boolean isSettled = Boolean.TRUE.equals(attrs.get("isSettled"));
        if (!isSettled) {
            return ValidationResult.invalid("PAYMENT_NOT_SETTLED", "Payment is pending settlement or authorization. Refunds require settled charges.");
        }

        String currency = (String) attrs.get("currency");
        if (currency != null && !currency.equalsIgnoreCase(input.currency())) {
            return ValidationResult.invalid("CURRENCY_MISMATCH", "Currency mismatch. Expected: " + currency + ", provided: " + input.currency());
        }

        long availableCents = ((Number) attrs.get("availableRefundableCents")).longValue();
        if (input.amountCents() <= 0) {
            return ValidationResult.invalid("INVALID_AMOUNT", "Refund amount must be greater than zero.");
        }
        if (input.amountCents() > availableCents) {
            return ValidationResult.invalid("INSUFFICIENT_BALANCE", "Refund amount exceeds remaining balance. Available: " + availableCents + " cents, requested: " + input.amountCents() + " cents.");
        }

        String actualDuplicate = (String) attrs.get("duplicateOfReference");
        if (actualDuplicate == null || actualDuplicate.isBlank() || !actualDuplicate.equalsIgnoreCase(input.duplicateOfReference())) {
            return ValidationResult.invalid("DUPLICATE_RELATION_UNVERIFIED", "Authoritative billing system does not correlate payment " + input.paymentReference() + " with duplicate reference: " + input.duplicateOfReference());
        }

        // Amount > 5000 cents ($50.00) is HIGH risk (requires Team Lead / Financial approval)
        RiskLevel risk = input.amountCents() > 5000 ? RiskLevel.HIGH : RiskLevel.LOW;
        return ValidationResult.valid(risk);
    }

    @Override
    public ExecutionResult execute(ActionContext context, RefundInput input, String idempotencyKey) {
        try {
            SimulatedPaymentAdapter.PaymentRefundResult res = paymentAdapter.refundPayment(
                    context.tenantId(),
                    input.paymentReference(),
                    input.amountCents(),
                    input.currency(),
                    idempotencyKey
            );
            return ExecutionResult.success(res.providerReference(), Map.of(
                    "providerReference", res.providerReference(),
                    "status", res.status(),
                    "refundedAmountCents", res.refundedAmountCents(),
                    "totalRefundedToDateCents", res.totalRefundedToDateCents(),
                    "timestamp", res.timestamp().toString()
            ));
        } catch (IllegalArgumentException | IllegalStateException e) {
            return ExecutionResult.failedFinal(e.getMessage());
        } catch (Exception e) {
            return ExecutionResult.unknown("unknown", "Provider connection timed out or error occurred: " + e.getMessage());
        }
    }

    @Override
    public ReconciliationResult reconcile(ActionContext context, RefundInput input, ExecutionResult executionResult) {
        CurrentState freshState = fetchCurrentState(context, input);
        if (!freshState.exists()) {
            return ReconciliationResult.mismatch(
                    Map.of("refundedAmountCents", input.amountCents()),
                    Map.of("error", "Payment not found"),
                    "Payment disappeared after execution"
            );
        }

        long actualRefunded = ((Number) freshState.attributes().get("refundedAmountCents")).longValue();
        if (actualRefunded >= input.amountCents()) {
            return ReconciliationResult.match(
                    Map.of("expectedRefundedAtLeast", input.amountCents()),
                    freshState.attributes(),
                    "Ledger confirmed refund applied"
            );
        } else {
            return ReconciliationResult.mismatch(
                    Map.of("expectedRefundedAtLeast", input.amountCents()),
                    freshState.attributes(),
                    "Observed refunded amount less than executed refund"
            );
        }
    }

    @Override
    public CompensationResult compensate(ActionContext context, RefundInput input, ExecutionResult executionResult) {
        return CompensationResult.nonCompensatable("A refund is a financial settlement and cannot be mechanically debited back. Manual dispute resolution required.");
    }
}
