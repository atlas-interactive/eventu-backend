package com.eventu.notificaciones;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

import java.time.LocalDateTime;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.client.RestClientException;

/** Pruebas de los correos de recuperación y de aviso de cambio de contraseña */
@ExtendWith(MockitoExtension.class)
class NotificadorCuentaTest {

    private static final String CORREO = "laura@unillanos.edu.co";

    @Mock private CorreoService correoService;

    private NotificadorCuenta notificador;

    @BeforeEach
    void preparar() {
        notificador = new NotificadorCuenta(correoService, "https://eventu.vercel.app/");
    }

    @Test
    void alSolicitarRecuperacion_enviaElEnlaceAlCorreoDeLaCuenta() {
        notificador.alSolicitarRecuperacion(solicitud());

        verify(correoService).enviar(eq(CORREO), eq("Restablece tu contraseña de EventU"), anyString());
    }

    @Test
    void textoRecuperacion_traeElEnlaceConElTokenSinBarraDobleYLaFechaDeVencimiento() {
        String texto = notificador.textoRecuperacion(solicitud());

        assertTrue(texto.contains("https://eventu.vercel.app/restablecer-contrasena?token=abc123"));
        assertFalse(texto.contains(".app//"));
        assertTrue(texto.contains("2 de noviembre de 2026"));
        assertTrue(texto.contains("Si no pediste este cambio"));
    }

    @Test
    void alSolicitarRecuperacion_siSendGridFalla_noPropagaElError() {
        doThrow(new RestClientException("SendGrid no responde")).when(correoService).enviar(anyString(), anyString(), anyString());

        notificador.alSolicitarRecuperacion(solicitud());
    }

    @Test
    void alCambiarContrasena_avisaAlDuenoDeLaCuenta() {
        notificador.alCambiarContrasena(new ContrasenaCambiada(CORREO, "Laura Gomez", LocalDateTime.of(2026, 11, 2, 14, 5)));

        verify(correoService).enviar(eq(CORREO), eq("Tu contraseña de EventU cambió"), anyString());
    }

    @Test
    void textoCambio_explicaQueHacerSiNoFueElUsuario() {
        String texto = notificador.textoCambio(new ContrasenaCambiada(CORREO, "Laura Gomez", LocalDateTime.of(2026, 11, 2, 14, 5)));

        assertTrue(texto.contains("Si no fuiste tú"));
        assertTrue(texto.contains("https://eventu.vercel.app/login"));
    }

    @Test
    void textoConfirmacion_traeElEnlaceConElTokenYLaFechaDeVencimiento() {
        String texto = notificador.textoConfirmacion(
                new ConfirmacionCorreoSolicitada(CORREO, "Laura Gomez", "xyz789", LocalDateTime.of(2026, 11, 5, 9, 0)));

        assertTrue(texto.contains("https://eventu.vercel.app/confirmar-correo?token=xyz789"));
        assertFalse(texto.contains(".app//"));
        assertTrue(texto.contains("5 de noviembre de 2026"));
    }

    @Test
    void alSolicitarConfirmacion_enviaElEnlaceYSiSendGridFallaNoPropagaElError() {
        doThrow(new RestClientException("SendGrid no responde")).when(correoService).enviar(anyString(), anyString(), anyString());

        notificador.alSolicitarConfirmacion(
                new ConfirmacionCorreoSolicitada(CORREO, "Laura Gomez", "xyz789", LocalDateTime.of(2026, 11, 5, 9, 0)));

        verify(correoService).enviar(eq(CORREO), eq("Confirma tu correo en EventU"), anyString());
    }

    private RecuperacionContrasenaSolicitada solicitud() {
        return new RecuperacionContrasenaSolicitada(CORREO, "Laura Gomez", "abc123", LocalDateTime.of(2026, 11, 2, 14, 35));
    }
}
