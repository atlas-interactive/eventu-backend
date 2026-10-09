package com.eventu.dto;

import com.eventu.models.Usuario;

public record UsuarioResponseDTO(Long id, String nombre, String correo, String rol) {

    public static UsuarioResponseDTO desde(Usuario usuario) {
        return new UsuarioResponseDTO(usuario.getId(), usuario.getNombre(), usuario.getCorreo(), usuario.getRol().name());
    }
}