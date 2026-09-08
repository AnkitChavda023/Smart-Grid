package com.smartgrid.disruptionagent.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "human_review_queue")
public class HumanReviewQueueEntry {

    @Id
    private UUID id;

    @Column(name = "disruption_id", nullable = false)
    private UUID disruptionId;

    @Column(columnDefinition = "text", nullable = false)
    private String reason;

    @Column(nullable = false)
    private boolean resolved;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected HumanReviewQueueEntry() {
    }

    public HumanReviewQueueEntry(UUID id, UUID disruptionId, String reason) {
        this.id = id;
        this.disruptionId = disruptionId;
        this.reason = reason;
        this.resolved = false;
        this.createdAt = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public UUID getDisruptionId() {
        return disruptionId;
    }

    public String getReason() {
        return reason;
    }

    public boolean isResolved() {
        return resolved;
    }

    public void setResolved(boolean resolved) {
        this.resolved = resolved;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
