package com.portpoc.recipient.infrastructure.messaging;

import com.portpoc.recipient.domain.model.PortRequest;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

@Component
public class PortRequestEventPublisher {
    private static final Logger logger = LoggerFactory.getLogger(PortRequestEventPublisher.class);

    private final RabbitTemplate rabbitTemplate;

    public PortRequestEventPublisher(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    public void publishPortRequestInitiated(PortRequest portRequest, UUID correlationId) {
        String logCorrelationId = MDC.get("correlationId");
        if (logCorrelationId == null) {
            MDC.put("correlationId", correlationId.toString());
        }

        try {
            PortRequestEventMessage message = new PortRequestEventMessage(
                portRequest.getId(),
                portRequest.getCustomerId(),
                portRequest.getCreatedAt(),
                correlationId
            );

            rabbitTemplate.convertAndSend(
                RabbitMqConfig.PORT_EXCHANGE,
                RabbitMqConfig.PORT_REQUEST_INITIATED_ROUTING_KEY,
                message,
                msg -> {
                    msg.getMessageProperties().setHeader("X-Correlation-ID", correlationId.toString());
                    return msg;
                }
            );

            logger.info("Published port.request.initiated event for request {} with correlationId {}",
                portRequest.getId(), correlationId);
        } catch (Exception e) {
            logger.error("Failed to publish port.request.initiated event for request {}", portRequest.getId(), e);
        } finally {
            MDC.remove("correlationId");
        }
    }
}
