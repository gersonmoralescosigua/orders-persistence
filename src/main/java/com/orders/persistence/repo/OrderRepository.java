package com.orders.persistence.repo;

import com.orders.persistence.model.OrderEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import java.util.UUID;

public interface OrderRepository extends JpaRepository<OrderEntity, Long> {
    Optional<OrderEntity> findByEventId(UUID eventId);
}
