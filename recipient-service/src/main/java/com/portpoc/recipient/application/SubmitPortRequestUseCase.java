package com.portpoc.recipient.application;

import com.portpoc.recipient.domain.model.PortRequest;
import com.portpoc.recipient.domain.ports.PortRequestRepository;
import com.portpoc.recipient.infrastructure.messaging.PortRequestEventPublisher;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class SubmitPortRequestUseCase {
    private final PortRequestRepository repository;
    private final PortRequestEventPublisher publisher;

    public SubmitPortRequestUseCase(PortRequestRepository repository, PortRequestEventPublisher publisher) {
        this.repository = repository;
        this.publisher = publisher;
    }

    public PortRequest execute(String customerId) {
        PortRequest request = PortRequest.create(customerId);
        repository.save(request);
        UUID correlationId = UUID.randomUUID();
        publisher.publishPortRequestInitiated(request, correlationId);
        return request;
    }
}
