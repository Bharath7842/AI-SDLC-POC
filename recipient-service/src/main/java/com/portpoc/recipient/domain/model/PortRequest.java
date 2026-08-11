package com.portpoc.recipient.domain.model;

import java.time.Instant;
import java.util.UUID;

public final class PortRequest {
    private final UUID id;
    private final PortRequestStatus status;
    private final Instant createdAt;
    private final Instant completedAt;
    private final String customerId;

    private PortRequest(UUID id, PortRequestStatus status, Instant createdAt, Instant completedAt, String customerId) {
        this.id = id;
        this.status = status;
        this.createdAt = createdAt;
        this.completedAt = completedAt;
        this.customerId = customerId;
    }

    public static PortRequest create(String customerId) {
        return new PortRequest(UUID.randomUUID(), PortRequestStatus.INITIATED, Instant.now(), null, customerId);
    }

    public static PortRequest of(UUID id, PortRequestStatus status, Instant createdAt, Instant completedAt, String customerId) {
        return new PortRequest(id, status, createdAt, completedAt, customerId);
    }

    public PortRequest complete() {
        if (this.status != PortRequestStatus.INITIATED) {
            throw new IllegalStateException("Cannot complete a request that is not in INITIATED status. Current status: " + this.status);
        }
        return new PortRequest(this.id, PortRequestStatus.COMPLETED, this.createdAt, Instant.now(), this.customerId);
    }

    public UUID getId() {
        return id;
    }

    public PortRequestStatus getStatus() {
        return status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getCompletedAt() {
        return completedAt;
    }

    public String getCustomerId() {
        return customerId;
    }
}
