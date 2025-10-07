package com.orders.persistence.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.orders.persistence.messaging.EventEnvelope;
import com.orders.persistence.model.OrderEntity;
import com.orders.persistence.repo.OrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PersistenceService {

    private final OrderRepository repo;
    private final ObjectMapper mapper = new ObjectMapper().findAndRegisterModules();

    /**
     * Persiste el evento scored. Implementa idempotencia por eventId.
     */
    public OrderEntity persistScored(EventEnvelope evt) throws Exception {
        if (evt == null || evt.getId() == null) {
            throw new IllegalArgumentException("Evento o id nulo");
        }
        UUID eventId = evt.getId();

        // 1) Idempotencia: si ya existe, retornamos la entidad existente
        var existing = repo.findByEventId(eventId);
        if (existing.isPresent()) {
            return existing.get();
        }

        // 2) Determinar status a partir de meta.riskLevel
        String riskLevel = "LOW";
        if (evt.getMeta() != null && evt.getMeta().get("riskLevel") != null) {
            riskLevel = String.valueOf(evt.getMeta().get("riskLevel"));
        }
        String status = "READY";
        if ("HIGH".equalsIgnoreCase(riskLevel)) {
            status = "ON_HOLD";
        }

        var p = evt.getPayload();

        OrderEntity entity = OrderEntity.builder()
                .eventId(eventId)
                .orderId(p != null ? p.getOrderId() : null)
                .customerId(p != null ? p.getCustomerId() : null)
                .currency(p != null ? p.getCurrency() : null)
                .amount(p != null ? p.getAmount() : null)
                .riskLevel(riskLevel)
                .status(status)
                .createdAt(p != null && p.getCreatedAt() != null ? p.getCreatedAt() : Instant.now())
                .rawJson(mapper.writeValueAsString(evt))
                .build();

        try {
            return repo.save(entity);
        } catch (DataIntegrityViolationException ex) {
            // posible racing condition: otro proceso guardó el mismo eventId
            return repo.findByEventId(eventId).orElseThrow(() -> ex);
        }
    }
}
