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

    /** Inscribe a un usuario (estudiante u organizador) en un evento*/
    @PostMapping
    public ResponseEntity<Map<String, Object>> inscribirAEvento(@RequestParam Long usuarioId, @RequestParam Long eventoId) {
        Inscripcion inscripcion = inscripcionService.inscribirUsuario(usuarioId, eventoId);
        return ResponseEntity.status(HttpStatus.CREATED).body(Map.<String, Object>of(
                "mensaje", "Inscripción realizada exitosamente.",
                "inscripcionId", inscripcion.getId(),
                "codigoQr", inscripcion.getCodigoQr()));
    }

    /** Lista las inscripciones de un usuario */
    @GetMapping("/usuario/{usuarioId}")
    public ResponseEntity<List<InscripcionResponseDTO>> listarInscripcionesDeUsuario(@PathVariable Long usuarioId) {
        return ResponseEntity.ok(inscripcionService.listarInscripcionesDeUsuario(usuarioId));
    }

    /** Entrega el código QR de una inscripción a su dueño */
    @GetMapping("/{id}/qr")
    public ResponseEntity<Map<String, Object>> obtenerCodigoQr(@PathVariable Long id, @RequestParam Long usuarioId) {
        return ResponseEntity.ok(inscripcionService.obtenerCodigoQr(id, usuarioId));
    }

    /** Cancela una inscripción (eliminación lógica: queda como CANCELADA) */
    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, Object>> cancelarInscripcion(@PathVariable Long id,@RequestParam Long usuarioId) {
        Inscripcion inscripcion = inscripcionService.cancelarInscripcion(id, usuarioId);
        return ResponseEntity.ok(Map.<String, Object>of(
                "mensaje", "Inscripción cancelada exitosamente.",
                "inscripcionId", inscripcion.getId()));
    }
}
