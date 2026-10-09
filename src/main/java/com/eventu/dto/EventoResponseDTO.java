package com.eventu.dto;

import com.eventu.models.Categoria;
import com.eventu.models.Evento;

import java.time.LocalDateTime;

/** Vista pública de un evento: no incluye datos privados del organizador */
public record EventoResponseDTO(
        Long id,
        String titulo,
        String descripcion,
        LocalDateTime fechaInicio,
        String ubicacion,
        Integer cuposMaximos,
        Integer cuposDisponibles,
        String estado,
        Long organizadorId,
        String organizadorNombre,
        Long categoriaId,
        String categoriaNombre) {

    /** Debe llamarse dentro de una transacción, porque lee relaciones perezosas */
    public static EventoResponseDTO desde(Evento evento) {
        Categoria categoria = evento.getCategoria();
        return new EventoResponseDTO(
                evento.getId(),
                evento.getTitulo(),
                evento.getDescripcion(),
                evento.getFechaInicio(),
                evento.getUbicacion(),
                evento.getCuposMaximos(),
                evento.getCuposDisponibles(),
                evento.getEstado().name(),
                evento.getOrganizador().getId(),
                evento.getOrganizador().getNombre(),
                categoria != null ? categoria.getId() : null,
                categoria != null ? categoria.getNombre() : null);
    }
}
