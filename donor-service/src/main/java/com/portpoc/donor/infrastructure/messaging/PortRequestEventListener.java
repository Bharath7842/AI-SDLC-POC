package com.portpoc.donor.infrastructure.messaging;

import java.time.Instant;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

@Component
public class PortRequestEventListener {
    private static final Logger logger = LoggerFactory.getLogger(PortRequestEventListener.class);

    private final PortRequestAcceptanceService acceptanceService;

    public PortRequestEventListener(PortRequestAcceptanceService acceptanceService) {
        this.acceptanceService = acceptanceService;
    }

    @RabbitListener(queues = RabbitMqConfig.PORT_REQUEST_INITIATED_QUEUE)
    public void handlePortRequestInitiated(
        PortRequestEventMessage message,
        @Header(value = "X-Correlation-ID", required = false) String correlationId) {

        UUID actualCorrelationId = correlationId != null
            ? UUID.fromString(correlationId)
            : message.correlationId();

        MDC.put("correlationId", actualCorrelationId.toString());

        try {
            logger.info("Received port.request.initiated event for request {} from customer {}",
                message.requestId(), message.customerId());

            acceptanceService.acceptPortRequest(message.requestId(), actualCorrelationId);

            logger.info("Successfully accepted port request {}", message.requestId());
        } catch (Exception e) {
            logger.error("Failed to accept port request {}", message.requestId(), e);
            throw e;
        } finally {
            MDC.remove("correlationId");
        }
    }
}
