package com.eventu.dto;

import java.util.List;

/** Valores disponibles para los filtros de la pantalla de logs (HU-20) */
public record OpcionesFiltroLogDTO(List<String> acciones, List<String> entidades, List<String> resultados) {
}
