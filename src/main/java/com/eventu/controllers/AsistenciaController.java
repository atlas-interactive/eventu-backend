package com.eventu.controllers;

import com.eventu.dto.ValidacionQrRequestDTO;
import com.eventu.services.AsistenciaService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.eventu.security.UsuarioAutenticado;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;

import java.util.Map;

/** Registro de asistencia mediante código QR */
@RestController
@RequestMapping("/api/asistencias")
public class AsistenciaController {

    private final AsistenciaService asistenciaService;

    public AsistenciaController(AsistenciaService asistenciaService) {
        this.asistenciaService = asistenciaService;
    }

    @PostMapping("/validar-qr")
    @PreAuthorize("hasAnyRole('ORGANIZADOR','ADMIN')")
    public ResponseEntity<Map<String, Object>> registrarAsistencia(@Valid @RequestBody ValidacionQrRequestDTO request,
                                                                @AuthenticationPrincipal UsuarioAutenticado actual) {
        return ResponseEntity.ok(asistenciaService.registrarAsistenciaPorQr(request.getCodigoQr(), actual.id()));
    }
}
