package com.eventu.notificaciones;

import java.time.LocalDateTime;

/**
 * Hecho que se publica al pedir un enlace para restablecer la contraseña.
 * Lleva el token en claro porque es el único momento en que existe: en la base de datos solo queda su hash
 */
public record RecuperacionContrasenaSolicitada(String correo, String nombre, String token, LocalDateTime expiraEn) {
}
