package com.orders.persistence.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "orders", uniqueConstraints = @UniqueConstraint(columnNames = "event_id"))
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class OrderEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;                // PK interno (autoincrement)

    @Column(name = "event_id", nullable = false, unique = true)
    private UUID eventId;           // id del envelope -> garantiza idempotencia

    @Column(name = "order_id", nullable = false)
    private String orderId;

    @Column(name = "customer_id")
    private String customerId;

    @Column(name = "currency")
    private String currency;

    @Column(name = "amount")
    private Double amount;

    @Column(name = "risk_level")
    private String riskLevel;       // LOW, MEDIUM, HIGH

    @Column(name = "status")
    private String status;          // READY, ON_HOLD

    @Column(name = "created_at")
    private Instant createdAt;

    @Lob
    @Column(name = "raw_json")
    private String rawJson;         // opcional: guarda el payload original
}
