package com.orders.persistence.mapper;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.orders.persistence.messaging.EventEnvelope;
import com.orders.persistence.model.OrderEntity;

import java.time.Instant;

public final class OrderMapper {
    
    private static final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();
    
    private OrderMapper() {}

    /**
     * Convierte un EventEnvelope a OrderEntity
     */
    public static OrderEntity toEntity(EventEnvelope envelope) throws JsonProcessingException {
        if (envelope == null || envelope.getId() == null) {
            throw new IllegalArgumentException("Envelope o ID nulo");
        }

        var payload = envelope.getPayload();
        String riskLevel = extractRiskLevel(envelope);
        String status = determineStatus(riskLevel);

        return OrderEntity.builder()
                .eventId(envelope.getId())
                .orderId(payload != null ? payload.getOrderId() : null)
                .customerId(payload != null ? payload.getCustomerId() : null)
                .currency(payload != null ? payload.getCurrency() : null)
                .amount(payload != null ? payload.getAmount() : null)
                .riskLevel(riskLevel)
                .status(status)
                .createdAt(payload != null && payload.getCreatedAt() != null 
                    ? payload.getCreatedAt() 
                    : Instant.now())
                .rawJson(objectMapper.writeValueAsString(envelope))
                .build();
    }

    /**
     * Extrae el riskLevel del metadata del envelope
     */
    private static String extractRiskLevel(EventEnvelope envelope) {
        if (envelope.getMeta() != null && envelope.getMeta().get("riskLevel") != null) {
            return String.valueOf(envelope.getMeta().get("riskLevel"));
        }
        return "LOW";
    }

    /**
     * Determina el status basado en el riskLevel
     */
    private static String determineStatus(String riskLevel) {
        return "HIGH".equalsIgnoreCase(riskLevel) ? "ON_HOLD" : "READY";
    }
}