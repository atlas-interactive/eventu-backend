package com.eventu.dto;

/** Evento con su número de inscripciones activas */
public record EventoConMasInscritosDTO(Long eventoId, String titulo, Long inscripcionesActivas) {
}
