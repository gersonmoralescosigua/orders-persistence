package com.orders.persistence.messaging;

import lombok.Data;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@Data
public class EventEnvelope {
    private String version;
    private String type;
    private String source;
    private UUID id;
    private Instant ts;
    private Map<String, Object> meta;
    private Payload payload;

    @Data
    public static class Payload {
        private String orderId;
        private String customerId;
        private String currency;
        private double amount;
        private Instant createdAt;
        // añade campos que necesites (items, address, etc.)
    }

    @Data
    public static class ErrorInfo {
        private String type;
        private String reason;
        private String step;
    }
}
