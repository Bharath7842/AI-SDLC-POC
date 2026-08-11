package com.portpoc.recipient.application;

import com.portpoc.recipient.domain.model.PortRequest;
import com.portpoc.recipient.domain.ports.PortRequestRepository;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class GetPortRequestStatusUseCase {
    private final PortRequestRepository repository;

    public GetPortRequestStatusUseCase(PortRequestRepository repository) {
        this.repository = repository;
    }

    public Optional<PortRequest> execute(UUID id) {
        return repository.findById(id);
    }
}
