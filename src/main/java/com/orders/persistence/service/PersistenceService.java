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
    public OrderEntity persistScored(EventEnvelope envelope) {
        UUID eventId = envelope.getId();
        
        // 1) Idempotencia: verificar si ya existe
        return repository.findByEventId(eventId)
                .orElseGet(() -> saveNewOrder(envelope, eventId));
    }

    private OrderEntity saveNewOrder(EventEnvelope envelope, UUID eventId) {
        try {
            OrderEntity entity = OrderMapper.toEntity(envelope);
            OrderEntity saved = repository.save(entity);
            log.info("✅ Orden persistida: eventId={}, orderId={}, status={}", 
                    eventId, saved.getOrderId(), saved.getStatus());
            return saved;
            
        } catch (DataIntegrityViolationException ex) {
            // Race condition: otro proceso guardó el mismo eventId
            log.warn("⚠️ Race condition detectada para eventId={}, recuperando existente", eventId);
            return repository.findByEventId(eventId)
                    .orElseThrow(() -> new IllegalStateException("Orden perdida después de race condition", ex));
                    
        } catch (Exception ex) {
            log.error("❌ Error persistiendo orden: eventId={}", eventId, ex);
            throw new RuntimeException("Error al persistir orden", ex);
        }
    }
}