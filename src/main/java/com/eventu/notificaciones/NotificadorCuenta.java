package com.eventu.notificaciones;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.time.format.DateTimeFormatter;
import java.util.Locale;

/**
 * Correos sobre la cuenta: el enlace para confirmar el correo, el enlace para restablecer la contraseña y el aviso de que cambió.
 * Corren después de confirmar la transacción y en otro hilo, así un fallo de SendGrid no revierte el cambio
 * ni demora la respuesta
 */
@Component
public class NotificadorCuenta {

    private static final Logger LOG = LoggerFactory.getLogger(NotificadorCuenta.class);
    private static final DateTimeFormatter FORMATO_FECHA =
            DateTimeFormatter.ofPattern("d 'de' MMMM 'de' yyyy, h:mm a", Locale.forLanguageTag("es-CO"));
    static final String RUTA_RESTABLECER = "/restablecer-contrasena?token=";
    static final String RUTA_CONFIRMAR = "/confirmar-correo?token=";

    private final CorreoService correoService;
    private final String urlWeb;

    /** @param urlWeb dirección de la aplicación web a la que apunta el enlace (propiedad {@code eventu.web.url}) */
    public NotificadorCuenta(CorreoService correoService, @Value("${eventu.web.url:http://localhost:5173}") String urlWeb) {
        this.correoService = correoService;
        this.urlWeb = urlWeb.endsWith("/") ? urlWeb.substring(0, urlWeb.length() - 1) : urlWeb;
    }

    /** Envía el enlace de confirmación de una cuenta nueva; si falla, el usuario puede pedir otro */
    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void alSolicitarConfirmacion(ConfirmacionCorreoSolicitada hecho) {
        try {
            correoService.enviar(hecho.correo(), "Confirma tu correo en EventU", textoConfirmacion(hecho));
        } catch (RuntimeException e) {
            LOG.error("No se pudo enviar el enlace de confirmación a {}", hecho.correo(), e);
        }
    }

    /** Envía el enlace de recuperación; si falla, el usuario puede pedir otro */
    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void alSolicitarRecuperacion(RecuperacionContrasenaSolicitada hecho) {
        try {
            correoService.enviar(hecho.correo(), "Restablece tu contraseña de EventU", textoRecuperacion(hecho));
        } catch (RuntimeException e) {
            LOG.error("No se pudo enviar el enlace de recuperación a {}", hecho.correo(), e);
        }
    }

    /** Avisa del cambio para que el dueño de la cuenta pueda reaccionar si no fue él */
    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void alCambiarContrasena(ContrasenaCambiada hecho) {
        try {
            correoService.enviar(hecho.correo(), "Tu contraseña de EventU cambió", textoCambio(hecho));
        } catch (RuntimeException e) {
            LOG.error("No se pudo avisar el cambio de contraseña a {}", hecho.correo(), e);
        }
    }

    String textoConfirmacion(ConfirmacionCorreoSolicitada hecho) {
        return "Hola, " + hecho.nombre() + ".\n\n"
                + "Gracias por registrarte en EventU. Para activar tu cuenta, confirma tu correo abriendo este enlace:\n\n"
                + urlWeb + RUTA_CONFIRMAR + hecho.token() + "\n\n"
                + "El enlace vence el " + hecho.expiraEn().format(FORMATO_FECHA) + ". "
                + "Si vence, puedes pedir uno nuevo desde la pantalla de inicio de sesión.\n\n"
                + "Si no creaste esta cuenta, ignora este correo.\n\n"
                + "EventU - Universidad de los Llanos";
    }

    String textoRecuperacion(RecuperacionContrasenaSolicitada hecho) {
        return "Hola, " + hecho.nombre() + ".\n\n"
                + "Recibimos una solicitud para restablecer la contraseña de tu cuenta de EventU. "
                + "Para elegir una nueva, abre este enlace:\n\n"
                + urlWeb + RUTA_RESTABLECER + hecho.token() + "\n\n"
                + "El enlace sirve una sola vez y vence el " + hecho.expiraEn().format(FORMATO_FECHA) + ".\n\n"
                + "Si no pediste este cambio, ignora este correo: tu contraseña sigue siendo la misma.\n\n"
                + "EventU - Universidad de los Llanos";
    }

    String textoCambio(ContrasenaCambiada hecho) {
        return "Hola, " + hecho.nombre() + ".\n\n"
                + "La contraseña de tu cuenta de EventU se cambió el " + hecho.fecha().format(FORMATO_FECHA) + ". "
                + "Las sesiones abiertas con la contraseña anterior se cerraron.\n\n"
                + "Si no fuiste tú, restablece tu contraseña desde \"¿Olvidaste tu contraseña?\" en "
                + urlWeb + "/login y avisa al administrador.\n\n"
                + "EventU - Universidad de los Llanos";
    }
}
