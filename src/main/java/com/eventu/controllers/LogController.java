package com.eventu.controllers;

import com.eventu.dto.LogFiltroRequestDTO;
import com.eventu.dto.LogResponseDTO;
import com.eventu.dto.OpcionesFiltroLogDTO;
import com.eventu.dto.PaginaResponseDTO;
import com.eventu.services.LogConsultaService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

/**
 * Registro de actividad de la plataforma (HU-20). Solo para administradores y solo de lectura:
 * no hay endpoints para editar ni eliminar registros, así que esos métodos responden 405
 */
@RestController
@RequestMapping("/api/logs")
@PreAuthorize("hasRole('ADMIN')")
public class LogController {

    private final LogConsultaService logConsultaService;

    public LogController(LogConsultaService logConsultaService) {
        this.logConsultaService = logConsultaService;
    }

    /**
     * Lista los logs del más reciente al más antiguo, paginados y con filtros opcionales
     *
     * @param usuario texto que se busca en el nombre o el correo del usuario
     * @param fechaDesde primer día incluido (AAAA-MM-DD)
     * @param fechaHasta último día incluido (AAAA-MM-DD)
     * @return 200 con la página (contenido vacío si no hay registros); 400 si un filtro o la paginación no son válidos
     */
    @GetMapping
    public ResponseEntity<PaginaResponseDTO<LogResponseDTO>> consultarLogs(
            @RequestParam(required = false) String accion,
            @RequestParam(required = false) String entidad,
            @RequestParam(required = false) String resultado,
            @RequestParam(required = false) String usuario,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaDesde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaHasta,
            @RequestParam(defaultValue = "0") int pagina,
            @RequestParam(defaultValue = "20") int tamano) {
        LogFiltroRequestDTO filtro = new LogFiltroRequestDTO(accion, entidad, resultado, usuario, fechaDesde, fechaHasta);
        return ResponseEntity.ok(logConsultaService.consultarLogs(filtro, pagina, tamano));
    }

    /** Devuelve las acciones, entidades y resultados disponibles para los filtros */
    @GetMapping("/opciones")
    public ResponseEntity<OpcionesFiltroLogDTO> obtenerOpcionesDeFiltro() {
        return ResponseEntity.ok(logConsultaService.obtenerOpcionesDeFiltro());
    }
}
