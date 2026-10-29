package com.eventu.controllers;

import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.eventu.security.UsuarioAutenticado;
import com.eventu.services.ReporteService;

/** Reportes en PDF para organizadores y administradores */
@RestController
@RequestMapping("/api/eventos")
public class ReporteController {

    private final ReporteService reporteService;

    public ReporteController(ReporteService reporteService) {
        this.reporteService = reporteService;
    }

    /** Descarga el PDF con los inscritos y asistentes de un evento propio (o de cualquiera, si es administrador) */
    @GetMapping("/{id}/reporte")
    @PreAuthorize("hasAnyRole('ORGANIZADOR','ADMIN')")
    public ResponseEntity<byte[]> descargarReporteInscritos(@PathVariable Long id,
                                                            @AuthenticationPrincipal UsuarioAutenticado actual) {
        byte[] pdf = reporteService.generarReporteInscritos(id, actual.id());
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment().filename("reporte-evento-" + id + ".pdf").build().toString())
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdf);
    }
}
