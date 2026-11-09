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

/** Campos obligatorios del inicio de sesión (HU-02), la validación del QR (HU-15), la confirmación de correo y la desactivación de cuenta (HU-19) */
class SolicitudesObligatoriasDTOTest {

    private final Validator validador = Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    void login_sinCorreoNiContrasena_indicaAmbosCampos() {
        LoginRequestDTO solicitud = new LoginRequestDTO();
        solicitud.setCorreo(" ");
        solicitud.setPassword("");

        assertEquals(Set.of("El correo es obligatorio.", "La contraseña es obligatoria."), mensajes(solicitud));
    }

    @Test
    void login_conDatos_noTieneErrores() {
        LoginRequestDTO solicitud = new LoginRequestDTO();
        solicitud.setCorreo("estudiante1@unillanos.edu.co");
        solicitud.setPassword("Estudiante1234");

        assertTrue(mensajes(solicitud).isEmpty());
    }

    @Test
    void validacionQr_sinCodigo_indicaQueEsObligatorio() {
        assertEquals(Set.of("El código QR es obligatorio."), mensajes(new ValidacionQrRequestDTO()));
    }

    @Test
    void confirmacionCorreo_sinToken_indicaQueElEnlaceNoEsValido() {
        assertEquals(Set.of("El enlace de confirmación no es válido."), mensajes(new ConfirmacionCorreoDTO()));
    }

    @Test
    void reenvioConfirmacion_conCorreoDeOtroDominio_indicaQueDebeSerInstitucional() {
        ReenvioConfirmacionDTO solicitud = new ReenvioConfirmacionDTO();
        solicitud.setCorreo("usuario@gmail.com");

        assertEquals(Set.of(Mensajes.CORREO_INSTITUCIONAL), mensajes(solicitud));
    }

    @Test
    void desactivacionCuenta_sinContrasena_indicaQueEsObligatoria() {
        assertEquals(Set.of("La contraseña es obligatoria."), mensajes(new DesactivacionCuentaDTO()));
    }

    private Set<String> mensajes(Object solicitud) {
        return validador.validate(solicitud).stream().map(ConstraintViolation::getMessage).collect(Collectors.toSet());
    }
}
