package com.orders.persistence.messaging;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Data;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Event Envelope V1 - Schema completo para eventos de órdenes
 * Tipos: ORDER.CREATED | ORDER.NORMALIZED | ORDER.SCORED | ORDER.READY_TO_SHIP | ORDER.ERROR
 */
@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class EventEnvelope {
private String version;  // "1.0"
  private String type;
  private String source;
  private UUID id;
  private Instant ts;
  private PersistedPayload payload;
  private Meta meta;  // Opcional
  private ErrorInfo error;  // Opcional - solo para ORDER.ERROR

  @Data
  @Builder
  public static class PersistedPayload {
    private String orderId;
    private String customerId;
    private String currency;  // Pattern: ^[A-Z]{3}$
    private Double amount;
    private List<Item> items;
    private String paymentMethod;  // CARD, PAYPAL, CASH_ON_DELIVERY, BANK_TRANSFER
    private String country;  // Pattern: ^[A-Z]{2}$
    private Instant createdAt;
    private ShippingAddress shippingAddress;
  }

  @Data
  @Builder
  public static class Item {
    private String sku;
    private Integer qty;  // minimum: 1
    private Double price;  // minimum: 0
    private String category;  // Opcional
  }

  @Data
  @Builder
  public static class ShippingAddress {
    private String line1;
    private String city;
    private String postalCode;
    private String country;  // Pattern: ^[A-Z]{2}$ - ⚠️ FALTABA en tu JSON de ejemplo
  }

  @Data
  @Builder
  @JsonInclude(JsonInclude.Include.NON_NULL)
  public static class Meta {
    // Para ORDER.CREATED
    private Boolean initialValidation;
    
    // Para ORDER.NORMALIZED
    private Instant normalizedAt;
    
    // Para ORDER.SCORED
    private Double riskScore;  // 0-100
    private String riskLevel;  // LOW, MEDIUM, HIGH
    private Instant fraudCheckedAt;
    
    // Para ORDER.READY_TO_SHIP
    private String status;  // READY, ON_HOLD
    private String warehouseHint;
  }

  @Data
  @Builder
  public static class ErrorInfo {
    private String type;  // VALIDATION_ERROR, INCOMPLETE_DATA, PERSISTENCE_ERROR, UNKNOWN
    private String reason;
    private String step;  // NORMALIZER, FRAUD, PERSISTENCE
    private UUID originalId;  // Opcional
  }

  public String getVersion() {
    return version;
  }

  public void setVersion(String version) {
    this.version = version;
  }

  public String getType() {
    return type;
  }

  public void setType(String type) {
    this.type = type;
  }

  public String getSource() {
    return source;
  }

  public void setSource(String source) {
    this.source = source;
  }

  public UUID getId() {
    return id;
  }

  public void setId(UUID id) {
    this.id = id;
  }

  public Instant getTs() {
    return ts;
  }

  public void setTs(Instant ts) {
    this.ts = ts;
  }

  public PersistedPayload getPayload() {
    return payload;
  }

  public void setPayload(PersistedPayload payload) {
    this.payload = payload;
  }

  public Meta getMeta() {
    return meta;
  }

  public void setMeta(Meta meta) {
    this.meta = meta;
  }

  public ErrorInfo getError() {
    return error;
  }

  public void setError(ErrorInfo error) {
    this.error = error;
  }
}
