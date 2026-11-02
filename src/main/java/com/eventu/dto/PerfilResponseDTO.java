package com.eventu.dto;

import com.eventu.models.Usuario;

import java.time.LocalDateTime;

/** Datos del perfil del usuario autenticado, nunca incluye la contraseña */
public record PerfilResponseDTO(Long id, String nombre, String correo, String rol, String codigoInstitucional,
                                LocalDateTime creadoEn) {

    /** Convierte la entidad a la vista del perfil */
    public static PerfilResponseDTO desde(Usuario usuario) {
        return new PerfilResponseDTO(usuario.getId(), usuario.getNombre(), usuario.getCorreo(), usuario.getRol().name(),
                usuario.getCodigoInstitucional(), usuario.getCreadoEn());
    }
}
