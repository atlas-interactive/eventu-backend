package com.eventu.controllers;

import com.eventu.dto.InscripcionResponseDTO;
import com.eventu.models.Inscripcion;
import com.eventu.services.InscripcionService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.eventu.security.UsuarioAutenticado;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;

import java.util.List;
import java.util.Map;

/** Inscripciones de los usuarios a los eventos */
@RestController
@RequestMapping("/api/inscripciones")
public class InscripcionController {

    private final InscripcionService inscripcionService;

    public InscripcionController(InscripcionService inscripcionService) {
        this.inscripcionService = inscripcionService;
    }

    @PostMapping
    public ResponseEntity<Map<String, Object>> inscribirAEvento(@RequestParam Long eventoId,
                                                                @AuthenticationPrincipal UsuarioAutenticado actual) {
        Inscripcion inscripcion = inscripcionService.inscribirUsuario(actual.id(), eventoId);
        return ResponseEntity.status(HttpStatus.CREATED).body(Map.<String, Object>of(
                "mensaje", "Inscripción realizada exitosamente.",
                "inscripcionId", inscripcion.getId(),
                "codigoQr", inscripcion.getCodigoQr()));
    }

    /** Solo devuelve las inscripciones del usuario autenticado */
    @GetMapping("/mias")
    public ResponseEntity<List<InscripcionResponseDTO>> listarMisInscripciones(
            @AuthenticationPrincipal UsuarioAutenticado actual) {
        return ResponseEntity.ok(inscripcionService.listarInscripcionesDeUsuario(actual.id()));
    }

    @GetMapping("/{id}/qr")
    public ResponseEntity<Map<String, Object>> obtenerCodigoQr(@PathVariable Long id,
                                                            @AuthenticationPrincipal UsuarioAutenticado actual) {
        return ResponseEntity.ok(inscripcionService.obtenerCodigoQr(id, actual.id()));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, Object>> cancelarInscripcion(@PathVariable Long id,
                                                                    @AuthenticationPrincipal UsuarioAutenticado actual) {
        Inscripcion inscripcion = inscripcionService.cancelarInscripcion(id, actual.id());
        return ResponseEntity.ok(Map.<String, Object>of(
                "mensaje", "Inscripción cancelada exitosamente.", "inscripcionId", inscripcion.getId()));
    }
}
