package com.resolveiq.rag.domain.repository;

import com.resolveiq.rag.domain.model.CitationRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;

public interface CitationRecordRepository extends JpaRepository<CitationRecord, UUID> {
    List<CitationRecord> findBySuggestionId(UUID suggestionId);
}
