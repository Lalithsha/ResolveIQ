package com.resolveiq.orchestration.application.action;

import com.resolveiq.orchestration.adapter.out.identity.SimulatedIdentityAdapter;
import com.resolveiq.orchestration.adapter.out.payment.SimulatedPaymentAdapter;
import com.resolveiq.orchestration.domain.model.action.SimulatedAccount;
import com.resolveiq.orchestration.domain.model.action.SimulatedAccountOperation;
import com.resolveiq.orchestration.domain.model.action.SimulatedPayment;
import com.resolveiq.orchestration.domain.model.action.SimulatedPaymentOperation;
import com.resolveiq.orchestration.domain.repository.SimulatedAccountOperationRepository;
import com.resolveiq.orchestration.domain.repository.SimulatedAccountRepository;
import com.resolveiq.orchestration.domain.repository.SimulatedPaymentOperationRepository;
import com.resolveiq.orchestration.domain.repository.SimulatedPaymentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SimulatedAdaptersTest {

    @Mock
    private SimulatedPaymentRepository paymentRepository;
    @Mock
    private SimulatedPaymentOperationRepository paymentOperationRepository;

    @Mock
    private SimulatedAccountRepository accountRepository;
    @Mock
    private SimulatedAccountOperationRepository accountOperationRepository;

    private SimulatedPaymentAdapter paymentAdapter;
    private SimulatedIdentityAdapter identityAdapter;

    @BeforeEach
    void setUp() {
        paymentAdapter = new SimulatedPaymentAdapter(paymentRepository, paymentOperationRepository);
        identityAdapter = new SimulatedIdentityAdapter(accountRepository, accountOperationRepository);
    }

    @Test
    @DisplayName("Payment refund executes and mutates ledger correctly")
    void paymentRefundExecutesAndMutatesLedger() {
        UUID tenantId = UUID.randomUUID();
        String payRef = "pay_001";
        SimulatedPayment payment = new SimulatedPayment(tenantId, "cust_001", payRef, "USD", 10000L, true, "pay_orig");

        when(paymentOperationRepository.findByTenantIdAndProviderIdempotencyKey(tenantId, "idem_1")).thenReturn(Optional.empty());
        when(paymentRepository.findByTenantIdAndPaymentReferenceWithLock(tenantId, payRef)).thenReturn(Optional.of(payment));
        when(paymentRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        var result = paymentAdapter.refundPayment(tenantId, payRef, 4000L, "USD", "idem_1");

        assertThat(result.status()).isEqualTo("SUCCEEDED");
        assertThat(result.refundedAmountCents()).isEqualTo(4000L);
        assertThat(payment.getRefundedAmountCents()).isEqualTo(4000L);
        assertThat(payment.getAvailableRefundableAmountCents()).isEqualTo(6000L);

        verify(paymentOperationRepository).save(any(SimulatedPaymentOperation.class));
    }

    @Test
    @DisplayName("Payment refund rejects if payment is not settled")
    void paymentRefundRejectsIfNotSettled() {
        UUID tenantId = UUID.randomUUID();
        String payRef = "pay_auth_only";
        SimulatedPayment payment = new SimulatedPayment(tenantId, "cust_001", payRef, "USD", 10000L, false, "pay_orig");

        when(paymentOperationRepository.findByTenantIdAndProviderIdempotencyKey(tenantId, "idem_2")).thenReturn(Optional.empty());
        when(paymentRepository.findByTenantIdAndPaymentReferenceWithLock(tenantId, payRef)).thenReturn(Optional.of(payment));

        assertThatThrownBy(() -> paymentAdapter.refundPayment(tenantId, payRef, 2000L, "USD", "idem_2"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("not settled");
    }

    @Test
    @DisplayName("Payment refund rejects if amount exceeds remaining refundable balance")
    void paymentRefundRejectsIfExceedsBalance() {
        UUID tenantId = UUID.randomUUID();
        String payRef = "pay_002";
        SimulatedPayment payment = new SimulatedPayment(tenantId, "cust_001", payRef, "USD", 5000L, true, "pay_orig");
        payment.setRefundedAmountCents(4000L);

        when(paymentOperationRepository.findByTenantIdAndProviderIdempotencyKey(tenantId, "idem_3")).thenReturn(Optional.empty());
        when(paymentRepository.findByTenantIdAndPaymentReferenceWithLock(tenantId, payRef)).thenReturn(Optional.of(payment));

        assertThatThrownBy(() -> paymentAdapter.refundPayment(tenantId, payRef, 2000L, "USD", "idem_3"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Insufficient refundable balance");
    }

    @Test
    @DisplayName("Payment refund returns existing operation on repeated idempotency key without debiting ledger")
    void paymentRefundReturnsExistingOperationOnDuplicateKey() {
        UUID tenantId = UUID.randomUUID();
        SimulatedPaymentOperation op = new SimulatedPaymentOperation(
                tenantId, "idem_repeat", "pay_001", "REFUND", 3000L, "USD", "SUCCEEDED", "pay_ref_exist"
        );
        when(paymentOperationRepository.findByTenantIdAndProviderIdempotencyKey(tenantId, "idem_repeat"))
                .thenReturn(Optional.of(op));

        var result = paymentAdapter.refundPayment(tenantId, "pay_001", 3000L, "USD", "idem_repeat");

        assertThat(result.providerReference()).isEqualTo("pay_ref_exist");
        verify(paymentRepository, never()).findByTenantIdAndPaymentReferenceWithLock(any(), any());
    }

    @Test
    @DisplayName("Account unlock succeeds and clears locked status")
    void accountUnlockSucceeds() {
        UUID tenantId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        SimulatedAccount account = new SimulatedAccount(
                tenantId, userId, "user@test.com", true, "SUSPICIOUS_LOGIN", Instant.now().minusSeconds(300)
        );

        when(accountOperationRepository.findByTenantIdAndProviderIdempotencyKey(tenantId, "id_idem_1")).thenReturn(Optional.empty());
        when(accountRepository.findByTenantIdAndUserIdWithLock(tenantId, userId)).thenReturn(Optional.of(account));
        when(accountRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        var result = identityAdapter.unlockAccount(tenantId, userId, "Customer verified", true, "id_idem_1");

        assertThat(result.status()).isEqualTo("SUCCEEDED");
        assertThat(account.isLocked()).isFalse();
        assertThat(account.getSessionsRevokedAt()).isNotNull();

        verify(accountOperationRepository).save(any(SimulatedAccountOperation.class));
    }

    @Test
    @DisplayName("Account unlock rejects if identity verification challenge is missing or expired")
    void accountUnlockRejectsIfIdentityExpired() {
        UUID tenantId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        SimulatedAccount account = new SimulatedAccount(
                tenantId, userId, "user@test.com", true, "SUSPICIOUS_LOGIN", Instant.now().minusSeconds(100000) // > 24 hours
        );

        when(accountOperationRepository.findByTenantIdAndProviderIdempotencyKey(tenantId, "id_idem_2")).thenReturn(Optional.empty());
        when(accountRepository.findByTenantIdAndUserIdWithLock(tenantId, userId)).thenReturn(Optional.of(account));

        assertThatThrownBy(() -> identityAdapter.unlockAccount(tenantId, userId, "Test", false, "id_idem_2"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("last 24 hours");
    }
}
