package com.eventu.dto;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Set;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;

import com.eventu.util.Mensajes;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;

/** Validaciones del registro (HU-01): valores límite de nombre, correo y contraseña */
class RegistroRequestDTOTest {

    private static final String DOMINIO = "@unillanos.edu.co";

    private final Validator validador = Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    void registro_conDatosValidos_noTieneErrores() {
        assertTrue(validador.validate(solicitud("Laura Gómez", "laura.pruebas" + DOMINIO, "Prueba1234")).isEmpty());
    }

    @Test
    void registro_conValoresEnElLimite_noTieneErrores() {
        String correoDeCienCaracteres = "a".repeat(100 - DOMINIO.length()) + DOMINIO;

        assertTrue(validador.validate(solicitud("A".repeat(100), "laura.pruebas" + DOMINIO, "Prueba1234")).isEmpty());
        assertTrue(validador.validate(solicitud("Laura Gómez", correoDeCienCaracteres, "Prueba1234")).isEmpty());
        assertTrue(validador.validate(solicitud("Laura Gómez", "laura.pruebas" + DOMINIO, "Prueba12")).isEmpty());
        assertTrue(validador.validate(solicitud("Laura Gómez", "laura.pruebas" + DOMINIO, "Prueba12" + "x".repeat(64))).isEmpty());
    }

    @Test
    void registro_conNombreDeSoloEspacios_indicaQueElNombreEsObligatorio() {
        assertEquals(Set.of("El nombre es obligatorio."), mensajes(solicitud("   ", "laura.pruebas" + DOMINIO, "Prueba1234")));
    }

    @Test
    void registro_conNombreDe101Caracteres_indicaElMaximo() {
        assertEquals(Set.of("El nombre no puede superar los 100 caracteres."),
                mensajes(solicitud("A".repeat(101), "laura.pruebas" + DOMINIO, "Prueba1234")));
    }

    @Test
    void registro_conCorreoDeOtroDominio_indicaQueDebeSerInstitucional() {
        assertEquals(Set.of(Mensajes.CORREO_INSTITUCIONAL), mensajes(solicitud("Laura Gómez", "usuario@gmail.com", "Prueba1234")));
    }

    @Test
    void registro_conDominioInstitucionalDentroDeOtroDominio_indicaQueDebeSerInstitucional() {
        assertEquals(Set.of(Mensajes.CORREO_INSTITUCIONAL),
                mensajes(solicitud("Laura Gómez", "usuario@unillanos.edu.co.evil.com", "Prueba1234")));
    }

    @Test
    void registro_conCorreoDe101Caracteres_indicaElMaximo() {
        String correo = "a".repeat(101 - DOMINIO.length()) + DOMINIO;

        assertEquals(Set.of("El correo no puede superar los 100 caracteres."), mensajes(solicitud("Laura Gómez", correo, "Prueba1234")));
    }

    @Test
    void registro_conContrasenaDeSieteCaracteres_indicaLaLongitudRequerida() {
        assertEquals(Set.of(Mensajes.LONGITUD_CONTRASENA), mensajes(solicitud("Laura Gómez", "laura.pruebas" + DOMINIO, "Prueba1")));
    }

    @Test
    void registro_conContrasenaDeSetentaYTresCaracteres_indicaLaLongitudRequerida() {
        assertEquals(Set.of(Mensajes.LONGITUD_CONTRASENA),
                mensajes(solicitud("Laura Gómez", "laura.pruebas" + DOMINIO, "Prueba12" + "x".repeat(65))));
    }

    @Test
    void registro_conCamposVacios_indicaCadaCampoObligatorio() {
        Set<String> errores = mensajes(solicitud("", "", ""));

        assertTrue(errores.contains("El nombre es obligatorio."));
        assertTrue(errores.contains("El correo es obligatorio."));
        assertTrue(errores.contains("La contraseña es obligatoria."));
    }

    private RegistroRequestDTO solicitud(String nombre, String correo, String password) {
        RegistroRequestDTO solicitud = new RegistroRequestDTO();
        solicitud.setNombre(nombre);
        solicitud.setCorreo(correo);
        solicitud.setPassword(password);
        return solicitud;
    }

    private Set<String> mensajes(RegistroRequestDTO solicitud) {
        return validador.validate(solicitud).stream().map(ConstraintViolation::getMessage).collect(Collectors.toSet());
    }
}
