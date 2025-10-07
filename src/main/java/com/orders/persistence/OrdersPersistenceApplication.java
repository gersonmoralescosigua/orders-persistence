

package com.orders.persistence;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class OrdersPersistenceApplication {

    public static void main(String[] args) {
        SpringApplication.run(OrdersPersistenceApplication.class, args);
    }

    @Bean
    public Consumer<Documento> ingestPostgres(DocumentoService service) {
        return doc -> {
            service.save(doc);
            // System.out.println("Guardado en Postgres: " + doc.getUniqueId());
        };
    }


}