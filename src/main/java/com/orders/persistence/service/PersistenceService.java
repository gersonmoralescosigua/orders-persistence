package com.orders.persistence.service;

import com.orders.persistence.mapper.OrderMapper;
import com.orders.persistence.messaging.EventEnvelope;
import com.orders.persistence.model.OrderEntity;
import com.orders.persistence.repo.OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class PersistenceService {

    private final OrderRepository repository;

    /**
     * Persiste el evento scored con idempotencia garantizada.
     * Retorna el envelope SOLO si se inserta en la DB (evento nuevo).
     * Retorna null si ya existía o si hubo race condition.
     */
    @Transactional
    public EventEnvelope persistScored(EventEnvelope envelope) {
        UUID eventId = envelope.getId();
        String status = determineStatus(envelope);

        // Si ya existe -> no publicar
        if (repository.findByEventId(eventId).isPresent()) {
            log.info("⚠️ Evento duplicado detectado - NO se publica: eventId={}", eventId);
            return null;
        }

        // Intentar insertar; si hay DataIntegrityViolation -> race condition -> no publicar
        OrderEntity saved;
        try {
            saved = saveNewOrder(envelope, eventId, status);
        } catch (DataIntegrityViolationException ex) {
            log.warn("⚠️ Race condition al insertar eventId={} - NO se publica", eventId);
            return null;
        }

        if (saved == null) {
            return null;
        }

        // Enriquecer el envelope y transformarlo para publicar
        if (envelope.getMeta() == null) {
            envelope.setMeta(EventEnvelope.Meta.builder().build());
        }
        envelope.getMeta().setStatus(status);
        envelope.getMeta().setWarehouseHint("WAREHOUSE-GT");

        envelope.setType("ORDER.READY_TO_SHIP");
        envelope.setSource("orders.persistence");
        envelope.setId(UUID.randomUUID());
        envelope.setTs(Instant.now());

        log.info("✅ Orden persistida y será publicada: orderId={}, status={}, newEventId={}",
                saved.getOrderId(), status, envelope.getId());

        return envelope;
    }

    private OrderEntity saveNewOrder(EventEnvelope envelope, UUID eventId, String status) {
        try {
            OrderEntity entity = OrderMapper.toEntity(envelope, status);
            OrderEntity saved = repository.save(entity);

            log.info("💾 Orden guardada en DB: eventId={}, orderId={}, status={}",
                    eventId, saved.getOrderId(), saved.getStatus());
            return saved;

        } catch (DataIntegrityViolationException ex) {
            // Propagar para que el caller trate como race condition (NO publicar)
            throw ex;

        } catch (Exception ex) {
            log.error("❌ Error al guardar en la base de datos orden: eventId={}", eventId, ex);
            throw new RuntimeException("Error al persistir orden", ex);
        }
    }

    private String determineStatus(EventEnvelope envelope) {
        String riskLevel = envelope.getMeta() != null && envelope.getMeta().getRiskLevel() != null
                ? envelope.getMeta().getRiskLevel()
                : "LOW";

        return "HIGH".equalsIgnoreCase(riskLevel) ? "ON_HOLD" : "READY";
    }
}