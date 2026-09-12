package com.eventu.controllers;

<<<<<<< Updated upstream
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
=======
import com.eventu.models.Inscripcion;
import com.eventu.services.InscripcionService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
>>>>>>> Stashed changes
import java.util.Map;

@RestController
@RequestMapping("/api/inscripciones")
@CrossOrigin(origins = "*")
public class InscripcionController {

<<<<<<< Updated upstream
    @GetMapping("/{id}/qr")
    public ResponseEntity<?> obtenerCodigoQR(@PathVariable Long id) {
        if (id <= 0) {
            return ResponseEntity.badRequest().body(Map.of("error", "ID de inscripcion invalido."));
        }

        // Prueba para movil
        return ResponseEntity.ok(Map.of(
            "inscripcionId", id,
            "codigoQr", "EVENTU-QR-INSCRIPCION-" + id + "-HASH987654",
            "estado", "ACTIVA",
            "evento", "Conferencia de Ingenieria de Software II"
        ));
=======
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
>>>>>>> Stashed changes
    }
}