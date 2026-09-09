package com.resolveiq.orchestration.adapter.out.payment;

import com.resolveiq.orchestration.domain.model.action.SimulatedPayment;
import com.resolveiq.orchestration.domain.model.action.SimulatedPaymentOperation;
import com.resolveiq.orchestration.domain.repository.SimulatedPaymentOperationRepository;
import com.resolveiq.orchestration.domain.repository.SimulatedPaymentRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Component
public class SimulatedPaymentAdapter {

    private static final Logger log = LoggerFactory.getLogger(SimulatedPaymentAdapter.class);

    private final SimulatedPaymentRepository paymentRepository;
    private final SimulatedPaymentOperationRepository operationRepository;

    public SimulatedPaymentAdapter(SimulatedPaymentRepository paymentRepository,
                                   SimulatedPaymentOperationRepository operationRepository) {
        this.paymentRepository = paymentRepository;
        this.operationRepository = operationRepository;
    }

    public record PaymentRefundResult(
            String providerReference,
            String status,
            long refundedAmountCents,
            long totalRefundedToDateCents,
            Instant timestamp
    ) {}

    public Optional<SimulatedPayment> lookupPayment(UUID tenantId, String paymentReference) {
        return paymentRepository.findByTenantIdAndPaymentReference(tenantId, paymentReference);
    }

    @Transactional
    public SimulatedPayment seedPayment(UUID tenantId, String customerAccountId, String paymentReference,
                                        String currency, long originalAmountCents, boolean isSettled,
                                        String duplicateOfReference) {
        return paymentRepository.findByTenantIdAndPaymentReference(tenantId, paymentReference)
                .orElseGet(() -> paymentRepository.save(new SimulatedPayment(
                        tenantId, customerAccountId, paymentReference, currency, originalAmountCents, isSettled, duplicateOfReference
                )));
    }

    @Transactional
    public PaymentRefundResult refundPayment(UUID tenantId, String paymentReference, long amountCents,
                                             String currency, String idempotencyKey) {
        // 1. Check idempotency log
        Optional<SimulatedPaymentOperation> existingOp =
                operationRepository.findByTenantIdAndProviderIdempotencyKey(tenantId, idempotencyKey);
        if (existingOp.isPresent()) {
            SimulatedPaymentOperation op = existingOp.get();
            log.info("Idempotent hit for payment refund: {} -> providerRef: {}", idempotencyKey, op.getProviderReference());
            return new PaymentRefundResult(
                    op.getProviderReference(),
                    op.getStatus(),
                    op.getAmountCents(),
                    op.getAmountCents(),
                    op.getCreatedAt()
            );
        }

        // 2. Fetch payment with pessimistic write lock
        SimulatedPayment payment = paymentRepository.findByTenantIdAndPaymentReferenceWithLock(tenantId, paymentReference)
                .orElseThrow(() -> new IllegalArgumentException("Payment not found: " + paymentReference));

        if (!payment.isSettled()) {
            throw new IllegalStateException("Payment " + paymentReference + " is not settled; authorizations cannot be refunded");
        }

        if (!payment.getCurrency().equalsIgnoreCase(currency)) {
            throw new IllegalArgumentException("Currency mismatch. Expected: " + payment.getCurrency() + ", provided: " + currency);
        }

        if (amountCents <= 0) {
            throw new IllegalArgumentException("Refund amount must be greater than zero");
        }

        long available = payment.getAvailableRefundableAmountCents();
        if (amountCents > available) {
            throw new IllegalStateException("Insufficient refundable balance. Remaining available: " + available + " cents, requested: " + amountCents + " cents");
        }

        // 3. Mutate ledger
        payment.setRefundedAmountCents(payment.getRefundedAmountCents() + amountCents);
        paymentRepository.save(payment);

        // 4. Record operation idempotency
        String providerRef = "pay_ref_" + UUID.randomUUID().toString().replace("-", "").substring(0, 16);
        SimulatedPaymentOperation operation = new SimulatedPaymentOperation(
                tenantId,
                idempotencyKey,
                paymentReference,
                "REFUND",
                amountCents,
                currency.toUpperCase(),
                "SUCCEEDED",
                providerRef
        );
        operationRepository.save(operation);

        log.info("Successfully executed refund of {} {} on payment {} (Provider Ref: {})",
                amountCents, currency, paymentReference, providerRef);

        return new PaymentRefundResult(
                providerRef,
                "SUCCEEDED",
                amountCents,
                payment.getRefundedAmountCents(),
                Instant.now()
        );
    }
}
