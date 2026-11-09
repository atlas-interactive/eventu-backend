package com.eventu;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

import java.util.TimeZone;

/** Punto de entrada del backend de EventU */
@SpringBootApplication
@EnableAsync
public class EventuBackendApplication {

    /** Las fechas se guardan sin zona y se escriben en hora de Colombia, así que la aplicación compara con esa misma hora */
    static final String ZONA_HORARIA = "America/Bogota";

    public static void main(String[] args) {
        TimeZone.setDefault(TimeZone.getTimeZone(ZONA_HORARIA));
        SpringApplication.run(EventuBackendApplication.class, args);
    }
}
