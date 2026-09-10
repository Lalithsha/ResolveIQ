package com.resolveiq.ticket.application.service.resolution;

import com.resolveiq.ticket.application.dto.resolution.ResolutionDtos.*;
import com.resolveiq.ticket.domain.model.Ticket;
import com.resolveiq.ticket.domain.model.TicketStatus;
import com.resolveiq.ticket.domain.model.resolution.*;
import com.resolveiq.ticket.domain.repository.TicketRepository;
import com.resolveiq.ticket.domain.repository.resolution.ResolutionOutcomeRepository;
import com.resolveiq.ticket.domain.repository.resolution.TicketResolutionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.scheduling.annotation.Scheduled;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;

@Service
@Transactional
public class ResolutionService implements ResolutionServicePort {

    private static final Logger log = LoggerFactory.getLogger(ResolutionService.class);

    private final TicketRepository ticketRepository;
    private final TicketResolutionRepository resolutionRepository;
    private final ResolutionOutcomeRepository outcomeRepository;

    public ResolutionService(TicketRepository ticketRepository,
                             TicketResolutionRepository resolutionRepository,
                             ResolutionOutcomeRepository outcomeRepository) {
        this.ticketRepository = ticketRepository;
        this.resolutionRepository = resolutionRepository;
        this.outcomeRepository = outcomeRepository;
    }

    public ResolutionAttemptResponse resolveTicket(UUID tenantId, UUID ticketId, UUID resolverId, String solutionFingerprint) {
        Ticket ticket = ticketRepository.findByIdAndTenantId(ticketId, tenantId)
            .orElseThrow(() -> new NoSuchElementException("Ticket not found: " + ticketId));

        int nextAttempt = resolutionRepository.findTopByTenantIdAndTicketIdOrderByAttemptNumberDesc(tenantId, ticketId)
            .map(r -> r.getAttemptNumber() + 1)
            .orElse(1);

        ticket.transitionTo(TicketStatus.RESOLVED);
        ticketRepository.save(ticket);

        TicketResolution resolution = new TicketResolution(
            tenantId, ticketId, nextAttempt, resolverId, solutionFingerprint
        );
        resolution = resolutionRepository.save(resolution);
        log.info("Ticket {} resolved (attempt {}) by agent {}", ticketId, nextAttempt, resolverId);

        return toAttemptResponse(resolution, List.of());
    }

    public ResolutionAttemptResponse recordOutcome(UUID tenantId, UUID ticketId, UUID customerId,
                                                   String ratingStr, String reason) {
        Ticket ticket = ticketRepository.findByIdAndTenantId(ticketId, tenantId)
            .orElseThrow(() -> new NoSuchElementException("Ticket not found: " + ticketId));

        if (customerId == null || !customerId.equals(ticket.getCustomerId())) {
            throw new org.springframework.security.access.AccessDeniedException("Missing customer identity or customer does not own ticket: " + ticketId);
        }

        TicketResolution resolution = resolutionRepository.findTopByTenantIdAndTicketIdOrderByAttemptNumberDesc(tenantId, ticketId)
            .orElseThrow(() -> new NoSuchElementException("No active resolution attempt for ticket: " + ticketId));

        OutcomeRating rating = OutcomeRating.valueOf(ratingStr.toUpperCase());

        ResolutionOutcome outcome = new ResolutionOutcome(
            tenantId, resolution.getId(), OutcomeSource.CUSTOMER, rating, reason, 1
        );
        outcomeRepository.save(outcome);

        if (rating == OutcomeRating.YES) {
            resolution.setStatus(ResolutionAttemptStatus.CONFIRMED);
            // 24-hour grace period before automated closure
            resolution.setScheduledClosureAt(Instant.now().plus(24, ChronoUnit.HOURS));
            int score = ResolutionScoreCalculator.calculateScoreV1(OutcomeRating.YES, false, false, false);
            resolution.setScore(score);
            log.info("Resolution attempt {} for ticket {} confirmed YES. Scheduled closure in 24h. Score: {}",
                resolution.getId(), ticketId, score);
        } else if (rating == OutcomeRating.PARTLY) {
            resolution.setStatus(ResolutionAttemptStatus.PARTIAL);
            if (ticket.getStatus() == TicketStatus.RESOLVED) {
                ticket.transitionTo(TicketStatus.IN_PROGRESS);
                ticketRepository.save(ticket);
            }
            int score = ResolutionScoreCalculator.calculateScoreV1(OutcomeRating.PARTLY, false, false, false);
            resolution.setScore(score);
            log.info("Resolution attempt {} for ticket {} marked PARTLY. Reopened to IN_PROGRESS. Score: {}",
                resolution.getId(), ticketId, score);
        } else if (rating == OutcomeRating.NO) {
            resolution.setStatus(ResolutionAttemptStatus.REJECTED);
            if (ticket.getStatus() == TicketStatus.RESOLVED) {
                ticket.transitionTo(TicketStatus.IN_PROGRESS);
                ticketRepository.save(ticket);
            }
            int score = ResolutionScoreCalculator.calculateScoreV1(OutcomeRating.NO, false, false, false);
            resolution.setScore(score);
            log.info("Resolution attempt {} for ticket {} REJECTED. Reopened to IN_PROGRESS immediately. Score: {}",
                resolution.getId(), ticketId, score);
        }

        resolution = resolutionRepository.save(resolution);
        List<ResolutionOutcome> allOutcomes = outcomeRepository.findByTenantIdAndResolutionIdOrderByOccurredAtAsc(tenantId, resolution.getId());
        return toAttemptResponse(resolution, allOutcomes);
    }

    public ResolutionAttemptResponse reopenTicket(UUID tenantId, UUID ticketId, UUID customerId, String reason) {
        Ticket ticket = ticketRepository.findByIdAndTenantId(ticketId, tenantId)
            .orElseThrow(() -> new NoSuchElementException("Ticket not found: " + ticketId));

        if (customerId == null || !customerId.equals(ticket.getCustomerId())) {
            throw new org.springframework.security.access.AccessDeniedException("Missing customer identity or customer does not own ticket: " + ticketId);
        }

        TicketResolution resolution = resolutionRepository.findTopByTenantIdAndTicketIdOrderByAttemptNumberDesc(tenantId, ticketId)
            .orElseThrow(() -> new NoSuchElementException("No resolution attempt found for ticket: " + ticketId));

        ResolutionOutcome outcome = new ResolutionOutcome(
            tenantId, resolution.getId(), OutcomeSource.REOPEN, OutcomeRating.NO, reason, 2
        );
        outcomeRepository.save(outcome);

        resolution.setStatus(ResolutionAttemptStatus.SUPERSEDED);
        int score = ResolutionScoreCalculator.calculateScoreV1(OutcomeRating.NO, true, false, false);
        resolution.setScore(score);
        resolutionRepository.save(resolution);

        if (ticket.getStatus() == TicketStatus.RESOLVED) {
            ticket.transitionTo(TicketStatus.IN_PROGRESS);
            ticketRepository.save(ticket);
        }

        log.info("Ticket {} explicitly reopened by customer {}. Resolution {} SUPERSEDED with score {}",
            ticketId, customerId, resolution.getId(), score);

        List<ResolutionOutcome> allOutcomes = outcomeRepository.findByTenantIdAndResolutionIdOrderByOccurredAtAsc(tenantId, resolution.getId());
        return toAttemptResponse(resolution, allOutcomes);
    }

    @Transactional(readOnly = true)
    public List<ResolutionAttemptResponse> getResolutionHistory(UUID tenantId, UUID ticketId) {
        List<TicketResolution> attempts = resolutionRepository.findByTenantIdAndTicketIdOrderByAttemptNumberAsc(tenantId, ticketId);
        List<ResolutionAttemptResponse> result = new ArrayList<>();
        for (TicketResolution attempt : attempts) {
            List<ResolutionOutcome> outcomes = outcomeRepository.findByTenantIdAndResolutionIdOrderByOccurredAtAsc(tenantId, attempt.getId());
            result.add(toAttemptResponse(attempt, outcomes));
        }
        return result;
    }

    @Transactional(readOnly = true)
    public ResolutionMetricsResponse getResolutionMetrics(UUID tenantId) {
        long eligible = resolutionRepository.countByTenantId(tenantId);
        if (eligible == 0) {
            return new ResolutionMetricsResponse(0, 0, 0.0, 0.0, 0.0, 0);
        }

        long confirmed = resolutionRepository.countByTenantIdAndStatus(tenantId, ResolutionAttemptStatus.CONFIRMED);
        long rejected = resolutionRepository.countByTenantIdAndStatus(tenantId, ResolutionAttemptStatus.REJECTED);
        long partial = resolutionRepository.countByTenantIdAndStatus(tenantId, ResolutionAttemptStatus.PARTIAL);
        long responded = confirmed + rejected + partial;

        double feedbackCoverage = (double) responded / eligible;
        double verifiedSuccessRate = (double) confirmed / eligible;

        List<TicketResolution> all = resolutionRepository.findAll();
        long firstContactConfirmed = all.stream()
            .filter(r -> r.getTenantId().equals(tenantId) && r.getAttemptNumber() == 1 && r.getStatus() == ResolutionAttemptStatus.CONFIRMED)
            .count();
        double fcrRate = (double) firstContactConfirmed / eligible;

        int totalScore = all.stream()
            .filter(r -> r.getTenantId().equals(tenantId))
            .mapToInt(TicketResolution::getScore)
            .sum();
        int avgScore = (int) (totalScore / eligible);

        return new ResolutionMetricsResponse(
            eligible, responded, feedbackCoverage, verifiedSuccessRate, fcrRate, avgScore
        );
    }

    @Scheduled(fixedDelayString = "${resolveiq.resolution.lifecycle-delay-ms:60000}")
    public void processResolutionLifecycle() {
        Instant now = Instant.now();
        for (TicketResolution resolution : resolutionRepository
            .findTop100ByStatusAndScheduledClosureAtLessThanEqualOrderByScheduledClosureAtAsc(
                ResolutionAttemptStatus.CONFIRMED, now)) {
            closeIfStillResolved(resolution, "Confirmed resolution grace period completed");
        }
        for (TicketResolution resolution : resolutionRepository
            .findTop100ByStatusAndConfirmationWindowExpiresAtLessThanEqualOrderByConfirmationWindowExpiresAtAsc(
                ResolutionAttemptStatus.AWAITING_CONFIRMATION, now)) {
            resolution.setStatus(ResolutionAttemptStatus.NO_RESPONSE);
            resolution.setScore(ResolutionScoreCalculator.calculateScoreV1(
                OutcomeRating.NO_RESPONSE, false, false, false));
            resolutionRepository.save(resolution);
            closeIfStillResolved(resolution, "Resolution confirmation window expired without customer response");
        }
    }

    private void closeIfStillResolved(TicketResolution resolution, String reason) {
        ticketRepository.findByIdAndTenantId(resolution.getTicketId(), resolution.getTenantId()).ifPresent(ticket -> {
            if (ticket.getStatus() == TicketStatus.RESOLVED) {
                ticket.transitionTo(TicketStatus.CLOSED);
                ticketRepository.save(ticket);
                log.info("Ticket {} automatically closed: {}", ticket.getId(), reason);
            }
        });
        resolution.setScheduledClosureAt(null);
        resolutionRepository.save(resolution);
    }

    private ResolutionAttemptResponse toAttemptResponse(TicketResolution r, List<ResolutionOutcome> outcomes) {
        List<OutcomeItemResponse> outcomeDtos = outcomes.stream()
            .map(o -> new OutcomeItemResponse(o.getId(), o.getSource().name(), o.getRating().name(), o.getReason(), o.getOccurredAt(), o.getWeight()))
            .toList();

        return new ResolutionAttemptResponse(
            r.getId(), r.getTicketId(), r.getAttemptNumber(), r.getResolverId(),
            r.getResolvedAt(), r.getConfirmationWindowExpiresAt(), r.getScheduledClosureAt(),
            r.getStatus().name(), r.getScore(), r.getScoreFormulaVersion(), outcomeDtos
        );
    }
}
