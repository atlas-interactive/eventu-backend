package com.eventu.dto;

import org.springframework.data.domain.Page;

import java.util.List;
import java.util.function.Function;

/**
 * Una página de resultados con los datos que necesita el frontend para paginar.
 * Se usa en lugar de devolver Page directamente, porque su formato JSON no es estable entre versiones de Spring
 *
 * @param pagina número de página, empezando en 0
 */
public record PaginaResponseDTO<T>(List<T> contenido, int pagina, int tamano, long totalElementos, int totalPaginas) {

    /** Convierte cada elemento de la página con la función recibida */
    public static <E, T> PaginaResponseDTO<T> desde(Page<E> pagina, Function<E, T> conversion) {
        return new PaginaResponseDTO<>(
                pagina.getContent().stream().map(conversion).toList(),
                pagina.getNumber(),
                pagina.getSize(),
                pagina.getTotalElements(),
                pagina.getTotalPages());
    }
}
