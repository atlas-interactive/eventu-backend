package com.eventu.notificaciones;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Hecho que se publica al cancelar un evento
 *Se publica dentro de la transacción y el correo solo se envía si esa transacción se confirma
 */
public record CancelacionEventoNotificada(
        Long eventoId,
        String titulo,
        LocalDateTime fechaInicio,
        String ubicacion,
        List<String> correos) {
}
