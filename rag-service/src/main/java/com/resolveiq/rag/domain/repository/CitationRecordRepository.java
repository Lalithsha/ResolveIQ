package com.resolveiq.rag.domain.repository;

import com.resolveiq.rag.domain.model.CitationRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface CitationRecordRepository extends JpaRepository<CitationRecord, UUID> {
    List<CitationRecord> findBySuggestionId(UUID suggestionId);
}
