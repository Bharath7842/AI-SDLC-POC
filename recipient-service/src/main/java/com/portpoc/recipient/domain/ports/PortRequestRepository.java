package com.portpoc.recipient.domain.ports;

import com.portpoc.recipient.domain.model.PortRequest;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PortRequestRepository {
    PortRequest save(PortRequest portRequest);

    Optional<PortRequest> findById(UUID id);

    List<PortRequest> findAll();
}
