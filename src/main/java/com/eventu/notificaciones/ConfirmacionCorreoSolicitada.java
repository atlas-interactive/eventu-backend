package com.eventu.notificaciones;

import java.time.LocalDateTime;

/**
 * Hecho que se publica al registrar una cuenta o al pedir otro enlace de confirmación.
 * Lleva el token en claro porque es el único momento en que existe: en la base de datos solo queda su hash
 */
public record ConfirmacionCorreoSolicitada(String correo, String nombre, String token, LocalDateTime expiraEn) {
}
