package com.portpoc.recipient.application;

import com.portpoc.recipient.domain.model.PortRequest;
import com.portpoc.recipient.domain.ports.PortRequestRepository;
import com.portpoc.recipient.domain.ports.ReceiptStorage;
import java.util.Optional;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class CompletePortRequestUseCase {
    private static final Logger logger = LoggerFactory.getLogger(CompletePortRequestUseCase.class);

    private final PortRequestRepository repository;
    private final ReceiptStorage receiptStorage;

    public CompletePortRequestUseCase(PortRequestRepository repository, ReceiptStorage receiptStorage) {
        this.repository = repository;
        this.receiptStorage = receiptStorage;
    }

    public Optional<PortRequest> execute(UUID id, String receiptUri) {
        Optional<PortRequest> request = repository.findById(id);

        if (request.isEmpty()) {
            return Optional.empty();
        }

        PortRequest current = request.get();
        if (current.getStatus().name().equals("COMPLETED")) {
            throw new IllegalStateException("Request already completed");
        }

        PortRequest completed = current.complete();
        repository.save(completed);
        logger.info("Port request {} completed with receipt {}", id, receiptUri);
        return Optional.of(completed);
    }
}
