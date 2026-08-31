package com.resolveiq.auth;

import com.resolveiq.auth.application.dto.AuthResponse;
import com.resolveiq.auth.application.dto.LoginRequest;
import com.resolveiq.auth.application.service.AuthService;
import com.resolveiq.auth.application.service.PasswordService;
import com.resolveiq.auth.domain.model.Role;
import com.resolveiq.auth.domain.model.Tenant;
import com.resolveiq.auth.domain.model.User;
import com.resolveiq.auth.domain.repository.TenantRepository;
import com.resolveiq.auth.domain.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers
@SpringBootTest(properties = {
    "eureka.client.enabled=false",
    "spring.cloud.discovery.enabled=false"
})
class ConcurrentLoginIT {
    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine")
        .withDatabaseName("resolveiq")
        .withUsername("resolveiq_test")
        .withPassword("resolveiq_test_password");

    @DynamicPropertySource
    static void database(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
    }

    @Autowired AuthService auth;
    @Autowired PasswordService passwords;
    @Autowired TenantRepository tenants;
    @Autowired UserRepository users;

    @Test
    void simultaneousSuccessfulLoginsSerializeAttemptCounterUpdates() throws Exception {
        UUID tenantId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        String email = "concurrent-" + tenantId + "@resolveiq.test";
        String password = "ConcurrentLogin2026!";
        tenants.saveAndFlush(new Tenant(tenantId, "Concurrent Login Tenant", tenantId + ".test", "ACTIVE"));
        users.saveAndFlush(new User(userId, tenantId, email, passwords.encode(password),
            "Concurrent User", Set.of(Role.CUSTOMER)));

        int callerCount = 6;
        CountDownLatch start = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(callerCount);
        try {
            List<Future<AuthResponse>> responses = new ArrayList<>();
            for (int caller = 0; caller < callerCount; caller++) {
                responses.add(executor.submit(() -> {
                    start.await();
                    return auth.login(new LoginRequest(tenantId, email, password), "127.0.0.1", "ConcurrentLoginIT");
                }));
            }
            start.countDown();
            for (Future<AuthResponse> response : responses) {
                assertThat(response.get().accessToken()).isNotBlank();
            }
        } finally {
            executor.shutdownNow();
        }

        User persisted = users.findById(userId).orElseThrow();
        assertThat(persisted.getFailedLoginAttempts()).isZero();
        assertThat(persisted.isLocked()).isFalse();
    }
}
