package com.eventu.dto;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDateTime;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;

/** Validaciones de los DTO de evento (HU-05 crear, HU-06 editar) */
class EventoRequestDTOTest {

    private final Validator validador = Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    void crear_conDatosCompletosYValidos_noTieneErrores() {
        assertTrue(validador.validate(solicitudValida()).isEmpty());
    }

    @Test
    void crear_sinTitulo_indicaQueElTituloEsObligatorio() {
        EventoRequestDTO solicitud = solicitudValida();
        solicitud.setTitulo("  ");

        assertTrue(mensajes(solicitud).contains("El título es obligatorio."));
    }

    @Test
    void crear_sinDescripcion_indicaQueLaDescripcionEsObligatoria() {
        EventoRequestDTO solicitud = solicitudValida();
        solicitud.setDescripcion(null);

        assertTrue(mensajes(solicitud).contains("La descripción es obligatoria."));
    }

    @Test
    void crear_sinUbicacion_indicaQueLaUbicacionEsObligatoria() {
        EventoRequestDTO solicitud = solicitudValida();
        solicitud.setUbicacion("");

        assertTrue(mensajes(solicitud).contains("La ubicación es obligatoria."));
    }

    @Test
    void crear_sinCategoria_indicaQueLaCategoriaEsObligatoria() {
        EventoRequestDTO solicitud = solicitudValida();
        solicitud.setCategoriaId(null);

        assertTrue(mensajes(solicitud).contains("La categoría es obligatoria."));
    }

    @Test
    void crear_conCupoCero_rechazaElCupo() {
        EventoRequestDTO solicitud = solicitudValida();
        solicitud.setCuposMaximos(0);

        assertTrue(mensajes(solicitud).contains("El cupo máximo debe ser mayor a cero."));
    }

    @Test
    void crear_conFechaEnElPasado_indicaQueLaFechaDebeSerFutura() {
        EventoRequestDTO solicitud = solicitudValida();
        solicitud.setFechaInicio(LocalDateTime.now().minusDays(1));

        assertTrue(mensajes(solicitud).contains("La fecha de inicio debe ser futura."));
    }

    @Test
    void editar_conFechaEnElPasado_indicaQueLaFechaDebeSerFutura() {
        EventoActualizacionRequestDTO cambios = new EventoActualizacionRequestDTO();
        cambios.setFechaInicio(LocalDateTime.now().minusDays(1));

        assertTrue(mensajes(cambios).contains("La fecha de inicio debe ser futura."));
    }

    private <T> Set<String> mensajes(T dto) {
        return validador.validate(dto).stream()
                .map(ConstraintViolation::getMessage)
                .collect(Collectors.toSet());
    }

    private static EventoRequestDTO solicitudValida() {
        EventoRequestDTO solicitud = new EventoRequestDTO();
        solicitud.setTitulo("Feria de ciencias");
        solicitud.setDescripcion("Muestra de proyectos");
        solicitud.setFechaInicio(LocalDateTime.now().plusDays(10));
        solicitud.setUbicacion("Auditorio principal");
        solicitud.setCuposMaximos(50);
        solicitud.setCategoriaId(5L);
        return solicitud;
    }
}