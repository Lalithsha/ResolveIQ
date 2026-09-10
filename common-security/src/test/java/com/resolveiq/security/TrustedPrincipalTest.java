package com.resolveiq.security;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class TrustedPrincipalTest {
    @Test
    void recentAuthenticationRejectsMissingStaleAndFutureTimes() {
        assertThat(principal(null).isRecentAuthentication(300)).isFalse();
        assertThat(principal(Instant.now().minusSeconds(301)).isRecentAuthentication(300)).isFalse();
        assertThat(principal(Instant.now().plusSeconds(31)).isRecentAuthentication(300)).isFalse();
        assertThat(principal(Instant.now().minusSeconds(10)).isRecentAuthentication(300)).isTrue();
    }

    private TrustedPrincipal principal(Instant authTime) {
        return new TrustedPrincipal(UUID.randomUUID(), UUID.randomUUID(), Set.of("AGENT"), "JWT", Set.of(), authTime);
    }
}
