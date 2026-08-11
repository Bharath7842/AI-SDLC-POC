package com.portpoc.recipient.infrastructure.persistence.jpa;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "port_requests")
public class PortRequestJpaEntity {
    @Id
    private String id;

    @Column(nullable = false)
    private String status;

    @Column(nullable = false)
    private Instant createdAt;

    @Column(nullable = true)
    private Instant completedAt;

    @Column(nullable = false)
    private String customerId;

    public PortRequestJpaEntity() {
    }

    public PortRequestJpaEntity(String id, String status, Instant createdAt, Instant completedAt, String customerId) {
        this.id = id;
        this.status = status;
        this.createdAt = createdAt;
        this.completedAt = completedAt;
        this.customerId = customerId;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getCompletedAt() {
        return completedAt;
    }

    public void setCompletedAt(Instant completedAt) {
        this.completedAt = completedAt;
    }

    public String getCustomerId() {
        return customerId;
    }

    public void setCustomerId(String customerId) {
        this.customerId = customerId;
    }
}
