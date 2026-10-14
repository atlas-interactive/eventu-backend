package com.eventu.dto;

import com.eventu.models.Usuario;

/** Vista de un usuario sin datos sensibles, nunca incluye la contraseña */
public record UsuarioResponseDTO(Long id, String nombre, String correo, String rol) {

    /** Convierte la entidad a su vista pública */
    public static UsuarioResponseDTO desde(Usuario usuario) {
        return new UsuarioResponseDTO(usuario.getId(), usuario.getNombre(), usuario.getCorreo(), usuario.getRol().name());
    }
}