package com.portpoc.recipient.infrastructure.rest;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.Instant;
import java.util.UUID;

public record PortRequestDto(
    @JsonProperty("id") UUID id,
    @JsonProperty("status") String status,
    @JsonProperty("createdAt") Instant createdAt,
    @JsonProperty("completedAt") Instant completedAt
) {
}
