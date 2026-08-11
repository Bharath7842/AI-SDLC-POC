package com.portpoc.recipient.domain.model;

import static org.junit.jupiter.api.Assertions.*;

import java.util.UUID;
import org.junit.jupiter.api.Test;

class PortRequestTest {

    @Test
    void testCreateNewPortRequest() {
        String customerId = "customer-123";
        PortRequest request = PortRequest.create(customerId);

        assertNotNull(request.getId());
        assertEquals(PortRequestStatus.INITIATED, request.getStatus());
        assertEquals(customerId, request.getCustomerId());
        assertNotNull(request.getCreatedAt());
        assertNull(request.getCompletedAt());
    }

    @Test
    void testCompleteRequest() {
        PortRequest request = PortRequest.create("customer-123");
        PortRequest completed = request.complete();

        assertEquals(PortRequestStatus.COMPLETED, completed.getStatus());
        assertNotNull(completed.getCompletedAt());
        assertEquals(request.getId(), completed.getId());
        assertEquals(request.getCustomerId(), completed.getCustomerId());
    }

    @Test
    void testCannotCompleteAlreadyCompletedRequest() {
        PortRequest request = PortRequest.create("customer-123").complete();

        assertThrows(IllegalStateException.class, request::complete);
    }

    @Test
    void testPortRequestImmutable() {
        UUID id = UUID.randomUUID();
        PortRequest request = PortRequest.of(id, PortRequestStatus.INITIATED, null, null, "customer-123");

        assertEquals(id, request.getId());
        assertEquals("customer-123", request.getCustomerId());
    }
}
