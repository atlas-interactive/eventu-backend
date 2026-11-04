package com.eventu.dto;

import java.time.LocalDate;

/**
 * Filtros opcionales de la consulta de logs (HU-20). Un valor en blanco equivale a no filtrar por ese campo;
 * la acción, la entidad y el resultado se comparan en mayúsculas porque así se guardan
 */
public record LogFiltroRequestDTO(String accion, String entidad, String resultado, String usuario,
                                  LocalDate fechaDesde, LocalDate fechaHasta) {

    public LogFiltroRequestDTO {
        accion = enMayusculas(accion);
        entidad = enMayusculas(entidad);
        resultado = enMayusculas(resultado);
        usuario = usuario == null || usuario.isBlank() ? null : usuario.trim();
    }

    private static String enMayusculas(String valor) {
        return valor == null || valor.isBlank() ? null : valor.trim().toUpperCase();
    }
}
