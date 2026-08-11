package com.portpoc.donor.infrastructure.messaging;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.Instant;
import java.util.UUID;

public record PortRequestAcceptedMessage(
    @JsonProperty("requestId") UUID requestId,
    @JsonProperty("acceptedAt") Instant acceptedAt,
    @JsonProperty("correlationId") UUID correlationId
) {
}
