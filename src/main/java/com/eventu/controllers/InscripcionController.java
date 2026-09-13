package com.eventu.controllers;

import com.eventu.models.Inscripcion;
import com.eventu.services.InscripcionService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/inscripciones")
@CrossOrigin(origins = "*")
public class InscripcionController {

    private final InscripcionService inscripcionService;

    public InscripcionController(InscripcionService inscripcionService) {
        this.inscripcionService = inscripcionService;
    }

    @PostMapping
    public ResponseEntity<?> inscribirAEvento(@RequestParam Long estudianteId, @RequestParam Long eventoId) {
        try {
            Inscripcion inscripcion = inscripcionService.inscribirEstudiante(estudianteId, eventoId);
            return ResponseEntity.status(HttpStatus.CREATED).body(Map.of(
                "mensaje", "Inscripcion realizada exitosamente.",
                "inscripcionId", inscripcion.getId(),
                "codigoQr", inscripcion.getCodigoQr()
            ));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/estudiante/{estudianteId}")
    public ResponseEntity<List<Inscripcion>> listarInscripcionesDeEstudiante(@PathVariable Long estudianteId) {
        List<Inscripcion> inscripciones = inscripcionService.listarInscripcionesPorEstudiante(estudianteId);
        return ResponseEntity.ok(inscripciones);
    }

    @GetMapping("/{id}/qr")
    public ResponseEntity<?> obtenerCodigoQR(@PathVariable Long id) {
        try {
            Inscripcion inscripcion = inscripcionService.obtenerInscripcionPorId(id);
            return ResponseEntity.ok(Map.of(
                "inscripcionId", inscripcion.getId(),
                "codigoQr", inscripcion.getCodigoQr(),
                "estado", inscripcion.getEstado().name(),
                "evento", inscripcion.getEvento().getTitulo()
            ));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", e.getMessage()));
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> cancelarInscripcion(@PathVariable Long id, @RequestParam Long estudianteId) {
        try {
            Inscripcion inscripcion = inscripcionService.cancelarInscripcion(id, estudianteId);
            return ResponseEntity.ok(Map.of(
                "mensaje", "Inscripcion cancelada exitosamente.",
                "inscripcionId", inscripcion.getId()
            ));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}