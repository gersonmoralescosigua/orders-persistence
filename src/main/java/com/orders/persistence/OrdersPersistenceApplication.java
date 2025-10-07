package com.orders.persistence;

import com.orders.persistence.messaging.EventEnvelope;
import com.orders.persistence.service.PersistenceService;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

import java.util.function.Consumer;

@SpringBootApplication
public class OrdersPersistenceApplication {

    public static void main(String[] args) {
        SpringApplication.run(OrdersPersistenceApplication.class, args);
    }

    @Bean
    public Consumer<EventEnvelope> ingestPostgres(PersistenceService service) {
        return event -> {
            try {
                service.persistScored(event);
                System.out.println("✅ Orden persistida: " + event.getId());
            } catch (Exception e) {
                System.err.println("❌ Error persistiendo orden: " + e.getMessage());
                // Aquí podrías implementar retry o DLQ según tus necesidades
            }
        };
    }
}