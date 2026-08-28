package com.resolveiq.analysis.domain.repository;

import com.resolveiq.analysis.domain.model.PromptVersion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface PromptVersionRepository extends JpaRepository<PromptVersion, UUID> {
    Optional<PromptVersion> findByNameAndActiveTrue(String name);
}
