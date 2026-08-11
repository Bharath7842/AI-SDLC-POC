package com.portpoc.donor.domain.model;

import java.time.Instant;
import java.util.UUID;

public final class PortRequest {
    private final UUID id;
    private final PortRequestStatus status;
    private final Instant createdAt;

    private PortRequest(UUID id, PortRequestStatus status, Instant createdAt) {
        this.id = id;
        this.status = status;
        this.createdAt = createdAt;
    }

    public static PortRequest of(UUID id, Instant createdAt) {
        return new PortRequest(id, PortRequestStatus.INITIATED, createdAt);
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
}
