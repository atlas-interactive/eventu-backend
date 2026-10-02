package com.eventu.dto;

import com.eventu.models.EstadoInscripcion;
import com.eventu.models.Inscripcion;

import java.time.LocalDateTime;

/** Vista de una inscripción con los datos básicos de su evento */
public record InscripcionResponseDTO(
        Long id,
        String estado,
        String codigoQr,
        LocalDateTime fechaInscripcion,
        Long eventoId,
        String eventoTitulo,
        LocalDateTime eventoFechaInicio,
        String eventoUbicacion) {

    /**
     * Debe llamarse dentro de una transacción, porque lee relaciones perezosas.
     * El código QR solo se incluye mientras la inscripción está activa
     */
    public static InscripcionResponseDTO desde(Inscripcion inscripcion) {
        boolean activa = inscripcion.getEstado() == EstadoInscripcion.ACTIVA;
        return new InscripcionResponseDTO(
                inscripcion.getId(),
                inscripcion.getEstado().name(),
                activa ? inscripcion.getCodigoQr() : null,
                inscripcion.getFechaInscripcion(),
                inscripcion.getEvento().getId(),
                inscripcion.getEvento().getTitulo(),
                inscripcion.getEvento().getFechaInicio(),
                inscripcion.getEvento().getUbicacion());
    }
}
