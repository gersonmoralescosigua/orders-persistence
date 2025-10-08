package com.orders.persistence.mapper;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.orders.persistence.messaging.EventEnvelope;
import com.orders.persistence.model.OrderEntity;

import java.time.Instant;

public final class OrderMapper {
    
    private static final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();
    
    private OrderMapper() {}

    public static OrderEntity toEntity(EventEnvelope envelope, String status) throws JsonProcessingException {
        if (envelope == null || envelope.getId() == null) {
            throw new IllegalArgumentException("Envelope o ID nulo");
        }

        var payload = envelope.getPayload();
        var meta = envelope.getMeta();

        return OrderEntity.builder()
                .eventId(envelope.getId())
                .orderId(payload != null ? payload.getOrderId() : null)
                .customerId(payload != null ? payload.getCustomerId() : null)
                .currency(payload != null ? payload.getCurrency() : null)
                .amount(payload != null ? payload.getAmount() : null)
                .riskLevel(meta != null ? meta.getRiskLevel() : "LOW")
                .status(status)
                .createdAt(payload != null && payload.getCreatedAt() != null 
                    ? payload.getCreatedAt() 
                    : Instant.now())
                .rawJson(objectMapper.writeValueAsString(envelope))
                .build();
    }
}