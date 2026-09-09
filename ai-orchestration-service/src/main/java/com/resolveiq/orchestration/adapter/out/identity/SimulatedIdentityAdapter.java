package com.resolveiq.orchestration.adapter.out.identity;

import com.resolveiq.orchestration.domain.model.action.SimulatedAccount;
import com.resolveiq.orchestration.domain.model.action.SimulatedAccountOperation;
import com.resolveiq.orchestration.domain.repository.SimulatedAccountOperationRepository;
import com.resolveiq.orchestration.domain.repository.SimulatedAccountRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Component
public class SimulatedIdentityAdapter {

    private static final Logger log = LoggerFactory.getLogger(SimulatedIdentityAdapter.class);

    private final SimulatedAccountRepository accountRepository;
    private final SimulatedAccountOperationRepository operationRepository;

    public SimulatedIdentityAdapter(SimulatedAccountRepository accountRepository,
                                    SimulatedAccountOperationRepository operationRepository) {
        this.accountRepository = accountRepository;
        this.operationRepository = operationRepository;
    }

    public record AccountUnlockResult(
            String providerReference,
            String status,
            Instant unlockedAt,
            boolean sessionsRevoked
    ) {}

    public Optional<SimulatedAccount> lookupAccount(UUID tenantId, UUID userId) {
        return accountRepository.findByTenantIdAndUserId(tenantId, userId);
    }

    @Transactional
    public SimulatedAccount seedAccount(UUID tenantId, UUID userId, String userEmail,
                                        boolean isLocked, String lockReason,
                                        Instant lastVerifiedIdentityAt) {
        return accountRepository.findByTenantIdAndUserId(tenantId, userId)
                .orElseGet(() -> accountRepository.save(new SimulatedAccount(
                        tenantId, userId, userEmail, isLocked, lockReason, lastVerifiedIdentityAt
                )));
    }

    @Transactional
    public AccountUnlockResult unlockAccount(UUID tenantId, UUID userId, String reason,
                                            boolean revokeSessions, String idempotencyKey) {
        // 1. Check idempotency log
        Optional<SimulatedAccountOperation> existingOp =
                operationRepository.findByTenantIdAndProviderIdempotencyKey(tenantId, idempotencyKey);
        if (existingOp.isPresent()) {
            SimulatedAccountOperation op = existingOp.get();
            log.info("Idempotent hit for account unlock: {} -> providerRef: {}", idempotencyKey, op.getProviderReference());
            return new AccountUnlockResult(
                    op.getProviderReference(),
                    op.getStatus(),
                    op.getCreatedAt(),
                    revokeSessions
            );
        }

        // 2. Fetch account under write lock
        SimulatedAccount account = accountRepository.findByTenantIdAndUserIdWithLock(tenantId, userId)
                .orElseThrow(() -> new IllegalArgumentException("Account not found for user: " + userId));

        if (!account.isLocked()) {
            throw new IllegalStateException("Account for user " + userId + " is not currently locked");
        }

        // 3. Verify recent identity verification (within 24 hours)
        Instant verifiedAt = account.getLastVerifiedIdentityAt();
        if (verifiedAt == null || Duration.between(verifiedAt, Instant.now()).abs().toHours() > 24) {
            throw new IllegalStateException("Account unlock requires a verified identity challenge completed within the last 24 hours");
        }

        // 4. Update account state
        account.setLocked(false);
        account.setLockReason(null);
        if (revokeSessions) {
            account.setSessionsRevokedAt(Instant.now());
        }
        accountRepository.save(account);

        // 5. Record operation
        String providerRef = "id_unlock_" + UUID.randomUUID().toString().replace("-", "").substring(0, 16);
        SimulatedAccountOperation operation = new SimulatedAccountOperation(
                tenantId,
                idempotencyKey,
                userId,
                "UNLOCK",
                "SUCCEEDED",
                providerRef
        );
        operationRepository.save(operation);

        log.info("Successfully unlocked account for user {} (Provider Ref: {}, Revoke Sessions: {})",
                userId, providerRef, revokeSessions);

        return new AccountUnlockResult(
                providerRef,
                "SUCCEEDED",
                Instant.now(),
                revokeSessions
        );
    }
}
