package com.eventu.security;

import com.eventu.models.Rol;

/** Datos del usuario cargados desde la base de datos tras verificar su identidad con el token JWT. */
public record UsuarioAutenticado(Long id, String correo, Rol rol) {
}
