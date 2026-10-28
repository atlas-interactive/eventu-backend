package com.eventu.dto;

import java.util.List;

/**
 * Dashboard de estadísticas. Las dos listas solo se llenan para el administrador;
 * para un organizador van vacías
 */
public record EstadisticasResponseDTO(
        long inscripcionesActivas,
        long asistentes,
        double porcentajeAsistencia,
        List<CategoriaConcurridaDTO> categoriasMasConcurridas,
        List<EventoConMasInscritosDTO> eventosConMasInscritos) {
}
