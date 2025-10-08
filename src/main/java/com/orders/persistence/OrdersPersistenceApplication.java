package com.orders.persistence;

import com.orders.persistence.messaging.EventEnvelope;
import com.orders.persistence.service.PersistenceService;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

import java.util.function.Function;

@SpringBootApplication
public class OrdersPersistenceApplication {

    public static void main(String[] args) {
        SpringApplication.run(OrdersPersistenceApplication.class, args);
    }

    /**
     * 🎯 Consume de "orders.scored" → Persiste en DB → Publica EventEnvelope enriquecido
     */
    @Bean
    public Function<EventEnvelope, EventEnvelope> ingestPostgres(PersistenceService service) {
        return event -> {
            try {
                EventEnvelope enriched = service.persistScored(event);
                System.out.println("✅ Orden persistida y publicada: " + enriched.getPayload().getOrderId());
                return enriched;  // ← EventEnvelope con status agregado en meta

            } catch (Exception e) {
                System.err.println("❌ Error persistiendo orden: " + e.getMessage());
                throw new RuntimeException("Error en pipeline", e);
            }
        };
    }
}