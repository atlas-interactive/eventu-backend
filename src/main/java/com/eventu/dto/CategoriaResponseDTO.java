package com.eventu.dto;

import com.eventu.models.Categoria;

import java.time.LocalDateTime;

/** Vista de una categoría de eventos, con la cantidad de eventos publicados que la usan */
public record CategoriaResponseDTO(Long id, String nombre, Boolean activo, LocalDateTime creadoEn, long eventosActivos) {

    public static CategoriaResponseDTO desde(Categoria categoria, long eventosActivos) {
        return new CategoriaResponseDTO(categoria.getId(), categoria.getNombre(), categoria.getActivo(), categoria.getCreadoEn(), eventosActivos);
    }
}
