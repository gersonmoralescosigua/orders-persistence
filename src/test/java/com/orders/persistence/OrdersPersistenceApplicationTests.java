package com.orders.persistence;

import com.orders.persistence.service.PersistenceService;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import com.orders.persistence.messaging.EventEnvelope;
import com.orders.persistence.model.OrderEntity;
import com.orders.persistence.repo.OrderRepository;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class OrdersPersistenceApplicationTests {

    @Autowired
    private PersistenceService service;

    @Autowired
    private OrderRepository repository;

    @Test
    void testPersistScored_Success() {
      // Arrange
      EventEnvelope envelope = createTestEnvelope("LOW");

      // Act
      OrderEntity saved = service.persistScored(envelope);

      // Assert
      assertThat(saved).isNotNull();
      assertThat(saved.getId()).isNotNull();
      assertThat(saved.getEventId()).isEqualTo(envelope.getId());
      assertThat(saved.getStatus()).isEqualTo("READY");
    }

    @Test
    void testPersistScored_HighRisk() {
      // Arrange
      EventEnvelope envelope = createTestEnvelope("HIGH");

      // Act
      OrderEntity saved = service.persistScored(envelope);

      // Assert
      assertThat(saved.getStatus()).isEqualTo("ON_HOLD");
      assertThat(saved.getRiskLevel()).isEqualTo("HIGH");
    }

    @Test
    void testPersistScored_Idempotencia() {
      // Arrange
      EventEnvelope envelope = createTestEnvelope("LOW");

      // Act - guardar dos veces
      OrderEntity first = service.persistScored(envelope);
      OrderEntity second = service.persistScored(envelope);

      // Assert - debe ser la misma entidad
      assertThat(first.getId()).isEqualTo(second.getId());
      assertThat(repository.count()).isEqualTo(1);
    }

    private EventEnvelope createTestEnvelope(String riskLevel) {
      EventEnvelope envelope = new EventEnvelope();
      envelope.setId(UUID.randomUUID());
      envelope.setVersion("1.0");
      envelope.setType("order.scored");
      envelope.setSource("risk-service");
      envelope.setTs(Instant.now());

      Map<String, Object> meta = new HashMap<>();
      meta.put("riskLevel", riskLevel);
      envelope.setMeta(meta);

      EventEnvelope.Payload payload = new EventEnvelope.Payload();
      payload.setOrderId("ORD-" + System.currentTimeMillis());
      payload.setCustomerId("CUST-001");
      payload.setCurrency("GTQ");
      payload.setAmount(1500.00);
      payload.setCreatedAt(Instant.now());
      envelope.setPayload(payload);

      return envelope;
    }
  }
