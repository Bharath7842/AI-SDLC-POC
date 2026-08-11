package com.portpoc.donor.infrastructure.messaging;

import java.time.Instant;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

@Service
public class PortRequestAcceptanceService {
    private static final Logger logger = LoggerFactory.getLogger(PortRequestAcceptanceService.class);

    private final RabbitTemplate rabbitTemplate;

    public PortRequestAcceptanceService(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    public void acceptPortRequest(UUID requestId, UUID correlationId) {
        String logCorrelationId = MDC.get("correlationId");
        if (logCorrelationId == null) {
            MDC.put("correlationId", correlationId.toString());
        }

        try {
            PortRequestAcceptedMessage message = new PortRequestAcceptedMessage(
                requestId,
                Instant.now(),
                correlationId
            );

            rabbitTemplate.convertAndSend(
                RabbitMqConfig.PORT_EXCHANGE,
                RabbitMqConfig.PORT_REQUEST_ACCEPTED_ROUTING_KEY,
                message,
                msg -> {
                    msg.getMessageProperties().setHeader("X-Correlation-ID", correlationId.toString());
                    return msg;
                }
            );

            logger.info("Published port.request.accepted event for request {} with correlationId {}",
                requestId, correlationId);
        } catch (Exception e) {
            logger.error("Failed to publish port.request.accepted event for request {}", requestId, e);
            throw e;
        } finally {
            MDC.remove("correlationId");
        }
    }
}
