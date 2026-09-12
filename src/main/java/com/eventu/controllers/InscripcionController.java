package com.eventu.controllers;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController
@RequestMapping("/api/inscripciones")
@CrossOrigin(origins = "*")
public class InscripcionController {

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
    }
}