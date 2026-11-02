package com.eventu.notificaciones;

import java.time.LocalDateTime;

/** Hecho que se publica cuando cambia la contraseña de una cuenta, por el perfil o por un enlace de recuperación */
public record ContrasenaCambiada(String correo, String nombre, LocalDateTime fecha) {
}
