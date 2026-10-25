package com.eventu;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

/** Punto de entrada del backend de EventU */
@SpringBootApplication
@EnableAsync
public class EventuBackendApplication {

    public static void main(String[] args) {
        SpringApplication.run(EventuBackendApplication.class, args);
    }
}
