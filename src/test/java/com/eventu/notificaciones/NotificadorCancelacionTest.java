package com.eventu.notificaciones;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.client.RestClientException;

/** Pruebas del aviso por correo al cancelar un evento (HU-07 escenario 4.0) */
@ExtendWith(MockitoExtension.class)
class NotificadorCancelacionTest {

    @Mock private CorreoService correoService;

    private NotificadorCancelacion notificador;

    @BeforeEach
    void preparar() {
        notificador = new NotificadorCancelacion(correoService);
    }

    @Test
    void alCancelarse_enviaUnCorreoAcadaInscritoConElTituloDelEventoEnElAsunto() {
        notificador.alCancelarseUnEvento(hecho(List.of("ana@unillanos.edu.co", "luis@unillanos.edu.co")));

        verify(correoService).enviar(eq("ana@unillanos.edu.co"), eq("Evento cancelado: Feria de ciencias"), anyString());
        verify(correoService).enviar(eq("luis@unillanos.edu.co"), eq("Evento cancelado: Feria de ciencias"), anyString());
    }

    @Test
    void alCancelarse_siUnCorreoFalla_sigueConLosDemas() {
        doThrow(new RestClientException("SendGrid no responde"))
                .when(correoService).enviar(eq("ana@unillanos.edu.co"), anyString(), anyString());

        notificador.alCancelarseUnEvento(hecho(List.of("ana@unillanos.edu.co", "luis@unillanos.edu.co")));

        verify(correoService).enviar(eq("luis@unillanos.edu.co"), anyString(), anyString());
    }

    @Test
    void alCancelarse_sinCorreos_noEnviaNada() {
        notificador.alCancelarseUnEvento(hecho(List.of()));

        verify(correoService, never()).enviar(anyString(), anyString(), anyString());
    }

    @Test
    void textoDelAviso_mencionaElEventoElLugarYQueLaInscripcionQuedoCancelada() {
        String texto = notificador.construirTexto(hecho(List.of("ana@unillanos.edu.co")));

        assertTrue(texto.contains("Feria de ciencias"));
        assertTrue(texto.contains("Auditorio principal"));
        assertTrue(texto.contains("inscripción quedó cancelada"));
    }

    private static CancelacionEventoNotificada hecho(List<String> correos) {
        return new CancelacionEventoNotificada(100L, "Feria de ciencias",
                LocalDateTime.of(2026, 11, 20, 15, 30), "Auditorio principal", correos);
    }
}
