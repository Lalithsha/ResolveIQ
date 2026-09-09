package com.resolveiq.orchestration.application.action;

import com.resolveiq.orchestration.adapter.out.identity.SimulatedIdentityAdapter;
import com.resolveiq.orchestration.domain.model.action.ActionType;
import com.resolveiq.orchestration.domain.model.action.RiskLevel;
import com.resolveiq.orchestration.domain.model.action.SimulatedAccount;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Component
public class UnlockAccountAction implements ResolutionAction<UnlockAccountAction.UnlockInput, UnlockAccountAction.UnlockOutput> {

    public record UnlockInput(
            UUID userId,
            String unlockReason,
            boolean revokeSessions
    ) {}

    public record UnlockOutput(
            String providerReference,
            String status,
            Instant unlockedAt,
            boolean sessionsRevoked
    ) {}

    private final SimulatedIdentityAdapter identityAdapter;

    public UnlockAccountAction(SimulatedIdentityAdapter identityAdapter) {
        this.identityAdapter = identityAdapter;
    }

    @Override
    public ActionType type() {
        return ActionType.UNLOCK_ACCOUNT;
    }

    @Override
    public Class<UnlockInput> inputType() {
        return UnlockInput.class;
    }

    @Override
    public String extractTarget(UnlockInput input) {
        return input.userId().toString();
    }

    @Override
    public CurrentState fetchCurrentState(ActionContext context, UnlockInput input) {
        Optional<SimulatedAccount> accountOpt = identityAdapter.lookupAccount(context.tenantId(), input.userId());
        if (accountOpt.isEmpty()) {
            return CurrentState.notFound();
        }
        SimulatedAccount account = accountOpt.get();
        return CurrentState.found(String.valueOf(account.getVersion()), Map.of(
                "userId", account.getUserId().toString(),
                "userEmail", account.getUserEmail(),
                "isLocked", account.isLocked(),
                "lockReason", account.getLockReason() != null ? account.getLockReason() : "",
                "lastVerifiedIdentityAt", account.getLastVerifiedIdentityAt() != null ? account.getLastVerifiedIdentityAt().toString() : "",
                "sessionsRevokedAt", account.getSessionsRevokedAt() != null ? account.getSessionsRevokedAt().toString() : ""
        ));
    }

    @Override
    public ValidationResult validate(ActionContext context, UnlockInput input, CurrentState state) {
        if (!state.exists()) {
            return ValidationResult.invalid("ACCOUNT_NOT_FOUND", "Directory account does not exist for user: " + input.userId());
        }

        Map<String, Object> attrs = state.attributes();
        boolean isLocked = Boolean.TRUE.equals(attrs.get("isLocked"));
        if (!isLocked) {
            return ValidationResult.invalid("ACCOUNT_NOT_LOCKED", "Account is already active/unlocked.");
        }

        String verifiedAtStr = (String) attrs.get("lastVerifiedIdentityAt");
        if (verifiedAtStr == null || verifiedAtStr.isBlank()) {
            return ValidationResult.invalid("IDENTITY_NOT_VERIFIED", "Account unlock requires a verified identity challenge completed within the last 24 hours.");
        }

        Instant verifiedAt = Instant.parse(verifiedAtStr);
        if (Duration.between(verifiedAt, Instant.now()).abs().toHours() > 24) {
            return ValidationResult.invalid("IDENTITY_CHALLENGE_EXPIRED", "Identity verification challenge expired (older than 24 hours). A new challenge is required.");
        }

        return ValidationResult.valid(RiskLevel.LOW);
    }

    @Override
    public ExecutionResult execute(ActionContext context, UnlockInput input, String idempotencyKey) {
        try {
            SimulatedIdentityAdapter.AccountUnlockResult res = identityAdapter.unlockAccount(
                    context.tenantId(),
                    input.userId(),
                    input.unlockReason(),
                    input.revokeSessions(),
                    idempotencyKey
            );
            return ExecutionResult.success(res.providerReference(), Map.of(
                    "providerReference", res.providerReference(),
                    "status", res.status(),
                    "unlockedAt", res.unlockedAt().toString(),
                    "sessionsRevoked", res.sessionsRevoked()
            ));
        } catch (IllegalArgumentException | IllegalStateException e) {
            return ExecutionResult.failedFinal(e.getMessage());
        } catch (Exception e) {
            return ExecutionResult.unknown("unknown", "Identity provider error: " + e.getMessage());
        }
    }

    @Override
    public ReconciliationResult reconcile(ActionContext context, UnlockInput input, ExecutionResult executionResult) {
        CurrentState freshState = fetchCurrentState(context, input);
        if (!freshState.exists()) {
            return ReconciliationResult.mismatch(
                    Map.of("isLocked", false),
                    Map.of("error", "Account not found"),
                    "Account disappeared after execution"
            );
        }

        boolean isLocked = Boolean.TRUE.equals(freshState.attributes().get("isLocked"));
        if (!isLocked) {
            return ReconciliationResult.match(
                    Map.of("isLocked", false),
                    freshState.attributes(),
                    "Directory confirmed account is unlocked"
            );
        } else {
            return ReconciliationResult.mismatch(
                    Map.of("isLocked", false),
                    freshState.attributes(),
                    "Account remains locked after unlock operation"
            );
        }
    }

    @Override
    public CompensationResult compensate(ActionContext context, UnlockInput input, ExecutionResult executionResult) {
        return CompensationResult.nonCompensatable("Relocking an account is a security intervention requiring audit, not an automatic rollback of an unlock.");
    }
}
