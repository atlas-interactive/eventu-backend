package com.eventu.controllers;

<<<<<<< Updated upstream
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
=======
import com.eventu.models.Asistencia;
import com.eventu.services.AsistenciaService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

>>>>>>> Stashed changes
import java.util.Map;

@RestController
@RequestMapping("/api/asistencias")
@CrossOrigin(origins = "*")
public class AsistenciaController {

<<<<<<< Updated upstream
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
=======
    private final AsistenciaService asistenciaService;

    public AsistenciaController(AsistenciaService asistenciaService) {
        this.asistenciaService = asistenciaService;
    }

    @PostMapping("/validar-qr")
    public ResponseEntity<?> registrarAsistencia(@RequestBody Map<String, String> payload) {
        try {
            String codigoQr = payload.get("codigoQr");

            if (codigoQr == null || codigoQr.isBlank()) {
                return ResponseEntity.badRequest().body(Map.of("error", "El código QR es obligatorio."));
            }

            Asistencia asistencia = asistenciaService.registrarAsistenciaPorQr(codigoQr);

            return ResponseEntity.ok(Map.of(
                "mensaje", "Asistencia registrada exitosamente.",
                "asistenciaId", asistencia.getId(),
                "estudiante", asistencia.getInscripcion().getEstudiante().getNombre(),
                "evento", asistencia.getInscripcion().getEvento().getTitulo(),
                "fechaRegistro", asistencia.getFechaRegistro().toString()
            ));

        } catch (RuntimeException e) {
            HttpStatus status = e.getMessage().contains("RN") ? HttpStatus.CONFLICT : HttpStatus.BAD_REQUEST;
            return ResponseEntity.status(status).body(Map.of("error", e.getMessage()));
        }
>>>>>>> Stashed changes
    }
}