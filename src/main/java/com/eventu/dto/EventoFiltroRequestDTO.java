package com.eventu.dto;

import java.time.LocalDate;

public record EventoFiltroRequestDTO(Long categoriaId, LocalDate fechaDesde, LocalDate fechaHasta, String texto) {

    /** Un texto en blanco equivale a no filtrar por texto */
    public EventoFiltroRequestDTO {
        texto = texto == null || texto.isBlank() ? null : texto.trim();
    }

    public boolean estaVacio() {
        return categoriaId == null && fechaDesde == null && fechaHasta == null && texto == null;
    }
}