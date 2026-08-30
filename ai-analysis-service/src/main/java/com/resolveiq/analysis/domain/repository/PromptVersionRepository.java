package com.resolveiq.analysis.domain.repository;

import com.resolveiq.analysis.domain.model.PromptVersion;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import java.util.UUID;

public interface PromptVersionRepository extends JpaRepository<PromptVersion, UUID> {
    Optional<PromptVersion> findByNameAndActiveTrue(String name);
}
