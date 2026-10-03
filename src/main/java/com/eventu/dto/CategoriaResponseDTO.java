package com.eventu.dto;

import com.eventu.models.Categoria;

import java.time.LocalDateTime;

/** Vista de una categoría de eventos */
public record CategoriaResponseDTO(Long id, String nombre, Boolean activo, LocalDateTime creadoEn) {

    public static CategoriaResponseDTO desde(Categoria categoria) {
        return new CategoriaResponseDTO(categoria.getId(), categoria.getNombre(), categoria.getActivo(), categoria.getCreadoEn());
    }
}
