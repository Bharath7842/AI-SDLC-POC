package com.portpoc.recipient.infrastructure.rest;

import com.portpoc.recipient.application.CompletePortRequestUseCase;
import com.portpoc.recipient.application.GetPortRequestStatusUseCase;
import com.portpoc.recipient.application.SubmitPortRequestUseCase;
import com.portpoc.recipient.domain.model.PortRequest;
import java.util.Optional;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/port-requests")
public class PortRequestController {
    private static final Logger logger = LoggerFactory.getLogger(PortRequestController.class);

    private final SubmitPortRequestUseCase submitUseCase;
    private final GetPortRequestStatusUseCase getStatusUseCase;
    private final CompletePortRequestUseCase completeUseCase;

    public PortRequestController(
        SubmitPortRequestUseCase submitUseCase,
        GetPortRequestStatusUseCase getStatusUseCase,
        CompletePortRequestUseCase completeUseCase) {
        this.submitUseCase = submitUseCase;
        this.getStatusUseCase = getStatusUseCase;
        this.completeUseCase = completeUseCase;
    }

    @PostMapping
    public ResponseEntity<PortRequestDto> submitPortRequest(@RequestBody SubmitRequestDto request) {
        String correlationId = MDC.get("correlationId");
        if (correlationId == null) {
            correlationId = UUID.randomUUID().toString();
            MDC.put("correlationId", correlationId);
        }

        try {
            if (request.customerId() == null || request.customerId().isBlank()) {
                return ResponseEntity.badRequest().build();
            }

            PortRequest portRequest = submitUseCase.execute(request.customerId());
            logger.info("Submitted port request {} for customer {}", portRequest.getId(), request.customerId());

            PortRequestDto dto = new PortRequestDto(
                portRequest.getId(),
                portRequest.getStatus().name(),
                portRequest.getCreatedAt(),
                portRequest.getCompletedAt()
            );

            return ResponseEntity.status(HttpStatus.CREATED).body(dto);
        } finally {
            MDC.remove("correlationId");
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<PortRequestDto> getPortRequestStatus(@PathVariable UUID id) {
        Optional<PortRequest> request = getStatusUseCase.execute(id);

        if (request.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        PortRequest portRequest = request.get();
        PortRequestDto dto = new PortRequestDto(
            portRequest.getId(),
            portRequest.getStatus().name(),
            portRequest.getCreatedAt(),
            portRequest.getCompletedAt()
        );

        return ResponseEntity.ok(dto);
    }

    @PostMapping("/{id}/complete")
    public ResponseEntity<PortRequestDto> completePortRequest(
        @PathVariable UUID id,
        @RequestBody CompleteRequestDto request) {

        try {
            Optional<PortRequest> completed = completeUseCase.execute(id, request.receiptUri());

            if (completed.isEmpty()) {
                return ResponseEntity.notFound().build();
            }

            PortRequest portRequest = completed.get();
            PortRequestDto dto = new PortRequestDto(
                portRequest.getId(),
                portRequest.getStatus().name(),
                portRequest.getCreatedAt(),
                portRequest.getCompletedAt()
            );

            logger.info("Completed port request {}", id);
            return ResponseEntity.ok(dto);
        } catch (IllegalStateException e) {
            logger.warn("Cannot complete request {}: {}", id, e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }

    public record SubmitRequestDto(String customerId) {}
    public record CompleteRequestDto(String receiptUri) {}
}
