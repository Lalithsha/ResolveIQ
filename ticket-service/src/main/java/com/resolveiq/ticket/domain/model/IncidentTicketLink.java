package com.resolveiq.ticket.domain.model;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "incident_ticket_links", schema = "ticket_schema")
public class IncidentTicketLink {

    @Id
    private UUID id;

    @Column(name = "incident_id", nullable = false)
    private UUID incidentId;

    @Column(name = "ticket_id", nullable = false)
    private UUID ticketId;

    @Enumerated(EnumType.STRING)
    @Column(name = "link_source", nullable = false, length = 32)
    private LinkSource linkSource;

    @Column(name = "similarity_score", nullable = false)
    private double similarityScore;

    @Column(name = "linked_by", nullable = false)
    private UUID linkedBy;

    @Column(name = "linked_at", nullable = false)
    private Instant linkedAt;

    @Column(name = "unlinked_at")
    private Instant unlinkedAt;

    @Column(name = "unlink_reason", columnDefinition = "TEXT")
    private String unlinkReason;

    public IncidentTicketLink() {}

    public IncidentTicketLink(
        UUID id,
        UUID incidentId,
        UUID ticketId,
        LinkSource linkSource,
        double similarityScore,
        UUID linkedBy
    ) {
        this.id = id != null ? id : UUID.randomUUID();
        this.incidentId = incidentId;
        this.ticketId = ticketId;
        this.linkSource = linkSource != null ? linkSource : LinkSource.AUTOMATIC;
        this.similarityScore = similarityScore;
        this.linkedBy = linkedBy;
        this.linkedAt = Instant.now();
    }

    public void unlink(UUID actorId, String reason) {
        this.unlinkedAt = Instant.now();
        this.unlinkReason = reason;
    }

    public UUID getId() { return id; }
    public UUID getIncidentId() { return incidentId; }
    public UUID getTicketId() { return ticketId; }
    public LinkSource getLinkSource() { return linkSource; }
    public double getSimilarityScore() { return similarityScore; }
    public UUID getLinkedBy() { return linkedBy; }
    public Instant getLinkedAt() { return linkedAt; }
    public Instant getUnlinkedAt() { return unlinkedAt; }
    public String getUnlinkReason() { return unlinkReason; }
}
