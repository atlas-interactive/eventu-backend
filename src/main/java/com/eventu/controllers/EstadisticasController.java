package com.eventu.controllers;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.eventu.dto.EstadisticasResponseDTO;
import com.eventu.security.UsuarioAutenticado;
import com.eventu.services.EstadisticasService;

/** Dashboard de estadísticas para organizadores y administradores */
@RestController
@RequestMapping("/api/estadisticas")
public class EstadisticasController {

    private final EstadisticasService estadisticasService;

    public EstadisticasController(EstadisticasService estadisticasService) {
        this.estadisticasService = estadisticasService;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ORGANIZADOR','ADMIN')")
    public ResponseEntity<EstadisticasResponseDTO> consultarEstadisticas(
            @AuthenticationPrincipal UsuarioAutenticado actual) {
        return ResponseEntity.ok(estadisticasService.obtenerEstadisticas(actual.id()));
    }
}
