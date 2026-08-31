package com.resolveiq.analysis.application.service;

import com.resolveiq.analysis.application.dto.AnalysisGovernanceSummary;
import com.resolveiq.analysis.application.dto.AnalysisTraceResponse;
import com.resolveiq.analysis.domain.model.AnalysisResult;
import com.resolveiq.analysis.domain.repository.AnalysisResultRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class AnalysisGovernanceService {
    private final AnalysisResultRepository repository;

    public AnalysisGovernanceService(AnalysisResultRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public AnalysisGovernanceSummary summary(UUID tenantId) {
        List<AnalysisResult> traces = repository.findTop100ByTenantIdOrderByCreatedAtDesc(tenantId);
        Object[] usage = repository.aggregateUsageByTenantId(tenantId);
        return new AnalysisGovernanceSummary(
            repository.countByTenantId(tenantId),
            repository.countByTenantIdAndValidationOutcome(tenantId, "VALID"),
            repository.countByTenantIdAndGuardrailOutcome(tenantId, "BLOCKED"),
            repository.countByTenantIdAndValidationOutcomeStartingWith(tenantId, "FALLBACK_"),
            number(usage, 0), number(usage, 1), number(usage, 2),
            traces.stream().map(this::toResponse).toList()
        );
    }

    @Transactional(readOnly = true)
    public Page<AnalysisTraceResponse> invocations(UUID tenantId, int page, int size) {
        int safePage = Math.max(0, page);
        int safeSize = Math.max(1, Math.min(size, 100));
        return repository.findByTenantId(tenantId,
                PageRequest.of(safePage, safeSize, Sort.by(Sort.Direction.DESC, "createdAt")))
            .map(this::toResponse);
    }

    private long number(Object[] values, int index) {
        return values != null && values.length > index && values[index] instanceof Number value ? value.longValue() : 0L;
    }

    private AnalysisTraceResponse toResponse(AnalysisResult item) {
        return new AnalysisTraceResponse(
            item.getId(), item.getTicketId(), item.getIntent(), item.getCategory(), item.getModelName(),
            item.getPromptVersion(), item.getValidationOutcome(), item.getGuardrailOutcome(), item.getGuardrailFindings(),
            item.getInputTokens(), item.getOutputTokens(), item.getEstimatedCostMicros(), item.getLatencyMs(), item.getCreatedAt()
        );
    }
}
