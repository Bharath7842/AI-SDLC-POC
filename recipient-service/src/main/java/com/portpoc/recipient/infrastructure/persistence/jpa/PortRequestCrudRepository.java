package com.portpoc.recipient.infrastructure.persistence.jpa;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PortRequestCrudRepository extends CrudRepository<PortRequestJpaEntity, String> {
}
