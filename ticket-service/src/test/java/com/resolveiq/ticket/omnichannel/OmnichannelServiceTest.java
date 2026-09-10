package com.resolveiq.ticket.omnichannel;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.resolveiq.security.TrustedPrincipal;
import com.resolveiq.ticket.adapter.out.email.SimulatedEmailChannelAdapter;
import com.resolveiq.ticket.application.dto.omnichannel.OmnichannelDtos.*;
import com.resolveiq.ticket.application.port.omnichannel.WebhookRequest;
import com.resolveiq.ticket.application.service.omnichannel.IdentityHashService;
import com.resolveiq.ticket.application.service.omnichannel.MailboxSimulator;
import com.resolveiq.ticket.application.service.omnichannel.OmnichannelService;
import com.resolveiq.ticket.domain.model.*;
import com.resolveiq.ticket.domain.model.omnichannel.*;
import com.resolveiq.ticket.domain.repository.TicketMessageRepository;
import com.resolveiq.ticket.domain.repository.TicketRepository;
import com.resolveiq.ticket.domain.repository.omnichannel.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OmnichannelServiceTest {

    @Mock private ConversationRepository conversationRepository;
    @Mock private ChannelIdentityRepository channelIdentityRepository;
    @Mock private ConversationParticipantRepository participantRepository;
    @Mock private ChannelMessageMetadataRepository messageMetadataRepository;
    @Mock private DeliveryAttemptRepository deliveryAttemptRepository;
    @Mock private ConversationMergeRecordRepository mergeRecordRepository;
    @Mock private CustomerPreferencesRepository customerPreferencesRepository;
    @Mock private ConsentRecordRepository consentRecordRepository;
    @Mock private HandoffSummaryRepository handoffSummaryRepository;
    @Mock private WebhookInboxEventRepository webhookInboxEventRepository;
    @Mock private UnverifiedEmailIntakeRepository unverifiedEmailIntakeRepository;
    @Mock private TicketRepository ticketRepository;
    @Mock private TicketMessageRepository ticketMessageRepository;

    private SimulatedEmailChannelAdapter emailAdapter;
    private IdentityHashService identityHashService;
    private OmnichannelService omnichannelService;

    private MailboxSimulator mailboxSimulator;
    private final String secret = "omnichannel_test_secret_123456789";
    private final UUID tenantId = UUID.randomUUID();
    private final UUID customerId = UUID.randomUUID();
    private final UUID agentId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        emailAdapter = new SimulatedEmailChannelAdapter(secret, new ObjectMapper());
        identityHashService = new IdentityHashService(secret);
        mailboxSimulator = new MailboxSimulator();
        omnichannelService = new OmnichannelService(
            conversationRepository,
            channelIdentityRepository,
            participantRepository,
            messageMetadataRepository,
            deliveryAttemptRepository,
            mergeRecordRepository,
            customerPreferencesRepository,
            consentRecordRepository,
            handoffSummaryRepository,
            webhookInboxEventRepository,
            unverifiedEmailIntakeRepository,
            ticketRepository,
            ticketMessageRepository,
            emailAdapter,
            identityHashService,
            mailboxSimulator
        );
    }

    private WebhookRequest createSignedRequest(String eventId, String from, String subject, String body) {
        String json = String.format("""
            {
                "eventId": "%s",
                "from": "%s",
                "to": "support@resolveiq.io",
                "subject": "%s",
                "body": "%s",
                "messageId": "ext_%s"
            }
            """, eventId, from, subject, body, eventId);
        byte[] bytes = json.getBytes(StandardCharsets.UTF_8);
        String timestamp = String.valueOf(Instant.now().getEpochSecond());
        String hmac = emailAdapter.computeHmacHex(bytes, timestamp);

        Map<String, String> headers = new HashMap<>();
        headers.put("X-ResolveIQ-Signature", "sha256=" + hmac);
        headers.put("X-ResolveIQ-Timestamp", timestamp);

        return new WebhookRequest(headers, bytes, "/webhooks/v1/email/" + tenantId);
    }

    @Test
    void testInboundEmailFromVerifiedCustomerCreatesTicketAndConversation() {
        String email = "alice@example.com";
        String hmac = identityHashService.computeAddressHmac(identityHashService.normalizeEmail(email));
        ChannelIdentity verifiedIdentity = new ChannelIdentity(tenantId, customerId, ChannelType.EMAIL, hmac, email, true);

        when(webhookInboxEventRepository.existsByTenantIdAndProviderAndExternalEventId(any(), eq("EMAIL"), eq("evt_1"))).thenReturn(false);
        when(channelIdentityRepository.findByTenantIdAndChannelAndAddressHmac(any(), eq(ChannelType.EMAIL), eq(hmac)))
            .thenReturn(Optional.of(verifiedIdentity));

        when(ticketRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(conversationRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(ticketMessageRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        WebhookRequest req = createSignedRequest("evt_1", email, "Payment issue", "Help me");
        InboundWebhookResult result = omnichannelService.processInboundEmailWebhook(tenantId.toString(), req);

        assertFalse(result.isDuplicate());
        assertEquals("PROCESSED", result.status());
        assertNotNull(result.conversationId());
        assertNotNull(result.messageId());
        verify(ticketRepository).save(any());
        verify(ticketMessageRepository).save(any());
    }

    @Test
    void testInboundEmailDuplicateEventIsIgnored() {
        when(webhookInboxEventRepository.existsByTenantIdAndProviderAndExternalEventId(any(), eq("EMAIL"), eq("evt_dup"))).thenReturn(true);

        WebhookRequest req = createSignedRequest("evt_dup", "alice@example.com", "Hello", "World");
        InboundWebhookResult result = omnichannelService.processInboundEmailWebhook(tenantId.toString(), req);

        assertTrue(result.isDuplicate());
        assertEquals("DUPLICATE", result.status());
        verify(ticketRepository, never()).save(any());
    }

    @Test
    void testInboundEmailFromUnknownCustomerQuarantinesIntake() {
        String email = "stranger@example.com";
        String hmac = identityHashService.computeAddressHmac(identityHashService.normalizeEmail(email));

        when(webhookInboxEventRepository.existsByTenantIdAndProviderAndExternalEventId(any(), eq("EMAIL"), eq("evt_2"))).thenReturn(false);
        when(channelIdentityRepository.findByTenantIdAndChannelAndAddressHmac(any(), eq(ChannelType.EMAIL), eq(hmac)))
            .thenReturn(Optional.empty());

        when(unverifiedEmailIntakeRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        WebhookRequest req = createSignedRequest("evt_2", email, "Hello", "I am new here");
        InboundWebhookResult result = omnichannelService.processInboundEmailWebhook(tenantId.toString(), req);

        assertFalse(result.isDuplicate());
        assertEquals("PENDING_VERIFICATION", result.status());
        assertNotNull(result.pendingIntakeId());
        verify(unverifiedEmailIntakeRepository).save(any());
        verify(ticketRepository, never()).save(any()); 
    }

    @Test
    void testEmailChallengeRequestAndVerifyPromotesPendingIntake() {
        String email = "newuser@example.com";
        String hmac = identityHashService.computeAddressHmac(identityHashService.normalizeEmail(email));

        ChannelIdentity unverified = new ChannelIdentity(tenantId, customerId, ChannelType.EMAIL, hmac, email, false);
        when(channelIdentityRepository.findByTenantIdAndChannelAndAddressHmac(tenantId, ChannelType.EMAIL, hmac))
            .thenReturn(Optional.of(unverified));
        when(channelIdentityRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        // 1. Request challenge (API response does NOT contain plaintext token)
        EmailChallengeResponse challengeResp = omnichannelService.requestEmailChallenge(tenantId, customerId, email);
        assertNotNull(challengeResp.expiresAt());

        // 2. Token retrieved securely from test MailboxSimulator
        String challengeToken = mailboxSimulator.getLatestChallengeCode(tenantId, email).orElseThrow();
        assertNotNull(challengeToken);

        // 3. Mock pending intake
        UnverifiedEmailIntake intake = new UnverifiedEmailIntake(tenantId, email, hmac, "Need help", "Body text", "ext_msg_1");
        when(unverifiedEmailIntakeRepository.findByTenantIdAndSenderHmacAndStatus(tenantId, hmac, "PENDING"))
            .thenReturn(List.of(intake));
        when(ticketRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(conversationRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(ticketMessageRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        // 4. Verify challenge with correct token from mailboxSimulator
        EmailVerifyResponse verifyResp = omnichannelService.verifyEmailChallenge(tenantId, customerId, email, challengeToken);
        assertTrue(verifyResp.isVerified());
        assertEquals(1, verifyResp.linkedPendingIntakes());
        assertEquals("VERIFIED_LINKED", intake.getStatus());
        verify(consentRecordRepository).save(any());
    }

    @Test
    void testForeignAddressReassignmentRejected() {
        String email = "shared@example.com";
        String hmac = identityHashService.computeAddressHmac(identityHashService.normalizeEmail(email));
        UUID otherCustomer = UUID.randomUUID();

        ChannelIdentity existing = new ChannelIdentity(tenantId, otherCustomer, ChannelType.EMAIL, hmac, email, true);
        when(channelIdentityRepository.findByTenantIdAndChannelAndAddressHmac(tenantId, ChannelType.EMAIL, hmac))
            .thenReturn(Optional.of(existing));

        assertThrows(IllegalStateException.class, () ->
            omnichannelService.requestEmailChallenge(tenantId, customerId, email)
        );
    }

    @Test
    void testTimelineExcludesInternalNotesForCustomerAndIncludesForAgent() {
        UUID convId = UUID.randomUUID();
        UUID ticketId = UUID.randomUUID();
        Conversation conv = new Conversation(tenantId, ticketId, customerId);

        when(conversationRepository.findByIdAndTenantId(convId, tenantId)).thenReturn(Optional.of(conv));

        TicketMessage publicMsg = new TicketMessage(ticketId, tenantId, customerId, "CUSTOMER", "Public customer message", false);
        TicketMessage internalNote = new TicketMessage(ticketId, tenantId, agentId, "AGENT", "Internal staff private note", true);

        when(ticketMessageRepository.findByTicketIdAndTenantIdOrderByCreatedAtAsc(ticketId, tenantId))
            .thenReturn(List.of(publicMsg, internalNote));
        when(messageMetadataRepository.findByTenantIdAndConversationIdOrderByCreatedAtAsc(tenantId, convId))
            .thenReturn(List.of());

        // Customer principal
        TrustedPrincipal customerPrincipal = new TrustedPrincipal(customerId, tenantId, Set.of("CUSTOMER"), "DIRECT");
        TimelineResponse customerTimeline = omnichannelService.getTimeline(tenantId, convId, customerPrincipal);

        assertEquals(1, customerTimeline.messages().size());
        assertEquals("Public customer message", customerTimeline.messages().get(0).content());
        assertFalse(customerTimeline.messages().get(0).isInternal());

        // Agent principal
        TrustedPrincipal agentPrincipal = new TrustedPrincipal(agentId, tenantId, Set.of("AGENT"), "DIRECT");
        TimelineResponse agentTimeline = omnichannelService.getTimeline(tenantId, convId, agentPrincipal);

        assertEquals(2, agentTimeline.messages().size());
        assertTrue(agentTimeline.messages().stream().anyMatch(m -> m.isInternal() && m.content().contains("Internal staff")));
    }

    @Test
    void testHandoffLifecycleAndConversationMerge() {
        UUID ticketId = UUID.randomUUID();
        Conversation conv = new Conversation(tenantId, ticketId, customerId);
        UUID convId = conv.getId();
        Ticket ticket = new Ticket(ticketId, "TCK-10001", tenantId, customerId, "Billing issue", "Details", "Billing", TicketPriority.HIGH, "PORTAL", "en");

        when(conversationRepository.findByIdAndTenantId(convId, tenantId)).thenReturn(Optional.of(conv));
        when(ticketRepository.findByIdAndTenantId(ticketId, tenantId)).thenReturn(Optional.of(ticket));
        when(handoffSummaryRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        TrustedPrincipal customerPrincipal = new TrustedPrincipal(customerId, tenantId, Set.of("CUSTOMER"), "DIRECT");

        // 1. Request handoff
        HandoffResponse handoffResp = omnichannelService.requestHandoff(tenantId, convId, customerPrincipal, new HandoffRequest("Need human specialist"));
        assertEquals(HandoffState.QUEUED, handoffResp.state());
        assertNotNull(handoffResp.summary());

        // 2. Assign handoff
        ConversationResponse assignedResp = omnichannelService.assignHandoff(tenantId, convId, agentId);
        assertEquals(HandoffState.ASSIGNED, assignedResp.handoffState());
        assertEquals(agentId, assignedResp.assignedAgentId());

        // 3. Merge conversations
        UUID targetConvId = UUID.randomUUID();
        Conversation targetConv = new Conversation(tenantId, UUID.randomUUID(), customerId);
        when(conversationRepository.findByIdAndTenantId(targetConvId, tenantId)).thenReturn(Optional.of(targetConv));
        when(mergeRecordRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        ConversationMergeRecord mergeRec = omnichannelService.mergeConversations(tenantId, convId, targetConvId, agentId, "Duplicate thread");
        assertNotNull(mergeRec);
        assertEquals(ConversationStatus.MERGED, conv.getStatus());

        // 4. Split conversation
        when(mergeRecordRepository.findByIdAndTenantId(mergeRec.getId(), tenantId)).thenReturn(Optional.of(mergeRec));
        ConversationMergeRecord splitRec = omnichannelService.splitConversations(tenantId, mergeRec.getId(), agentId);
        assertNotNull(splitRec.getSplitAt());
        assertEquals(ConversationStatus.ACTIVE, conv.getStatus());
    }
}
