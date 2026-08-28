package com.resolveiq.analysis.domain.model;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "prompt_versions", schema = "analysis_schema")
public class PromptVersion {

    @Id
    private UUID id;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(nullable = false, length = 50)
    private String version;

    @Column(name = "template_body", nullable = false, columnDefinition = "TEXT")
    private String templateBody;

    @Column(nullable = false)
    private boolean active;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    public PromptVersion() {}

    public PromptVersion(String name, String version, String templateBody, boolean active) {
        this.id = UUID.randomUUID();
        this.name = name;
        this.version = version;
        this.templateBody = templateBody;
        this.active = active;
        this.createdAt = Instant.now();
    }

    public UUID getId() { return id; }
    public String getName() { return name; }
    public String getVersion() { return version; }
    public String getTemplateBody() { return templateBody; }
    public boolean isActive() { return active; }
    public Instant getCreatedAt() { return createdAt; }
}
