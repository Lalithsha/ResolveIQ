package com.resolveiq.analysis.domain.model.evidence;

public enum PipelineStatus {
    QUEUED,
    EXTRACTING,
    REDACTING,
    ANALYZING,
    READY,
    PARTIAL,
    FAILED,
    BLOCKED_REDACTION,
    TOMBSTONED
}
