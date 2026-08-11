package com.portpoc.recipient.infrastructure.persistence.jpa;

import com.portpoc.recipient.domain.model.PortRequest;
import com.portpoc.recipient.domain.model.PortRequestStatus;
import com.portpoc.recipient.domain.ports.PortRequestRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class JpaPortRequestRepository implements PortRequestRepository {
    private final PortRequestCrudRepository crudRepository;

    public JpaPortRequestRepository(PortRequestCrudRepository crudRepository) {
        this.crudRepository = crudRepository;
    }

    @Override
    public PortRequest save(PortRequest portRequest) {
        PortRequestJpaEntity entity = new PortRequestJpaEntity(
            portRequest.getId().toString(),
            portRequest.getStatus().name(),
            portRequest.getCreatedAt(),
            portRequest.getCompletedAt(),
            portRequest.getCustomerId()
        );
        crudRepository.save(entity);
        return portRequest;
    }

    @Override
    public Optional<PortRequest> findById(UUID id) {
        return crudRepository.findById(id.toString())
            .map(this::mapToDomain);
    }

    @Override
    public List<PortRequest> findAll() {
        return ((Iterable<PortRequestJpaEntity>) crudRepository.findAll())
            .spliterator()
            .trySplit() == null
            ? List.of()
            : java.util.stream.StreamSupport.stream(crudRepository.findAll().spliterator(), false)
                .map(this::mapToDomain)
                .toList();
    }

    private PortRequest mapToDomain(PortRequestJpaEntity entity) {
        return PortRequest.of(
            UUID.fromString(entity.getId()),
            PortRequestStatus.valueOf(entity.getStatus()),
            entity.getCreatedAt(),
            entity.getCompletedAt(),
            entity.getCustomerId()
        );
    }
}
