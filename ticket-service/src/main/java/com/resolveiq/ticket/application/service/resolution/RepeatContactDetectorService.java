package com.resolveiq.ticket.application.service.resolution;

import com.resolveiq.ticket.domain.model.Ticket;
import com.resolveiq.ticket.domain.model.TicketStatus;
import com.resolveiq.ticket.domain.model.resolution.*;
import com.resolveiq.ticket.domain.repository.TicketRepository;
import com.resolveiq.ticket.domain.repository.resolution.RepeatContactSignalRepository;
import com.resolveiq.ticket.domain.repository.resolution.ResolutionOutcomeRepository;
import com.resolveiq.ticket.domain.repository.resolution.TicketResolutionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@Transactional
public class RepeatContactDetectorService {

    private static final Logger log = LoggerFactory.getLogger(RepeatContactDetectorService.class);
    private static final Duration REPEAT_WINDOW = Duration.ofDays(7);

    private final TicketRepository ticketRepository;
    private final TicketResolutionRepository resolutionRepository;
    private final ResolutionOutcomeRepository outcomeRepository;
    private final RepeatContactSignalRepository repeatSignalRepository;

    public RepeatContactDetectorService(TicketRepository ticketRepository,
                                        TicketResolutionRepository resolutionRepository,
                                        ResolutionOutcomeRepository outcomeRepository,
                                        RepeatContactSignalRepository repeatSignalRepository) {
        this.ticketRepository = ticketRepository;
        this.resolutionRepository = resolutionRepository;
        this.outcomeRepository = outcomeRepository;
        this.repeatSignalRepository = repeatSignalRepository;
    }

    public Optional<RepeatContactSignal> checkAndRecordRepeatContact(Ticket currentTicket) {
        UUID tenantId = currentTicket.getTenantId();
        UUID customerId = currentTicket.getCustomerId();

        List<Ticket> customerTickets = ticketRepository.findByTenantIdAndCustomerIdOrderByCreatedAtDesc(tenantId, customerId);
        Instant cutoff = currentTicket.getCreatedAt().minus(REPEAT_WINDOW);

        for (Ticket prior : customerTickets) {
            if (prior.getId().equals(currentTicket.getId())) continue;
            if (prior.getCreatedAt().isBefore(cutoff)) continue;

            // Only prior resolved or closed tickets count as potential repeat contacts
            if (prior.getStatus() == TicketStatus.RESOLVED || prior.getStatus() == TicketStatus.CLOSED) {
                double similarity = computeSimilarity(currentTicket, prior);
                if (similarity >= 0.50) {
                    long deltaSeconds = Duration.between(prior.getCreatedAt(), currentTicket.getCreatedAt()).abs().toSeconds();
                    RepeatContactStatus status = similarity >= 0.75 ? RepeatContactStatus.CONFIRMED : RepeatContactStatus.SUGGESTED;

                    RepeatContactSignal signal = new RepeatContactSignal(
                        tenantId, customerId, prior.getId(), currentTicket.getId(),
                        similarity, deltaSeconds, status
                    );
                    signal = repeatSignalRepository.save(signal);
                    log.info("Detected repeat contact signal {} for customer {} between prior ticket {} and current {}",
                        signal.getId(), customerId, prior.getId(), currentTicket.getId());

                    if (status == RepeatContactStatus.CONFIRMED) {
                        applyRepeatContactPenalty(tenantId, prior.getId());
                    }

                    return Optional.of(signal);
                }
            }
        }
        return Optional.empty();
    }

    private void applyRepeatContactPenalty(UUID tenantId, UUID priorTicketId) {
        resolutionRepository.findTopByTenantIdAndTicketIdOrderByAttemptNumberDesc(tenantId, priorTicketId)
            .ifPresent(res -> {
                outcomeRepository.save(new ResolutionOutcome(
                    tenantId, res.getId(), OutcomeSource.REPEAT_CONTACT,
                    OutcomeRating.NO, "Customer opened repeat contact ticket within 7 days", 2
                ));
                // Recalculate score with repeat penalty
                int newScore = ResolutionScoreCalculator.calculateScoreV1(
                    res.getStatus() == ResolutionAttemptStatus.CONFIRMED ? OutcomeRating.YES : OutcomeRating.NO_RESPONSE,
                    false, true, false
                );
                res.setScore(newScore);
                resolutionRepository.save(res);
                log.info("Applied repeat contact penalty to resolution {} on ticket {}. New score: {}",
                    res.getId(), priorTicketId, newScore);
            });
    }

    private double computeSimilarity(Ticket t1, Ticket t2) {
        double score = 0.0;
        if (t1.getCategory() != null && t1.getCategory().equalsIgnoreCase(t2.getCategory())) {
            score += 0.50;
        }
        if (t1.getSubject() != null && t2.getSubject() != null) {
            String s1 = t1.getSubject().toLowerCase();
            String s2 = t2.getSubject().toLowerCase();
            if (s1.equals(s2)) {
                score += 0.50;
            } else if (s1.contains(s2) || s2.contains(s1)) {
                score += 0.35;
            }
        }
        return Math.min(1.0, score);
    }
}
