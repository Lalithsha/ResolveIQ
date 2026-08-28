package com.resolveiq.routing.application.service;

import com.resolveiq.routing.domain.model.SlaPolicy;
import com.resolveiq.routing.domain.repository.SlaPolicyRepository;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Service
public class SlaCalculationService {

    private final SlaPolicyRepository slaPolicyRepository;

    public SlaCalculationService(SlaPolicyRepository slaPolicyRepository) {
        this.slaPolicyRepository = slaPolicyRepository;
    }

    public SlaTarget calculateSlaTarget(UUID tenantId, String priority) {
        String safePriority = priority != null ? priority.toUpperCase() : "MEDIUM";
        Optional<SlaPolicy> policyOpt = slaPolicyRepository.findByTenantIdAndPriority(tenantId, safePriority);

        if (policyOpt.isPresent()) {
            SlaPolicy policy = policyOpt.get();
            Instant now = Instant.now();
            Instant firstResponseDue = now.plusSeconds(policy.getFirstResponseTargetMinutes() * 60L);
            Instant resolutionDue = now.plusSeconds(policy.getResolutionTargetMinutes() * 60L);
            return new SlaTarget(policy.getId(), firstResponseDue, resolutionDue);
        }

        // Default SLAs: CRITICAL = 1h resp / 4h res; HIGH = 2h resp / 8h res; MEDIUM = 4h resp / 24h res; LOW = 8h resp / 48h res
        Instant now = Instant.now();
        long respMinutes = switch (safePriority) {
            case "CRITICAL" -> 60;
            case "HIGH" -> 120;
            case "LOW" -> 480;
            default -> 240; // MEDIUM
        };
        long resMinutes = switch (safePriority) {
            case "CRITICAL" -> 240;
            case "HIGH" -> 480;
            case "LOW" -> 2880;
            default -> 1440; // MEDIUM
        };

        return new SlaTarget(
            null,
            now.plusSeconds(respMinutes * 60),
            now.plusSeconds(resMinutes * 60)
        );
    }

    public record SlaTarget(
        UUID policyId,
        Instant firstResponseDueAt,
        Instant resolutionDueAt
    ) {}
}
