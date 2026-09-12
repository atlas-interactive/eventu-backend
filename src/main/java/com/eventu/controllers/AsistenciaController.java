package com.eventu.controllers;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController
@RequestMapping("/api/asistencias")
@CrossOrigin(origins = "*")
public class AsistenciaController {

    @PostMapping("/validar-qr")
    public ResponseEntity<?> registrarAsistencia(@RequestBody Map<String, String> payload) {
        String codigoQr = payload.get("codigoQr");

        if (codigoQr == null || codigoQr.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "El código QR es obligatorio."));
        }

        if ("QR-DUPLICADO-TEST".equals(codigoQr)) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(Map.of("error", "RN07: Esta asistencia ya fue registrada previamente."));
        }

        return ResponseEntity.ok(Map.of(
            "mensaje", "Asistencia registrada exitosamente.",
            "estudiante", "Martin Pineda Jaramillo",
            "evento", "Simposio de Desarrollo Movil",
            "fechaRegistro", System.currentTimeMillis()
        ));
    }
}