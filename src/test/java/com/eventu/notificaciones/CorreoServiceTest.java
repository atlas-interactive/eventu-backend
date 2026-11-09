package com.eventu.notificaciones;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/** Configuración del envío de correos: sin clave o sin remitente el correo se omite sin fallar */
class CorreoServiceTest {

    @Test
    void estaConfigurado_soloConClaveYRemitente() {
        assertTrue(new CorreoService("clave", "admineventu@gmail.com").estaConfigurado());
        assertFalse(new CorreoService("", "admineventu@gmail.com").estaConfigurado());
        assertFalse(new CorreoService("clave", " ").estaConfigurado());
        assertFalse(new CorreoService(null, null).estaConfigurado());
    }

    @Test
    void enviar_sinConfiguracion_noFallaNiIntentaConectarse() {
        CorreoService servicio = new CorreoService("", "");

        assertDoesNotThrow(() -> servicio.enviar("estudiante1@unillanos.edu.co", "Asunto", "Texto"));
    }
}
