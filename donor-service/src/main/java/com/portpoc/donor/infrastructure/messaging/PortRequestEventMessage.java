package com.portpoc.donor.infrastructure.messaging;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.Instant;
import java.util.UUID;

public record PortRequestEventMessage(
    @JsonProperty("requestId") UUID requestId,
    @JsonProperty("customerId") String customerId,
    @JsonProperty("createdAt") Instant createdAt,
    @JsonProperty("correlationId") UUID correlationId
) {
}
