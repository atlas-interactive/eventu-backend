package com.eventu.dto;

import static org.junit.jupiter.api.Assertions.assertEquals;
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

    @Test
    void crear_conValoresEnElLimite_aceptaLosValidosYRechazaLosQueSeExceden() {
        EventoRequestDTO tituloDe150 = solicitudValida();
        tituloDe150.setTitulo("T".repeat(150));
        EventoRequestDTO ubicacionDe150 = solicitudValida();
        ubicacionDe150.setUbicacion("U".repeat(150));
        EventoRequestDTO cupoDeUno = solicitudValida();
        cupoDeUno.setCuposMaximos(1);
        EventoRequestDTO tituloDe151 = solicitudValida();
        tituloDe151.setTitulo("T".repeat(151));
        EventoRequestDTO ubicacionDe151 = solicitudValida();
        ubicacionDe151.setUbicacion("U".repeat(151));

        assertTrue(mensajes(tituloDe150).isEmpty());
        assertTrue(mensajes(ubicacionDe150).isEmpty());
        assertTrue(mensajes(cupoDeUno).isEmpty());
        assertEquals(Set.of("El título no puede superar los 150 caracteres."), mensajes(tituloDe151));
        assertEquals(Set.of("La ubicación no puede superar los 150 caracteres."), mensajes(ubicacionDe151));
    }

    @Test
    void editar_conCupoCeroOTituloDeSoloEspacios_indicaUnErrorPorCaso() {
        EventoActualizacionRequestDTO cupoCero = new EventoActualizacionRequestDTO();
        cupoCero.setCuposMaximos(0);
        EventoActualizacionRequestDTO tituloVacio = new EventoActualizacionRequestDTO();
        tituloVacio.setTitulo("   ");

        assertEquals(Set.of("El cupo máximo debe ser mayor a cero."), mensajes(cupoCero));
        assertEquals(Set.of("El título no puede estar vacío."), mensajes(tituloVacio));
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