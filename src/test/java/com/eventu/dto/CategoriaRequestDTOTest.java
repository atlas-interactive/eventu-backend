package com.eventu.dto;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Set;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;

/** Validaciones del nombre de una categoría (HU-04) */
class CategoriaRequestDTOTest {

    private final Validator validador = Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    void nombre_deCincuentaCaracteres_noTieneErrores() {
        assertTrue(validador.validate(solicitud("T".repeat(50))).isEmpty());
    }

    @Test
    void nombre_deCincuentaYUnCaracteres_indicaElMaximo() {
        assertEquals(Set.of("El nombre de la categoría no puede superar los 50 caracteres."), mensajes(solicitud("T".repeat(51))));
    }

    @Test
    void nombre_deSoloEspacios_indicaQueEsObligatorio() {
        assertEquals(Set.of("El nombre de la categoría es obligatorio."), mensajes(solicitud("   ")));
    }

    private CategoriaRequestDTO solicitud(String nombre) {
        CategoriaRequestDTO solicitud = new CategoriaRequestDTO();
        solicitud.setNombre(nombre);
        return solicitud;
    }

    private Set<String> mensajes(CategoriaRequestDTO solicitud) {
        return validador.validate(solicitud).stream().map(ConstraintViolation::getMessage).collect(Collectors.toSet());
    }
}
