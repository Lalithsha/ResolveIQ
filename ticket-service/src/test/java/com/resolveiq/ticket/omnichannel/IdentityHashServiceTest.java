package com.resolveiq.ticket.omnichannel;

import com.resolveiq.ticket.application.service.omnichannel.IdentityHashService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class IdentityHashServiceTest {

    private IdentityHashService identityHashService;

    @BeforeEach
    void setUp() {
        identityHashService = new IdentityHashService("unit_test_secret_for_identity_hmac_12345");
    }

    @Test
    void testNormalizeEmailPreservesMailboxAndLowercasesDomain() {
        // Mailbox plus-addressing and dots are preserved per section 22.5
        String input = "  John.Doe+Support@ACME-Corp.COM  ";
        String normalized = identityHashService.normalizeEmail(input);
        assertEquals("John.Doe+Support@acme-corp.com", normalized);
    }

    @Test
    void testComputeAddressHmacIsDeterministic() {
        String addr1 = "user1@example.com";
        String addr2 = "user1@example.com";
        String addr3 = "user2@example.com";

        String hmac1 = identityHashService.computeAddressHmac(addr1);
        String hmac2 = identityHashService.computeAddressHmac(addr2);
        String hmac3 = identityHashService.computeAddressHmac(addr3);

        assertEquals(hmac1, hmac2);
        assertNotEquals(hmac1, hmac3);
        assertEquals(64, hmac1.length()); // SHA256 hex is 64 chars
    }

    @Test
    void testChallengeCodeGenerationAndVerification() {
        String code = identityHashService.generateChallengeCode();
        assertEquals(6, code.length());

        String hash = identityHashService.hashChallengeCode(code);
        assertNotNull(hash);

        assertTrue(identityHashService.verifyChallenge(code, hash));
        assertFalse(identityHashService.verifyChallenge("999999", hash));
        assertFalse(identityHashService.verifyChallenge(null, hash));
    }
}
