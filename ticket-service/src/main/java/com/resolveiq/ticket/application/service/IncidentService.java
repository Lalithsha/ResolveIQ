package com.resolveiq.ticket.application.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.resolveiq.contracts.event.EventEnvelope;
import com.resolveiq.contracts.event.IncidentEvents;
import com.resolveiq.ticket.application.dto.IncidentDtos.*;
import com.resolveiq.ticket.application.port.NotificationPort;
import com.resolveiq.ticket.application.port.SignalAdapter;
import com.resolveiq.ticket.application.port.TicketSimilarityPort;
import com.resolveiq.ticket.domain.model.*;
import com.resolveiq.ticket.domain.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
public class IncidentService {

    private static final Logger log = LoggerFactory.getLogger(IncidentService.class);
    private static final Pattern ERROR_CODE_PATTERN = Pattern.compile("\\b(ERR_[A-Z0-9_]+|SAML_[A-Z0-9_]+|AUTH_[A-Z0-9_]+|GATEWAY_[A-Z0-9_]+)\\b");

    private final SupportIncidentRepository incidentRepository;
    private final IncidentClusterRepository clusterRepository;
    private final IncidentTicketLinkRepository linkRepository;
    private final IncidentComponentRepository componentRepository;
    private final IncidentUpdateRepository updateRepository;
    private final CustomerImpactRepository impactRepository;
    private final NotificationSubscriptionRepository subscriptionRepository;
    private final NotificationDeliveryRepository deliveryRepository;
    private final TicketRepository ticketRepository;
    private final OutboxEventRepository outboxRepository;
    private final TicketSimilarityPort similarityPort;
    private final SignalAdapter signalAdapter;
    private final NotificationPort notificationPort;
    private final ObjectMapper objectMapper;

    public IncidentService(
        SupportIncidentRepository incidentRepository,
        IncidentClusterRepository clusterRepository,
        IncidentTicketLinkRepository linkRepository,
        IncidentComponentRepository componentRepository,
        IncidentUpdateRepository updateRepository,
        CustomerImpactRepository impactRepository,
        NotificationSubscriptionRepository subscriptionRepository,
        NotificationDeliveryRepository deliveryRepository,
        TicketRepository ticketRepository,
        OutboxEventRepository outboxRepository,
        TicketSimilarityPort similarityPort,
        SignalAdapter signalAdapter,
        NotificationPort notificationPort,
        ObjectMapper objectMapper
    ) {
        this.incidentRepository = incidentRepository;
        this.clusterRepository = clusterRepository;
        this.linkRepository = linkRepository;
        this.componentRepository = componentRepository;
        this.updateRepository = updateRepository;
        this.impactRepository = impactRepository;
        this.subscriptionRepository = subscriptionRepository;
        this.deliveryRepository = deliveryRepository;
        this.ticketRepository = ticketRepository;
        this.outboxRepository = outboxRepository;
        this.similarityPort = similarityPort;
        this.signalAdapter = signalAdapter;
        this.notificationPort = notificationPort;
        this.objectMapper = objectMapper;
    }

    private java.time.Clock clock = java.time.Clock.systemUTC();

    public void setClock(java.time.Clock clock) {
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public Page<IncidentResponse> listIncidents(UUID tenantId, IncidentStatus status, IncidentSeverity severity, Pageable pageable) {
        Page<SupportIncident> page;
        if (status != null && severity != null) {
            page = incidentRepository.findByTenantIdAndStatusAndSeverity(tenantId, status, severity, pageable);
        } else if (status != null) {
            page = incidentRepository.findByTenantIdAndStatus(tenantId, status, pageable);
        } else if (severity != null) {
            page = incidentRepository.findByTenantIdAndSeverity(tenantId, severity, pageable);
        } else {
            page = incidentRepository.findByTenantId(tenantId, pageable);
        }

        return page.map(inc -> toIncidentResponse(tenantId, inc));
    }

    @Transactional(readOnly = true)
    public IncidentResponse getIncident(UUID tenantId, UUID id) {
        SupportIncident incident = incidentRepository.findByTenantIdAndId(tenantId, id)
            .orElseThrow(() -> new IllegalArgumentException("Incident not found: " + id));
        return toIncidentResponse(tenantId, incident);
    }

    @Transactional(readOnly = true)
    public List<IncidentProposalResponse> listProposals(UUID tenantId) {
        List<IncidentCluster> clusters = clusterRepository.findByTenantIdAndStatus(tenantId, ClusterStatus.PROPOSED);
        List<IncidentProposalResponse> result = new ArrayList<>();

        for (IncidentCluster cluster : clusters) {
            List<IncidentTicketLink> links = cluster.getIncidentId() != null
                ? linkRepository.findByIncidentId(cluster.getIncidentId())
                : List.of();

            List<IncidentProposalResponse.SampleTicketDto> samples = new ArrayList<>();
            for (IncidentTicketLink link : links) {
                ticketRepository.findByIdAndTenantId(link.getTicketId(), tenantId).ifPresent(t -> {
                    samples.add(new IncidentProposalResponse.SampleTicketDto(
                        t.getId(),
                        t.getTicketNumber(),
                        t.getSubject(),
                        t.getCustomerId().toString(),
                        link.getSimilarityScore()
                    ));
                });
            }

            List<String> fingerprints = parseFingerprints(cluster.getErrorFingerprints());

            result.add(new IncidentProposalResponse(
                cluster.getId(),
                cluster.getIncidentId(),
                cluster.getDominantCategory(),
                cluster.getProduct(),
                cluster.getTicketCount(),
                cluster.getBaselineCount(),
                cluster.getAnomalyScore(),
                samples,
                fingerprints,
                cluster.getStatus(),
                cluster.getExplanation()
            ));
        }
        return result;
    }

    @Transactional
    public IncidentResponse confirmIncident(UUID tenantId, UUID incidentId, UUID actorId) {
        SupportIncident incident = incidentRepository.findByTenantIdAndId(tenantId, incidentId)
            .orElseThrow(() -> new IllegalArgumentException("Incident not found: " + incidentId));

        incident.confirm(actorId);
        incidentRepository.save(incident);

        // Confirm linked cluster if present
        clusterRepository.findAll().stream()
            .filter(c -> tenantId.equals(c.getTenantId()) && incidentId.equals(c.getIncidentId()))
            .forEach(c -> {
                c.setStatus(ClusterStatus.CONFIRMED);
                clusterRepository.save(c);
            });

        publishIncidentUpdatedEvent(tenantId, incident, null, "CONFIRMED", actorId);
        return toIncidentResponse(tenantId, incident);
    }

    @Transactional
    public IncidentResponse dismissIncident(UUID tenantId, UUID incidentId, UUID actorId, String reason) {
        SupportIncident incident = incidentRepository.findByTenantIdAndId(tenantId, incidentId)
            .orElseThrow(() -> new IllegalArgumentException("Incident not found: " + incidentId));

        incident.dismiss(reason != null ? reason : "Dismissed by incident lead");
        incidentRepository.save(incident);

        clusterRepository.findAll().stream()
            .filter(c -> tenantId.equals(c.getTenantId()) && incidentId.equals(c.getIncidentId()))
            .forEach(c -> {
                c.setStatus(ClusterStatus.DISMISSED);
                clusterRepository.save(c);
            });

        publishIncidentUpdatedEvent(tenantId, incident, null, "DISMISSED", actorId);
        return toIncidentResponse(tenantId, incident);
    }

    @Transactional
    public IncidentResponse transitionIncident(UUID tenantId, UUID incidentId, IncidentStatus newStatus, String resolutionSummary, UUID actorId) {
        SupportIncident incident = incidentRepository.findByTenantIdAndId(tenantId, incidentId)
            .orElseThrow(() -> new IllegalArgumentException("Incident not found: " + incidentId));

        incident.transition(newStatus, resolutionSummary, actorId);
        incidentRepository.save(incident);

        publishIncidentUpdatedEvent(tenantId, incident, null, newStatus.name(), actorId);
        return toIncidentResponse(tenantId, incident);
    }

    @Transactional
    public void linkTicket(UUID tenantId, UUID incidentId, UUID ticketId, UUID actorId, double similarityScore) {
        SupportIncident incident = incidentRepository.findByTenantIdAndId(tenantId, incidentId)
            .orElseThrow(() -> new IllegalArgumentException("Incident not found: " + incidentId));

        Ticket ticket = ticketRepository.findByIdAndTenantId(ticketId, tenantId)
            .orElseThrow(() -> new IllegalArgumentException("Ticket not found: " + ticketId));

        if (!linkRepository.existsByIncidentIdAndTicketId(incidentId, ticketId)) {
            IncidentTicketLink link = new IncidentTicketLink(
                UUID.randomUUID(),
                incidentId,
                ticketId,
                LinkSource.MANUAL,
                similarityScore > 0 ? similarityScore : 0.85,
                actorId
            );
            linkRepository.save(link);

            if (impactRepository.findByTenantIdAndIncidentIdAndCustomerId(tenantId, incidentId, ticket.getCustomerId()).isEmpty()) {
                CustomerImpact impact = new CustomerImpact(
                    UUID.randomUUID(),
                    tenantId,
                    incidentId,
                    ticket.getCustomerId(),
                    ticketId,
                    "DIRECT"
                );
                impactRepository.save(impact);
            }
        }
    }

    @Transactional
    public void unlinkTicket(UUID tenantId, UUID incidentId, UUID ticketId, UUID actorId, String reason) {
        SupportIncident incident = incidentRepository.findByTenantIdAndId(tenantId, incidentId)
            .orElseThrow(() -> new IllegalArgumentException("Incident not found: " + incidentId));

        Ticket ticket = ticketRepository.findByIdAndTenantId(ticketId, tenantId)
            .orElseThrow(() -> new IllegalArgumentException("Ticket not found: " + ticketId));

        IncidentTicketLink link = linkRepository.findByIncidentIdAndTicketId(incidentId, ticketId)
            .orElseThrow(() -> new IllegalArgumentException("Link not found for ticket: " + ticketId));

        link.unlink(actorId, reason != null ? reason : "Manual unlink");
        linkRepository.save(link);

        // Recompute customer impact when the last active link for this customer is removed
        boolean hasOtherActiveLinks = linkRepository.findByIncidentId(incidentId).stream()
            .filter(l -> l.getUnlinkedAt() == null && !l.getTicketId().equals(ticketId))
            .anyMatch(l -> ticketRepository.findByIdAndTenantId(l.getTicketId(), tenantId)
                .map(t -> t.getCustomerId().equals(ticket.getCustomerId()))
                .orElse(false));

        if (!hasOtherActiveLinks) {
            impactRepository.findByTenantIdAndIncidentIdAndCustomerId(tenantId, incidentId, ticket.getCustomerId())
                .ifPresent(impactRepository::delete);
            log.info("Customer {} removed from incident {} impact: all linked tickets unlinked", ticket.getCustomerId(), incidentId);
        }
    }

    @Transactional
    public IncidentUpdateResponse createUpdate(UUID tenantId, UUID incidentId, CreateUpdateRequest request, UUID authorId) {
        SupportIncident incident = incidentRepository.findByTenantIdAndId(tenantId, incidentId)
            .orElseThrow(() -> new IllegalArgumentException("Incident not found: " + incidentId));

        int nextUpdateNumber = (int) updateRepository.countByTenantIdAndIncidentId(tenantId, incidentId) + 1;
        long affectedCount = impactRepository.countByTenantIdAndIncidentId(tenantId, incidentId);

        IncidentUpdate update = new IncidentUpdate(
            UUID.randomUUID(),
            tenantId,
            incidentId,
            nextUpdateNumber,
            IncidentUpdateStatus.DRAFT,
            request.title(),
            request.message(),
            request.audienceType(),
            (int) affectedCount,
            authorId
        );
        updateRepository.save(update);

        return toUpdateResponse(update);
    }

    @Transactional
    public IncidentUpdateResponse approveUpdate(UUID tenantId, UUID incidentId, UUID updateId, UUID approverId, Set<String> roles) {
        SupportIncident incident = incidentRepository.findByTenantIdAndId(tenantId, incidentId)
            .orElseThrow(() -> new IllegalArgumentException("Incident not found: " + incidentId));

        IncidentUpdate update = updateRepository.findByTenantIdAndId(tenantId, updateId)
            .orElseThrow(() -> new IllegalArgumentException("Update not found: " + updateId));

        if (!update.getIncidentId().equals(incidentId)) {
            throw new IllegalArgumentException("Update does not belong to incident: " + incidentId);
        }

        // Two-person rule enforcement for HIGH/CRITICAL incidents
        if ((incident.getSeverity() == IncidentSeverity.HIGH || incident.getSeverity() == IncidentSeverity.CRITICAL)
            && update.getAuthorId().equals(approverId)) {
            throw new IllegalStateException("Two-person rule violation: author cannot approve own update on " + incident.getSeverity() + " incident");
        }

        long currentAudienceCount = impactRepository.countByTenantIdAndIncidentId(tenantId, incidentId);
        update.approve(approverId, (int) currentAudienceCount);
        updateRepository.save(update);

        return toUpdateResponse(update);
    }

    @Transactional
    public IncidentUpdateResponse publishUpdate(UUID tenantId, UUID incidentId, UUID updateId, UUID publisherId) {
        SupportIncident incident = incidentRepository.findByTenantIdAndId(tenantId, incidentId)
            .orElseThrow(() -> new IllegalArgumentException("Incident not found: " + incidentId));

        IncidentUpdate update = updateRepository.findByTenantIdAndId(tenantId, updateId)
            .orElseThrow(() -> new IllegalArgumentException("Update not found: " + updateId));

        if (!update.getIncidentId().equals(incidentId)) {
            throw new IllegalArgumentException("Update does not belong to incident: " + incidentId);
        }

        if (update.getStatus() != IncidentUpdateStatus.APPROVED) {
            throw new IllegalStateException("Only approved updates can be published");
        }

        update.publish(publisherId);
        updateRepository.save(update);

        // Deliver to all affected customers
        List<CustomerImpact> impacts = impactRepository.findByTenantIdAndIncidentId(tenantId, incidentId);
        for (CustomerImpact impact : impacts) {
            String deliveryKey = String.format("%s:%s:%s:PORTAL", tenantId, update.getId(), impact.getCustomerId());
            if (deliveryRepository.findByTenantIdAndUpdateIdAndRecipientCustomerIdAndChannel(tenantId, update.getId(), impact.getCustomerId(), "PORTAL").isEmpty()) {
                NotificationDelivery delivery = new NotificationDelivery(
                    UUID.randomUUID(),
                    tenantId,
                    update.getId(),
                    impact.getCustomerId(),
                    "PORTAL",
                    DeliveryStatus.PENDING,
                    deliveryKey
                );
                deliveryRepository.save(delivery);

                NotificationPort.DeliveryResult result = notificationPort.sendNotification(
                    tenantId,
                    update.getId(),
                    impact.getCustomerId(),
                    "PORTAL",
                    update.getTitle(),
                    update.getMessage(),
                    deliveryKey
                );

                if (result.status() == DeliveryStatus.SENT) {
                    delivery.markSent(result.providerMessageId());
                    impact.setNotified(true);
                    impactRepository.save(impact);
                } else {
                    delivery.markFailed(result.failureReason());
                }
                deliveryRepository.save(delivery);
            }
        }

        publishIncidentUpdatedEvent(tenantId, incident, update.getId(), "PUBLISHED", publisherId);
        return toUpdateResponse(update);
    }

    @Transactional(readOnly = true)
    public List<CustomerIncidentResponse> getActiveCustomerIncidents(UUID tenantId, UUID customerId) {
        List<CustomerImpact> impacts = impactRepository.findByTenantIdAndCustomerId(tenantId, customerId);
        List<CustomerIncidentResponse> results = new ArrayList<>();

        for (CustomerImpact impact : impacts) {
            incidentRepository.findByTenantIdAndId(tenantId, impact.getIncidentId()).ifPresent(inc -> {
                // Only confirmed/active incidents (never proposed, never resolved/dismissed) with published customer-safe updates
                if (inc.getStatus() != IncidentStatus.PROPOSED &&
                    inc.getStatus() != IncidentStatus.RESOLVED &&
                    inc.getStatus() != IncidentStatus.DISMISSED) {
                    List<IncidentUpdate> updates = updateRepository.findByTenantIdAndIncidentIdOrderByUpdateNumberAsc(tenantId, inc.getId());
                    List<IncidentUpdate> publishedUpdates = updates.stream()
                        .filter(u -> u.getStatus() == IncidentUpdateStatus.PUBLISHED)
                        .toList();

                    if (!publishedUpdates.isEmpty()) {
                        IncidentUpdate latestUpdate = publishedUpdates.get(publishedUpdates.size() - 1);
                        String latestCustomerSafeMsg = latestUpdate.getMessage();
                        String latestCustomerSafeTitle = latestUpdate.getTitle() != null && !latestUpdate.getTitle().isBlank()
                            ? latestUpdate.getTitle()
                            : inc.getTitle();
                        results.add(new CustomerIncidentResponse(
                            inc.getId(),
                            inc.getIncidentNumber(),
                            latestCustomerSafeTitle,
                            inc.getStatus(),
                            inc.getSeverity(),
                            latestCustomerSafeMsg,
                            latestCustomerSafeMsg,
                            inc.getUpdatedAt()
                        ));
                    }
                }
            });
        }
        return results;
    }

    @Transactional
    public void subscribeCustomer(UUID tenantId, UUID incidentId, UUID customerId, String channel) {
        if (subscriptionRepository.findByTenantIdAndIncidentIdAndCustomerIdAndChannel(tenantId, incidentId, customerId, channel).isEmpty()) {
            NotificationSubscription sub = new NotificationSubscription(
                UUID.randomUUID(),
                tenantId,
                incidentId,
                customerId,
                channel
            );
            subscriptionRepository.save(sub);
        }
    }

    @Transactional
    public DetectionRunResult runDetectionScan(UUID tenantId) {
        return runDetectionScan(tenantId, 10, 8);
    }

    @Transactional
    public DetectionRunResult runDetectionScan(UUID tenantId, int minTickets, int minCustomers) {
        Instant windowEnd = clock.instant();
        Instant windowStart = windowEnd.minus(15, ChronoUnit.MINUTES);

        List<Ticket> windowTickets = ticketRepository.findByTenantIdAndCreatedAtGreaterThanEqualOrderByCreatedAtDesc(
            tenantId,
            windowStart,
            PageRequest.of(0, 500)
        );

        List<Ticket> ticketsToEvaluate = windowTickets.stream()
            .filter(t -> t.getCreatedAt() != null && !t.getCreatedAt().isBefore(windowStart) && !t.getCreatedAt().isAfter(windowEnd))
            .toList();

        Map<String, List<Ticket>> groupedByCategory = ticketsToEvaluate.stream()
            .collect(Collectors.groupingBy(t -> t.getCategory() != null ? t.getCategory() : "GENERAL"));

        int proposalsCreated = 0;
        int ticketsLinked = 0;

        for (Map.Entry<String, List<Ticket>> entry : groupedByCategory.entrySet()) {
            String category = entry.getKey();
            List<Ticket> tickets = entry.getValue();

            if (tickets.size() < minTickets) {
                continue;
            }

            // Extract common error fingerprints
            Map<String, Integer> fingerprintCounts = new HashMap<>();
            for (Ticket t : tickets) {
                String text = (t.getSubject() != null ? t.getSubject() : "") + " " + (t.getDescription() != null ? t.getDescription() : "");
                Matcher matcher = ERROR_CODE_PATTERN.matcher(text);
                while (matcher.find()) {
                    fingerprintCounts.merge(matcher.group(1), 1, Integer::sum);
                }
            }

            String topFingerprint = fingerprintCounts.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse(null);

            Ticket seed = tickets.get(0);
            String seedText = ticketText(seed);
            List<TicketSimilarityPort.CandidateTicket> candidates = tickets.stream().skip(1)
                .map(ticket -> new TicketSimilarityPort.CandidateTicket(ticket.getId(), ticketText(ticket),
                    ticket.getCategory(), "API", firstFingerprint(ticket)))
                .toList();
            List<TicketSimilarityPort.SimilarityResult> matches = Objects.requireNonNullElse(
                similarityPort.findSimilarTickets(tenantId, seed.getId(), seedText, category, "API",
                    topFingerprint, 0.82, candidates),
                List.of());
            Set<UUID> matchedIds = matches.stream().map(TicketSimilarityPort.SimilarityResult::ticketId).collect(Collectors.toSet());
            Map<UUID, Double> measuredScores = matches.stream().collect(Collectors.toMap(
                TicketSimilarityPort.SimilarityResult::ticketId, TicketSimilarityPort.SimilarityResult::similarityScore, Math::max));
            List<Ticket> clusterTickets = tickets.stream()
                .filter(ticket -> ticket.getId().equals(seed.getId()) || matchedIds.contains(ticket.getId()))
                .toList();
            long distinctCustomers = clusterTickets.stream().map(Ticket::getCustomerId).distinct().count();
            if (clusterTickets.size() < minTickets || distinctCustomers < minCustomers) continue;

            List<Ticket> historical = ticketRepository.findByTenantIdAndCreatedAtBetweenOrderByCreatedAtAsc(
                tenantId, windowStart.minus(7, ChronoUnit.DAYS), windowStart);
            double baseline = rollingMedianBaseline(Objects.requireNonNullElse(historical, List.of()), category, windowStart);
            double anomalyRatio = clusterTickets.size() / Math.max(1.0, baseline);

            if (anomalyRatio >= 1.5) {
                String centroidHash = String.format("%s:%s:%s", tenantId, category, topFingerprint != null ? topFingerprint : "NONE");
                Optional<IncidentCluster> existingCluster = clusterRepository.findByTenantIdAndCentroidHash(tenantId, centroidHash);

                if (existingCluster.isEmpty()) {
                    String incNumber = "INC-" + (System.currentTimeMillis() % 100000);
                    String title = String.format(
                        "Spike in %s issues%s",
                        category,
                        topFingerprint != null ? " (" + topFingerprint + ")" : ""
                    );

                    SupportIncident incident = new SupportIncident(
                        UUID.randomUUID(),
                        tenantId,
                        incNumber,
                        title,
                        IncidentStatus.PROPOSED,
                        anomalyRatio >= 3.0 ? IncidentSeverity.HIGH : IncidentSeverity.MEDIUM,
                        Instant.now(),
                        UUID.fromString("00000000-0000-0000-0000-000000000000"), // System detector
                        String.format("Support Incident Radar detected a volume spike (%d compatible tickets, %.1fx baseline) in category %s.", clusterTickets.size(), anomalyRatio, category),
                        "radar-v1.0",
                        Math.min(0.98, 0.70 + (anomalyRatio * 0.05))
                    );
                    incidentRepository.save(incident);

                    List<String> fps = topFingerprint != null ? List.of(topFingerprint) : List.of();
                    String fpsJson = serializeFingerprints(fps);

                    IncidentCluster cluster = new IncidentCluster(
                        UUID.randomUUID(),
                        tenantId,
                        incident.getId(),
                        windowStart,
                        windowEnd,
                        category + ":" + (topFingerprint != null ? topFingerprint : "GENERAL"),
                        centroidHash,
                        clusterTickets.size(),
                        (int) baseline,
                        anomalyRatio,
                        category,
                        "API",
                        "GLOBAL",
                        fpsJson,
                        ClusterStatus.PROPOSED,
                        String.format("Detected %d compatible tickets in 15min window. Seven-day median baseline is %.1f.", clusterTickets.size(), baseline),
                        "radar-v1.0"
                    );
                    clusterRepository.save(cluster);

                    // Add primary component
                    IncidentComponent component = new IncidentComponent(
                        UUID.randomUUID(),
                        tenantId,
                        incident.getId(),
                        category + " Service",
                        "DEGRADED",
                        "Automated cluster detection"
                    );
                    componentRepository.save(component);

                    // Link tickets and record customer impacts
                    for (Ticket t : clusterTickets) {
                        IncidentTicketLink link = new IncidentTicketLink(
                            UUID.randomUUID(),
                            incident.getId(),
                            t.getId(),
                            LinkSource.AUTOMATIC,
                            t.getId().equals(seed.getId()) ? 1.0 : measuredScores.get(t.getId()),
                            UUID.fromString("00000000-0000-0000-0000-000000000000")
                        );
                        linkRepository.save(link);
                        ticketsLinked++;

                        if (impactRepository.findByTenantIdAndIncidentIdAndCustomerId(tenantId, incident.getId(), t.getCustomerId()).isEmpty()) {
                            CustomerImpact impact = new CustomerImpact(
                                UUID.randomUUID(),
                                tenantId,
                                incident.getId(),
                                t.getCustomerId(),
                                t.getId(),
                                "DIRECT"
                            );
                            impactRepository.save(impact);
                        }
                    }

                    publishIncidentProposedEvent(tenantId, cluster, incident, clusterTickets.stream().map(Ticket::getId).toList(), fps);
                    proposalsCreated++;
                }
            }
        }

        return new DetectionRunResult(ticketsToEvaluate.size(), proposalsCreated, ticketsLinked, "radar-v1.0");
    }

    private String ticketText(Ticket ticket) {
        return String.join(" ", Objects.toString(ticket.getSubject(), ""), Objects.toString(ticket.getDescription(), ""));
    }

    private String firstFingerprint(Ticket ticket) {
        Matcher matcher = ERROR_CODE_PATTERN.matcher(ticketText(ticket));
        return matcher.find() ? matcher.group(1) : null;
    }

    private double rollingMedianBaseline(List<Ticket> historical, String category, Instant windowStart) {
        long[] counts = new long[7 * 24 * 4];
        for (Ticket ticket : historical) {
            if (!Objects.equals(category, ticket.getCategory() != null ? ticket.getCategory() : "GENERAL")) continue;
            long minutes = ChronoUnit.MINUTES.between(windowStart.minus(7, ChronoUnit.DAYS), ticket.getCreatedAt());
            int bucket = (int) (minutes / 15);
            if (bucket >= 0 && bucket < counts.length) counts[bucket]++;
        }
        Arrays.sort(counts);
        int middle = counts.length / 2;
        return (counts[middle - 1] + counts[middle]) / 2.0;
    }

    private IncidentResponse toIncidentResponse(UUID tenantId, SupportIncident incident) {
        int linkedTickets = (int) linkRepository.countByIncidentIdAndUnlinkedAtIsNull(incident.getId());
        int affectedCustomers = (int) impactRepository.countByTenantIdAndIncidentId(tenantId, incident.getId());

        List<IncidentComponent> comps = componentRepository.findByTenantIdAndIncidentId(tenantId, incident.getId());
        List<IncidentResponse.ComponentDto> compDtos = comps.stream()
            .map(c -> new IncidentResponse.ComponentDto(c.getId(), c.getComponentName(), c.getStatus(), c.getImpactSummary()))
            .toList();

        String compName = comps.isEmpty() ? "checkout" : comps.get(0).getComponentName();
        List<OperationalSignal> signals = signalAdapter.fetchSignalsForWindow(
            tenantId,
            compName,
            incident.getDetectedAt().minusSeconds(300),
            incident.getDetectedAt().plusSeconds(300)
        );

        return new IncidentResponse(
            incident.getId(),
            incident.getIncidentNumber(),
            incident.getTitle(),
            incident.getStatus(),
            incident.getSeverity(),
            incident.getDetectedAt(),
            incident.getConfirmedAt(),
            incident.getResolvedAt(),
            incident.getOwnerUserId(),
            incident.getSummary(),
            incident.getRootCause(),
            incident.getResolutionSummary(),
            linkedTickets,
            affectedCustomers,
            compDtos,
            signals
        );
    }

    private IncidentUpdateResponse toUpdateResponse(IncidentUpdate u) {
        return new IncidentUpdateResponse(
            u.getId(),
            u.getIncidentId(),
            u.getUpdateNumber(),
            u.getStatus(),
            u.getTitle(),
            u.getMessage(),
            u.getAudienceType(),
            u.getAudienceCount(),
            u.getAuthorId(),
            u.getApprovedBy(),
            u.getApprovedAt(),
            u.getPublishedAt(),
            u.getContentDigest()
        );
    }

    private void publishIncidentProposedEvent(UUID tenantId, IncidentCluster cluster, SupportIncident incident, List<UUID> ticketIds, List<String> fingerprints) {
        IncidentEvents.IncidentProposedPayload payload = new IncidentEvents.IncidentProposedPayload(
            cluster.getId(),
            tenantId,
            cluster.getDominantCategory(),
            cluster.getProduct(),
            cluster.getTicketCount(),
            cluster.getAnomalyScore(),
            ticketIds,
            fingerprints,
            cluster.getExplanation(),
            cluster.getAlgorithmVersion(),
            Instant.now()
        );

        EventEnvelope<IncidentEvents.IncidentProposedPayload> envelope = EventEnvelope.create(
            IncidentEvents.INCIDENT_PROPOSED,
            1,
            "ticket-service",
            tenantId,
            "incident_cluster",
            cluster.getId(),
            null,
            null,
            null,
            payload
        );

        saveOutbox("incident_cluster", cluster.getId(), IncidentEvents.INCIDENT_PROPOSED, envelope);
    }

    private void publishIncidentUpdatedEvent(UUID tenantId, SupportIncident incident, UUID updateId, String updateStatus, UUID actorId) {
        long affected = impactRepository.countByTenantIdAndIncidentId(tenantId, incident.getId());
        IncidentEvents.IncidentUpdatedPayload payload = new IncidentEvents.IncidentUpdatedPayload(
            incident.getId(),
            incident.getIncidentNumber(),
            tenantId,
            incident.getTitle(),
            incident.getStatus().name(),
            incident.getSeverity().name(),
            updateId,
            updateStatus,
            incident.getResolutionSummary(),
            (int) affected,
            actorId,
            Instant.now()
        );

        EventEnvelope<IncidentEvents.IncidentUpdatedPayload> envelope = EventEnvelope.create(
            IncidentEvents.INCIDENT_UPDATED,
            1,
            "ticket-service",
            tenantId,
            "support_incident",
            incident.getId(),
            null,
            null,
            null,
            payload
        );

        saveOutbox("support_incident", incident.getId(), IncidentEvents.INCIDENT_UPDATED, envelope);
    }

    private void saveOutbox(String aggregateType, UUID aggregateId, String eventType, Object envelope) {
        try {
            String jsonPayload = objectMapper.writeValueAsString(envelope);
            OutboxEvent outboxEvent = new OutboxEvent(aggregateType, aggregateId, eventType, jsonPayload);
            outboxRepository.save(outboxEvent);
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize outbox event {}", eventType, e);
        }
    }

    private String serializeFingerprints(List<String> fps) {
        try {
            return objectMapper.writeValueAsString(fps);
        } catch (Exception e) {
            return "[]";
        }
    }

    private List<String> parseFingerprints(String json) {
        if (json == null || json.isBlank()) return List.of();
        try {
            return Arrays.asList(objectMapper.readValue(json, String[].class));
        } catch (Exception e) {
            return List.of();
        }
    }
}
