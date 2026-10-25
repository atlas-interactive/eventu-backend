package com.eventu.notificaciones;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.time.format.DateTimeFormatter;
import java.util.Locale;

/**
 * Avisa por correo a los inscritos cuando se cancela un evento.
 * Corre después de confirmar la transacción y en otro hilo, un fallo del proveedor de correo
 * no revierte la cancelación ni demora la respuesta al organizador
 */
@Component
public class NotificadorCancelacion {

    private static final Logger LOG = LoggerFactory.getLogger(NotificadorCancelacion.class);
    private static final DateTimeFormatter FORMATO_FECHA =
            DateTimeFormatter.ofPattern("d 'de' MMMM 'de' yyyy, h:mm a", Locale.forLanguageTag("es-CO"));

    private final CorreoService correoService;

    public NotificadorCancelacion(CorreoService correoService) {
        this.correoService = correoService;
    }

    /** Envía un correo a cada inscrito; si uno falla, sigue con los demás*/
    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void alCancelarseUnEvento(CancelacionEventoNotificada hecho) {
        String asunto = "Evento cancelado: " + hecho.titulo();
        String texto = construirTexto(hecho);
        for (String correo : hecho.correos()) {
            try {
                correoService.enviar(correo, asunto, texto);
            } catch (RuntimeException e) {
                LOG.error("No se pudo avisar la cancelación del evento {} a {}", hecho.eventoId(), correo, e);
            }
        }
    }

    /** Texto del aviso: qué evento se canceló y qué pasó con la inscripción */
    String construirTexto(CancelacionEventoNotificada hecho) {
        return "Hola,\n\n"
                + "El organizador canceló el evento \"" + hecho.titulo() + "\", programado para el "
                + hecho.fechaInicio().format(FORMATO_FECHA) + " en " + hecho.ubicacion() + ".\n\n"
                + "Tu inscripción quedó cancelada y tu código QR ya no es válido.\n\n"
                + "EventU - Universidad de los Llanos";
    }
}
