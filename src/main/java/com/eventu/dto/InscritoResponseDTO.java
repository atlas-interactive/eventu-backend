package com.eventu.dto;

import com.eventu.models.Inscripcion;

import java.time.LocalDateTime;

/** Participante inscrito en un evento, con su estado de asistencia. Lo consulta el organizador del evento */
public record InscritoResponseDTO(
        Long inscripcionId,
        Long usuarioId,
        String nombre,
        String correo,
        LocalDateTime fechaInscripcion,
        boolean asistio) {

    /** Debe llamarse dentro de una transacción, porque lee relaciones perezosas */
    public static InscritoResponseDTO desde(Inscripcion inscripcion, boolean asistio) {
        return new InscritoResponseDTO(
                inscripcion.getId(),
                inscripcion.getUsuario().getId(),
                inscripcion.getUsuario().getNombre(),
                inscripcion.getUsuario().getCorreo(),
                inscripcion.getFechaInscripcion(),
                asistio);
    }
}
