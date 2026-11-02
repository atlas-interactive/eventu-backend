package com.eventu.dto;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Set;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;

import com.eventu.util.Mensajes;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;

/** Validaciones de longitud de la contraseña nueva (HU-19 escenario 3.0 y recuperación) y del correo de recuperación */
class ContrasenaDTOTest {

    private final Validator validador = Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    void cambio_nuevaDeSieteCaracteres_indicaLaLongitudRequerida() {
        assertTrue(mensajes(cambio("1234567")).contains(Mensajes.LONGITUD_CONTRASENA));
    }

    @Test
    void cambio_nuevaDeSetentaYTresCaracteres_indicaLaLongitudRequerida() {
        assertTrue(mensajes(cambio("a".repeat(73))).contains(Mensajes.LONGITUD_CONTRASENA));
    }

    @Test
    void cambio_nuevaDeOchoCaracteres_esValida() {
        assertTrue(validador.validate(cambio("12345678")).isEmpty());
    }

    @Test
    void restablecer_nuevaMuyCorta_indicaLaLongitudRequerida() {
        RestablecerContrasenaDTO datos = new RestablecerContrasenaDTO();
        datos.setToken("abc");
        datos.setPassword("corta");

        assertTrue(mensajes(datos).contains(Mensajes.LONGITUD_CONTRASENA));
    }

    @Test
    void recuperacion_correoNoInstitucional_indicaQueDebeSerInstitucional() {
        SolicitudRecuperacionDTO datos = new SolicitudRecuperacionDTO();
        datos.setCorreo("laura@gmail.com");

        assertTrue(mensajes(datos).contains(Mensajes.CORREO_INSTITUCIONAL));
    }

    private CambioContrasenaDTO cambio(String nueva) {
        CambioContrasenaDTO datos = new CambioContrasenaDTO();
        datos.setPasswordActual("clave-actual-123");
        datos.setPasswordNueva(nueva);
        return datos;
    }

    private <T> Set<String> mensajes(T datos) {
        return validador.validate(datos).stream().map(ConstraintViolation::getMessage).collect(Collectors.toSet());
    }
}
