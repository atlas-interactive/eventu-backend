package com.eventu.controllers;

import com.eventu.dto.ValidacionQrRequestDTO;
import com.eventu.services.AsistenciaService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/** Registro de asistencia mediante código QR */
@RestController
@RequestMapping("/api/asistencias")
public class AsistenciaController {

    private final AsistenciaService asistenciaService;

    public AsistenciaController(AsistenciaService asistenciaService) {
        this.asistenciaService = asistenciaService;
    }

    /** Valida un código QR y registra la asistencia */
    @PostMapping("/validar-qr")
    public ResponseEntity<Map<String, Object>> registrarAsistencia(@Valid @RequestBody ValidacionQrRequestDTO request) {
        return ResponseEntity.ok(
                asistenciaService.registrarAsistenciaPorQr(request.getCodigoQr(), request.getOrganizadorId()));
    }
}
