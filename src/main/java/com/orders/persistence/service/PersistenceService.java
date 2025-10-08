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

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class PersistenceService {

    private final OrderRepository repository;

    /**
     * Persiste el evento scored con idempotencia garantizada
     */
    @Transactional
    public EventEnvelope persistScored(EventEnvelope envelope) {  // ← Cambiar retorno
        UUID eventId = envelope.getId();
        
        // 1) Guardar en DB (idempotencia)
        repository.findByEventId(eventId)
                .orElseGet(() -> saveNewOrder(envelope, eventId));
        
        // 2) Enriquecer el envelope con status
        String status = determineStatus(envelope);
        
        if (envelope.getMeta() == null) {
            envelope.setMeta(EventEnvelope.Meta.builder().build());
        }
        envelope.getMeta().setStatus(status);
        
        log.info("✅ Orden persistida: orderId={}, status={}", 
                envelope.getPayload().getOrderId(), status);
        
        return envelope;
    }

    private OrderEntity saveNewOrder(EventEnvelope envelope, UUID eventId) {
        try {
            // Lógica de negocio: determinar status según riskLevel
            String status = determineStatus(envelope);
            
            OrderEntity entity = OrderMapper.toEntity(envelope, status);
            OrderEntity saved = repository.save(entity);
            
            log.info("✅ Orden persistida: eventId={}, orderId={}, status={}", 
                    eventId, saved.getOrderId(), saved.getStatus());
            return saved;
            
        } catch (DataIntegrityViolationException ex) {
            log.warn("⚠️ Race condition detectada para eventId={}, recuperando existente", eventId);
            return repository.findByEventId(eventId)
                    .orElseThrow(() -> new IllegalStateException("Orden perdida después de race condition", ex));
                    
        } catch (Exception ex) {
            log.error("❌ Error persistiendo orden: eventId={}", eventId, ex);
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