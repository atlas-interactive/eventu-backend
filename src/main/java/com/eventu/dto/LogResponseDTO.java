package com.eventu.dto;

import com.eventu.models.Log;
import com.eventu.models.Usuario;

import java.time.LocalDateTime;

/**
 * Vista de un registro de actividad. Si el registro no tiene usuario asociado (por ejemplo, la solicitud
 * de recuperación con un correo no registrado), usuarioId, usuarioNombre y usuarioCorreo llegan en null
 */
public record LogResponseDTO(
        Long id,
        LocalDateTime fecha,
        Long usuarioId,
        String usuarioNombre,
        String usuarioCorreo,
        String accion,
        String entidad,
        Long entidadId,
        String resultado,
        String motivo) {

    /** Debe llamarse dentro de una transacción o con el usuario ya cargado, porque lee una relación perezosa */
    public static LogResponseDTO desde(Log log) {
        Usuario usuario = log.getUsuario();
        return new LogResponseDTO(
                log.getId(),
                log.getFecha(),
                log.getUsuarioId(),
                usuario == null ? null : usuario.getNombre(),
                usuario == null ? null : usuario.getCorreo(),
                log.getAccion(),
                log.getEntidad(),
                log.getEntidadId(),
                log.getResultado(),
                log.getMotivo());
    }
}
