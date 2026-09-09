package com.resolveiq.ticket.application.service.omnichannel;

import com.resolveiq.contracts.event.EventEnvelope;
import com.resolveiq.contracts.event.TicketEvents;
import com.resolveiq.security.TrustedPrincipal;
import com.resolveiq.ticket.adapter.out.email.SimulatedEmailChannelAdapter;
import com.resolveiq.ticket.application.dto.omnichannel.OmnichannelDtos.*;
import com.resolveiq.ticket.application.port.omnichannel.OutboundMessage;
import com.resolveiq.ticket.application.port.omnichannel.VerifiedInboundEvent;
import com.resolveiq.ticket.application.port.omnichannel.WebhookRequest;
import com.resolveiq.ticket.domain.model.*;
import com.resolveiq.ticket.domain.model.omnichannel.*;
import com.resolveiq.ticket.domain.repository.TicketMessageRepository;
import com.resolveiq.ticket.domain.repository.TicketRepository;
import com.resolveiq.ticket.domain.repository.omnichannel.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.MessageDigest;
import java.time.Instant;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
public class OmnichannelService {

    private static final Logger log = LoggerFactory.getLogger(OmnichannelService.class);
    private static final Pattern TICKET_REF_PATTERN = Pattern.compile("(?i)(?:#|RESOLVEIQ-|TCK-)([0-9a-fA-F-]{4,36})");

    private final ConversationRepository conversationRepository;
    private final ChannelIdentityRepository channelIdentityRepository;
    private final ConversationParticipantRepository participantRepository;
    private final ChannelMessageMetadataRepository messageMetadataRepository;
    private final DeliveryAttemptRepository deliveryAttemptRepository;
    private final ConversationMergeRecordRepository mergeRecordRepository;
    private final CustomerPreferencesRepository customerPreferencesRepository;
    private final ConsentRecordRepository consentRecordRepository;
    private final HandoffSummaryRepository handoffSummaryRepository;
    private final WebhookInboxEventRepository webhookInboxEventRepository;
    private final UnverifiedEmailIntakeRepository unverifiedEmailIntakeRepository;
    private final TicketRepository ticketRepository;
    private final TicketMessageRepository ticketMessageRepository;
    private final SimulatedEmailChannelAdapter emailAdapter;
    private final IdentityHashService identityHashService;

    @Value("${resolveiq.default.tenant-id:00000000-0000-0000-0000-000000000001}")
    private String defaultTenantId;

    public OmnichannelService(
        ConversationRepository conversationRepository,
        ChannelIdentityRepository channelIdentityRepository,
        ConversationParticipantRepository participantRepository,
        ChannelMessageMetadataRepository messageMetadataRepository,
        DeliveryAttemptRepository deliveryAttemptRepository,
        ConversationMergeRecordRepository mergeRecordRepository,
        CustomerPreferencesRepository customerPreferencesRepository,
        ConsentRecordRepository consentRecordRepository,
        HandoffSummaryRepository handoffSummaryRepository,
        WebhookInboxEventRepository webhookInboxEventRepository,
        UnverifiedEmailIntakeRepository unverifiedEmailIntakeRepository,
        TicketRepository ticketRepository,
        TicketMessageRepository ticketMessageRepository,
        SimulatedEmailChannelAdapter emailAdapter,
        IdentityHashService identityHashService
    ) {
        this.conversationRepository = conversationRepository;
        this.channelIdentityRepository = channelIdentityRepository;
        this.participantRepository = participantRepository;
        this.messageMetadataRepository = messageMetadataRepository;
        this.deliveryAttemptRepository = deliveryAttemptRepository;
        this.mergeRecordRepository = mergeRecordRepository;
        this.customerPreferencesRepository = customerPreferencesRepository;
        this.consentRecordRepository = consentRecordRepository;
        this.handoffSummaryRepository = handoffSummaryRepository;
        this.webhookInboxEventRepository = webhookInboxEventRepository;
        this.unverifiedEmailIntakeRepository = unverifiedEmailIntakeRepository;
        this.ticketRepository = ticketRepository;
        this.ticketMessageRepository = ticketMessageRepository;
        this.emailAdapter = emailAdapter;
        this.identityHashService = identityHashService;
    }

    /**
     * Inbound Signed Email Webhook Handler
     */
    @Transactional
    public InboundWebhookResult processInboundEmailWebhook(String tenantPublicKey, WebhookRequest request) {
        UUID tenantId = resolveTenant(tenantPublicKey);

        // 1. Signature & timestamp verification
        VerifiedInboundEvent event = emailAdapter.verifyAndNormalize(request);

        // 2. Replay check (idempotency by external event ID)
        if (webhookInboxEventRepository.existsByTenantIdAndProviderAndExternalEventId(tenantId, "EMAIL", event.providerEventId())) {
            log.info("Duplicate webhook event ignored: providerEventId={}", event.providerEventId());
            return new InboundWebhookResult(true, "DUPLICATE", "Webhook event already processed", null, null, null);
        }

        String payloadHash = computePayloadHash(request.rawBody());
        webhookInboxEventRepository.save(new WebhookInboxEvent(tenantId, "EMAIL", event.providerEventId(), payloadHash));

        // 3. Identity Resolution
        String normalizedEmail = identityHashService.normalizeEmail(event.senderAddress());
        String addressHmac = identityHashService.computeAddressHmac(normalizedEmail);

        Optional<ChannelIdentity> optIdentity = channelIdentityRepository.findByTenantIdAndChannelAndAddressHmac(
            tenantId, ChannelType.EMAIL, addressHmac
        );

        if (optIdentity.isPresent() && optIdentity.get().isVerified() && optIdentity.get().getCustomerId() != null) {
            ChannelIdentity identity = optIdentity.get();
            UUID customerId = identity.getCustomerId();

            // Check if email subject references an existing ticket
            Optional<Ticket> optExistingTicket = findReferencedTicket(tenantId, event.subject());
            Ticket ticket;
            Conversation conv;

            if (optExistingTicket.isPresent()) {
                ticket = optExistingTicket.get();
                UUID existingTicketId = ticket.getId();
                conv = conversationRepository.findByTicketIdAndTenantId(existingTicketId, tenantId)
                    .orElseGet(() -> conversationRepository.save(new Conversation(tenantId, existingTicketId, customerId)));
            } else {
                // Create new ticket for verified customer
                String ticketNum = "TCK-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
                ticket = new Ticket(
                    UUID.randomUUID(),
                    ticketNum,
                    tenantId,
                    customerId,
                    event.subject().isBlank() ? "Email Inquiry from " + event.senderAddress() : event.subject(),
                    event.bodyText(),
                    "General",
                    TicketPriority.MEDIUM,
                    "EMAIL",
                    "en"
                );
                ticket = ticketRepository.save(ticket);
                conv = conversationRepository.save(new Conversation(tenantId, ticket.getId(), customerId));
                conv.setPreferredChannel(ChannelType.EMAIL);
                conversationRepository.save(conv);
            }

            // Save authoritative message
            TicketMessage message = new TicketMessage(
                ticket.getId(),
                tenantId,
                customerId,
                "CUSTOMER",
                event.bodyText(),
                false
            );
            ticketMessageRepository.save(message);

            // Save message channel metadata
            ChannelMessageMetadata meta = new ChannelMessageMetadata(
                tenantId,
                message.getId(),
                conv.getId(),
                ChannelType.EMAIL,
                ChannelDirection.INBOUND,
                event.externalMessageId(),
                event.senderAddress(),
                event.recipientAddress(),
                DeliveryStatus.DELIVERED,
                event.providerEventId()
            );
            meta.setProviderTimestamp(event.providerTimestamp());
            messageMetadataRepository.save(meta);

            return new InboundWebhookResult(false, "PROCESSED", "Message linked to ticket " + ticket.getTicketNumber(), conv.getId(), message.getId(), null);
        } else {
            // Unverified sender: quarantine into pending intake (do NOT fabricate a customer or create unverified ticket)
            UnverifiedEmailIntake intake = new UnverifiedEmailIntake(
                tenantId,
                event.senderAddress(),
                addressHmac,
                event.subject(),
                event.bodyText(),
                event.externalMessageId()
            );
            intake = unverifiedEmailIntakeRepository.save(intake);
            log.warn("Unverified email sender quarantined: email={}, intakeId={}", event.senderAddress(), intake.getId());

            return new InboundWebhookResult(false, "PENDING_VERIFICATION", "Quarantined in unverified intake pending address verification", null, null, intake.getId());
        }
    }

    /**
     * Request Email Linking Challenge (Portal Customer)
     */
    @Transactional
    public EmailChallengeResponse requestEmailChallenge(UUID tenantId, UUID customerId, String rawEmail) {
        String normalized = identityHashService.normalizeEmail(rawEmail);
        String hmac = identityHashService.computeAddressHmac(normalized);

        ChannelIdentity identity = channelIdentityRepository.findByTenantIdAndChannelAndAddressHmac(
            tenantId, ChannelType.EMAIL, hmac
        ).orElseGet(() -> {
            ChannelIdentity newId = new ChannelIdentity(tenantId, customerId, ChannelType.EMAIL, hmac, normalized, false);
            return channelIdentityRepository.save(newId);
        });

        if (identity.isVerified() && customerId.equals(identity.getCustomerId())) {
            return new EmailChallengeResponse(normalized, null, null, "Email address is already verified and linked to your account.");
        }

        String challengeCode = identityHashService.generateChallengeCode();
        String challengeHash = identityHashService.hashChallengeCode(challengeCode);
        Instant expiresAt = Instant.now().plusSeconds(900); // 15 minutes

        identity.setChallenge(challengeHash, expiresAt);
        channelIdentityRepository.save(identity);

        log.info("Email challenge generated: customerId={}, address={}, code={}", customerId, normalized, challengeCode);

        return new EmailChallengeResponse(
            normalized,
            challengeCode,
            expiresAt,
            "Verification challenge generated. Please submit code to complete link."
        );
    }

    /**
     * Verify Email Challenge and Atomically Promote Quarantined Intakes
     */
    @Transactional
    public EmailVerifyResponse verifyEmailChallenge(UUID tenantId, UUID customerId, String rawEmail, String token) {
        String normalized = identityHashService.normalizeEmail(rawEmail);
        String hmac = identityHashService.computeAddressHmac(normalized);

        ChannelIdentity identity = channelIdentityRepository.findByTenantIdAndChannelAndAddressHmac(
            tenantId, ChannelType.EMAIL, hmac
        ).orElseThrow(() -> new IllegalArgumentException("No pending verification found for address: " + normalized));

        if (identity.getChallengeAttempts() >= 3) {
            throw new IllegalStateException("Maximum verification attempts exceeded. Please request a new code.");
        }

        if (identity.getChallengeExpiresAt() == null || Instant.now().isAfter(identity.getChallengeExpiresAt())) {
            throw new IllegalStateException("Verification challenge has expired. Please request a new code.");
        }

        boolean valid = identityHashService.verifyChallenge(token, identity.getVerificationChallengeHash());
        if (!valid) {
            identity.incrementChallengeAttempts();
            channelIdentityRepository.save(identity);
            throw new IllegalArgumentException("Invalid verification token.");
        }

        // Mark identity verified
        identity.markVerified(customerId);
        channelIdentityRepository.save(identity);

        // Record transactional consent
        consentRecordRepository.save(new ConsentRecord(
            tenantId, customerId, ChannelType.EMAIL, "TRANSACTIONAL_SUPPORT", true, "PORTAL_VERIFIED_CHALLENGE"
        ));

        // Promote pending unverified email intakes for this address
        List<UnverifiedEmailIntake> pendingIntakes = unverifiedEmailIntakeRepository.findByTenantIdAndSenderHmacAndStatus(
            tenantId, hmac, "PENDING"
        );

        int promotedCount = 0;
        for (UnverifiedEmailIntake intake : pendingIntakes) {
            String ticketNum = "TCK-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
            Ticket ticket = new Ticket(
                UUID.randomUUID(),
                ticketNum,
                tenantId,
                customerId,
                intake.getSubject().isBlank() ? "Email Inquiry" : intake.getSubject(),
                intake.getBodyText(),
                "General",
                TicketPriority.MEDIUM,
                "EMAIL",
                "en"
            );
            ticket = ticketRepository.save(ticket);

            Conversation conv = new Conversation(tenantId, ticket.getId(), customerId);
            conv.setPreferredChannel(ChannelType.EMAIL);
            conversationRepository.save(conv);

            TicketMessage message = new TicketMessage(
                ticket.getId(),
                tenantId,
                customerId,
                "CUSTOMER",
                intake.getBodyText(),
                false
            );
            ticketMessageRepository.save(message);

            ChannelMessageMetadata meta = new ChannelMessageMetadata(
                tenantId,
                message.getId(),
                conv.getId(),
                ChannelType.EMAIL,
                ChannelDirection.INBOUND,
                intake.getExternalMessageId(),
                intake.getSenderEmail(),
                null,
                DeliveryStatus.DELIVERED,
                intake.getId().toString()
            );
            messageMetadataRepository.save(meta);

            intake.promoteToTicket(ticket.getId());
            unverifiedEmailIntakeRepository.save(intake);
            promotedCount++;
        }

        return new EmailVerifyResponse(
            identity.getId(),
            normalized,
            true,
            promotedCount,
            "Email successfully verified and linked. Promoted " + promotedCount + " pending email message(s)."
        );
    }

    /**
     * Get Unified Conversation Timeline
     * Enforces internal notes protection: customers NEVER receive internal notes!
     */
    @Transactional(readOnly = true)
    public TimelineResponse getTimeline(UUID tenantId, UUID conversationId, TrustedPrincipal principal) {
        Conversation conv = conversationRepository.findByIdAndTenantId(conversationId, tenantId)
            .orElseThrow(() -> new IllegalArgumentException("Conversation not found: " + conversationId));

        boolean isCustomer = principal.roles().contains("CUSTOMER") && !principal.roles().contains("AGENT") && !principal.roles().contains("ADMIN");
        if (isCustomer && !conv.getPrimaryCustomerId().equals(principal.userId())) {
            throw new SecurityException("Access denied to conversation");
        }

        List<TicketMessage> messages = ticketMessageRepository.findByTicketIdAndTenantIdOrderByCreatedAtAsc(
            conv.getTicketId(), tenantId
        );

        Map<UUID, ChannelMessageMetadata> metaMap = messageMetadataRepository.findByTenantIdAndConversationIdOrderByCreatedAtAsc(
            tenantId, conversationId
        ).stream().collect(Collectors.toMap(ChannelMessageMetadata::getMessageId, m -> m, (a, b) -> a));

        List<TimelineMessageItem> timelineItems = new ArrayList<>();
        for (TicketMessage msg : messages) {
            // Protection: internal notes are completely excluded for customers
            if (isCustomer && msg.isInternal()) {
                continue;
            }

            ChannelMessageMetadata meta = metaMap.get(msg.getId());
            ChannelType channel = meta != null ? meta.getChannel() : ChannelType.PORTAL;
            ChannelDirection direction = meta != null ? meta.getDirection() 
                : (msg.isInternal() ? ChannelDirection.INTERNAL : ("CUSTOMER".equalsIgnoreCase(msg.getSenderRole()) ? ChannelDirection.INBOUND : ChannelDirection.OUTBOUND));
            DeliveryStatus deliveryStatus = meta != null ? meta.getDeliveryStatus() : DeliveryStatus.DELIVERED;
            String senderAddr = meta != null ? meta.getSenderAddress() : null;
            String recipientAddr = meta != null ? meta.getRecipientAddress() : null;

            timelineItems.add(new TimelineMessageItem(
                msg.getId(),
                conv.getId(),
                msg.getSenderId(),
                msg.getSenderRole(),
                msg.getContent(),
                msg.isInternal(),
                channel,
                direction,
                deliveryStatus,
                senderAddr,
                recipientAddr,
                msg.getCreatedAt()
            ));
        }

        HandoffSummaryResponse latestHandoff = handoffSummaryRepository.findFirstByTenantIdAndConversationIdOrderByCreatedAtDesc(
            tenantId, conversationId
        ).map(h -> new HandoffSummaryResponse(
            h.getId(),
            h.getConversationId(),
            h.getTicketId(),
            h.getIssueSummary(),
            h.getVerifiedFacts(),
            h.getAttemptedSteps(),
            h.getPromisedActions(),
            h.getSentiment(),
            h.getOpenQuestions(),
            h.getCreatedAt()
        )).orElse(null);

        return new TimelineResponse(
            conv.getId(),
            conv.getTicketId(),
            conv.getStatus(),
            conv.getHandoffState(),
            conv.getPreferredChannel(),
            timelineItems,
            latestHandoff
        );
    }

    /**
     * Add Message to Conversation
     * Enforces internal note outbound restriction and consent verification!
     */
    @Transactional
    public TimelineMessageItem addMessage(
        UUID tenantId,
        UUID conversationId,
        TrustedPrincipal principal,
        AddConversationMessageRequest request
    ) {
        Conversation conv = conversationRepository.findByIdAndTenantId(conversationId, tenantId)
            .orElseThrow(() -> new IllegalArgumentException("Conversation not found: " + conversationId));

        boolean isCustomer = principal.roles().contains("CUSTOMER") && !principal.roles().contains("AGENT") && !principal.roles().contains("ADMIN");
        if (isCustomer && request.isInternal()) {
            throw new SecurityException("Customers cannot add internal notes");
        }

        String senderRole = isCustomer ? "CUSTOMER" : (principal.roles().contains("ADMIN") ? "ADMIN" : "AGENT");

        TicketMessage message = new TicketMessage(
            conv.getTicketId(),
            tenantId,
            principal.userId(),
            senderRole,
            request.content(),
            request.isInternal()
        );
        ticketMessageRepository.save(message);

        ChannelType targetChannel = request.channel() != null ? request.channel() : conv.getPreferredChannel();
        ChannelDirection direction = request.isInternal() 
            ? ChannelDirection.INTERNAL 
            : (isCustomer ? ChannelDirection.INBOUND : ChannelDirection.OUTBOUND);

        DeliveryStatus deliveryStatus = DeliveryStatus.DELIVERED;
        String recipientAddress = null;

        // If agent sends an outbound message to EMAIL, check consent & verified channel identity
        if (!request.isInternal() && !isCustomer && targetChannel == ChannelType.EMAIL) {
            Optional<CustomerPreferences> optPrefs = customerPreferencesRepository.findByTenantIdAndCustomerId(
                tenantId, conv.getPrimaryCustomerId()
            );

            if (optPrefs.isPresent() && !optPrefs.get().isEmailNotificationsEnabled()) {
                log.info("Customer has disabled email notifications; falling back to PORTAL");
                targetChannel = ChannelType.PORTAL;
            } else {
                List<ChannelIdentity> identities = channelIdentityRepository.findByTenantIdAndCustomerId(tenantId, conv.getPrimaryCustomerId());
                Optional<ChannelIdentity> verifiedEmail = identities.stream()
                    .filter(i -> i.getChannel() == ChannelType.EMAIL && i.isVerified())
                    .findFirst();

                if (verifiedEmail.isPresent()) {
                    recipientAddress = verifiedEmail.get().getDisplayAddress();
                    OutboundMessage outbound = new OutboundMessage(
                        tenantId,
                        message.getId(),
                        conv.getId(),
                        recipientAddress,
                        request.subject() != null ? request.subject() : "Support Update",
                        request.content(),
                        false
                    );
                    var receipt = emailAdapter.send(outbound, request.idempotencyKey() != null ? request.idempotencyKey() : UUID.randomUUID().toString());
                    deliveryStatus = receipt.status();

                    deliveryAttemptRepository.save(new DeliveryAttempt(
                        tenantId,
                        message.getId(),
                        ChannelType.EMAIL,
                        recipientAddress,
                        deliveryStatus,
                        receipt.providerMessageId(),
                        receipt.errorDetails(),
                        1
                    ));
                } else {
                    log.info("Customer has no verified email address; delivering via PORTAL");
                    targetChannel = ChannelType.PORTAL;
                }
            }
        }

        ChannelMessageMetadata meta = new ChannelMessageMetadata(
            tenantId,
            message.getId(),
            conv.getId(),
            targetChannel,
            direction,
            null,
            null,
            recipientAddress,
            deliveryStatus,
            request.idempotencyKey()
        );
        messageMetadataRepository.save(meta);

        // Update conversation updated_at
        conv.setStatus(ConversationStatus.ACTIVE);
        conversationRepository.save(conv);

        return new TimelineMessageItem(
            message.getId(),
            conv.getId(),
            message.getSenderId(),
            message.getSenderRole(),
            message.getContent(),
            message.isInternal(),
            targetChannel,
            direction,
            deliveryStatus,
            null,
            recipientAddress,
            message.getCreatedAt()
        );
    }

    /**
     * Request Intelligent Handoff
     */
    @Transactional
    public HandoffResponse requestHandoff(UUID tenantId, UUID conversationId, TrustedPrincipal principal, HandoffRequest request) {
        Conversation conv = conversationRepository.findByIdAndTenantId(conversationId, tenantId)
            .orElseThrow(() -> new IllegalArgumentException("Conversation not found: " + conversationId));

        Ticket ticket = ticketRepository.findByIdAndTenantId(conv.getTicketId(), tenantId)
            .orElseThrow(() -> new IllegalArgumentException("Ticket not found for conversation"));

        String issueSummary = request.reason() != null && !request.reason().isBlank() 
            ? request.reason() 
            : "Customer requested human specialist assistance.";

        String verifiedFacts = String.format("Ticket %s [%s] category=%s priority=%s",
            ticket.getTicketNumber(), ticket.getStatus(), ticket.getCategory(), ticket.getPriority());

        String attemptedSteps = "Omnichannel intake complete, automated triage performed.";
        String promisedActions = "Conversation queued for human specialist assignment.";
        String sentiment = ticket.getSentiment() != null ? ticket.getSentiment() : "NEUTRAL";
        String openQuestions = "Awaiting customer requirements clarification.";

        HandoffSummary summary = new HandoffSummary(
            tenantId,
            conv.getId(),
            ticket.getId(),
            issueSummary,
            verifiedFacts,
            attemptedSteps,
            promisedActions,
            sentiment,
            openQuestions,
            principal.userId()
        );
        summary = handoffSummaryRepository.save(summary);

        conv.requestHandoff();
        conversationRepository.save(conv);

        return new HandoffResponse(
            conv.getId(),
            conv.getHandoffState(),
            new HandoffSummaryResponse(
                summary.getId(),
                summary.getConversationId(),
                summary.getTicketId(),
                summary.getIssueSummary(),
                summary.getVerifiedFacts(),
                summary.getAttemptedSteps(),
                summary.getPromisedActions(),
                summary.getSentiment(),
                summary.getOpenQuestions(),
                summary.getCreatedAt()
            ),
            "Handoff requested and queued for available agent."
        );
    }

    /**
     * Assign Handoff to Agent
     */
    @Transactional
    public ConversationResponse assignHandoff(UUID tenantId, UUID conversationId, UUID agentId) {
        Conversation conv = conversationRepository.findByIdAndTenantId(conversationId, tenantId)
            .orElseThrow(() -> new IllegalArgumentException("Conversation not found: " + conversationId));

        conv.assignAgent(agentId);
        conversationRepository.save(conv);

        ticketRepository.findByIdAndTenantId(conv.getTicketId(), tenantId).ifPresent(ticket -> {
            ticket.assignAgent(agentId);
            ticketRepository.save(ticket);
        });

        return toConversationResponse(conv);
    }

    /**
     * Merge Conversations (Requires CONVERSATION_MERGE permission)
     */
    @Transactional
    public ConversationMergeRecord mergeConversations(
        UUID tenantId,
        UUID sourceConvId,
        UUID targetConvId,
        UUID mergedBy,
        String reason
    ) {
        if (sourceConvId.equals(targetConvId)) {
            throw new IllegalArgumentException("Cannot merge a conversation into itself");
        }

        Conversation source = conversationRepository.findByIdAndTenantId(sourceConvId, tenantId)
            .orElseThrow(() -> new IllegalArgumentException("Source conversation not found"));
        Conversation target = conversationRepository.findByIdAndTenantId(targetConvId, tenantId)
            .orElseThrow(() -> new IllegalArgumentException("Target conversation not found"));

        ConversationMergeRecord record = new ConversationMergeRecord(
            tenantId, source.getId(), target.getId(), mergedBy, reason
        );
        record = mergeRecordRepository.save(record);

        source.setStatus(ConversationStatus.MERGED);
        conversationRepository.save(source);

        log.info("Conversations merged: source={}, target={}, actor={}", sourceConvId, targetConvId, mergedBy);
        return record;
    }

    /**
     * Split Merged Conversations
     */
    @Transactional
    public ConversationMergeRecord splitConversations(UUID tenantId, UUID mergeRecordId, UUID splitBy) {
        ConversationMergeRecord record = mergeRecordRepository.findByIdAndTenantId(mergeRecordId, tenantId)
            .orElseThrow(() -> new IllegalArgumentException("Merge record not found: " + mergeRecordId));

        if (record.getSplitAt() != null) {
            throw new IllegalStateException("Conversation is already split");
        }

        Conversation source = conversationRepository.findByIdAndTenantId(record.getSourceConversationId(), tenantId)
            .orElseThrow(() -> new IllegalArgumentException("Source conversation not found"));

        source.setStatus(ConversationStatus.ACTIVE);
        conversationRepository.save(source);

        record.split(splitBy);
        return mergeRecordRepository.save(record);
    }

    /**
     * Customer Preferences & Consent
     */
    @Transactional
    public CustomerPreferencesResponse updatePreferences(UUID tenantId, UUID customerId, ChannelPreferenceUpdateRequest req) {
        CustomerPreferences prefs = customerPreferencesRepository.findByTenantIdAndCustomerId(tenantId, customerId)
            .orElseGet(() -> new CustomerPreferences(tenantId, customerId));

        if (req.preferredChannel() != null) prefs.setPreferredChannel(req.preferredChannel());
        if (req.emailNotificationsEnabled() != null) prefs.setEmailNotificationsEnabled(req.emailNotificationsEnabled());
        if (req.marketingConsent() != null) prefs.setMarketingConsent(req.marketingConsent());

        prefs = customerPreferencesRepository.save(prefs);
        return new CustomerPreferencesResponse(
            prefs.getCustomerId(),
            prefs.getPreferredChannel(),
            prefs.isEmailNotificationsEnabled(),
            prefs.isMarketingConsent(),
            prefs.getUpdatedAt()
        );
    }

    @Transactional(readOnly = true)
    public CustomerPreferencesResponse getPreferences(UUID tenantId, UUID customerId) {
        CustomerPreferences prefs = customerPreferencesRepository.findByTenantIdAndCustomerId(tenantId, customerId)
            .orElseGet(() -> new CustomerPreferences(tenantId, customerId));
        return new CustomerPreferencesResponse(
            prefs.getCustomerId(),
            prefs.getPreferredChannel(),
            prefs.isEmailNotificationsEnabled(),
            prefs.isMarketingConsent(),
            prefs.getUpdatedAt()
        );
    }

    @Transactional(readOnly = true)
    public List<ChannelIdentityResponse> getCustomerIdentities(UUID tenantId, UUID customerId) {
        return channelIdentityRepository.findByTenantIdAndCustomerId(tenantId, customerId).stream()
            .map(i -> new ChannelIdentityResponse(
                i.getId(),
                i.getCustomerId(),
                i.getChannel(),
                i.getDisplayAddress(),
                i.isVerified(),
                i.getVerifiedAt(),
                i.getConfidence()
            )).toList();
    }

    @Transactional(readOnly = true)
    public ConversationResponse getConversation(UUID tenantId, UUID conversationId) {
        Conversation conv = conversationRepository.findByIdAndTenantId(conversationId, tenantId)
            .orElseThrow(() -> new IllegalArgumentException("Conversation not found: " + conversationId));
        return toConversationResponse(conv);
    }

    private ConversationResponse toConversationResponse(Conversation conv) {
        return new ConversationResponse(
            conv.getId(),
            conv.getTenantId(),
            conv.getTicketId(),
            conv.getStatus(),
            conv.getPrimaryCustomerId(),
            conv.getAssignedAgentId(),
            conv.getHandoffState(),
            conv.getHandoffRequestedAt(),
            conv.getPreferredChannel(),
            conv.getVersion(),
            conv.getCreatedAt(),
            conv.getUpdatedAt()
        );
    }

    private UUID resolveTenant(String tenantPublicKey) {
        try {
            return UUID.fromString(tenantPublicKey);
        } catch (Exception e) {
            return UUID.fromString(defaultTenantId);
        }
    }

    private String computePayloadHash(byte[] rawBody) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(md.digest(rawBody));
        } catch (Exception e) {
            return UUID.randomUUID().toString();
        }
    }

    private Optional<Ticket> findReferencedTicket(UUID tenantId, String subject) {
        if (subject == null || subject.isBlank()) return Optional.empty();
        Matcher matcher = TICKET_REF_PATTERN.matcher(subject);
        if (matcher.find()) {
            String ref = matcher.group(1);
            try {
                UUID ticketId = UUID.fromString(ref);
                return ticketRepository.findByIdAndTenantId(ticketId, tenantId);
            } catch (IllegalArgumentException e) {
                return ticketRepository.findByTicketNumberAndTenantId("TCK-" + ref, tenantId)
                    .or(() -> ticketRepository.findByTicketNumberAndTenantId(ref, tenantId));
            }
        }
        return Optional.empty();
    }
}
