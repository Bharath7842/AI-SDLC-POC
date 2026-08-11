package com.portpoc.donor.domain.model;

import static org.junit.jupiter.api.Assertions.*;

import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class PortRequestTest {

    @Test
    void testCreatePortRequestFromEvent() {
        UUID id = UUID.randomUUID();
        Instant createdAt = Instant.now();

        PortRequest request = PortRequest.of(id, createdAt);

        assertEquals(id, request.getId());
        assertEquals(PortRequestStatus.INITIATED, request.getStatus());
        assertEquals(createdAt, request.getCreatedAt());
    }

    @Test
    void testPortRequestIsImmutable() {
        UUID id = UUID.randomUUID();
        Instant createdAt = Instant.now();
        PortRequest request = PortRequest.of(id, createdAt);

        assertEquals(id, request.getId());
        assertEquals(createdAt, request.getCreatedAt());
    }
}
