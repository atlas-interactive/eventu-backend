package com.eventu.security;

import com.eventu.models.Rol;

/** Datos del usuario que hizo la petición, tomados del token JWT ya validado */
public record UsuarioAutenticado(Long id, String correo, Rol rol) {
}
