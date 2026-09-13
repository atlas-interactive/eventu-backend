package com.eventu.controllers;

import com.eventu.services.CertificadoService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/certificados")
@CrossOrigin(origins = "*")
public class CertificadoController {

    private final CertificadoService certificadoService;

    public CertificadoController(CertificadoService certificadoService) {
        this.certificadoService = certificadoService;
    }

    @GetMapping("/evento/{eventoId}/estudiante/{estudianteId}")
    public ResponseEntity<?> descargarCertificado(@PathVariable Long eventoId, @PathVariable Long estudianteId) {
        try {
            byte[] pdf = certificadoService.generarCertificadoPdf(estudianteId, eventoId);
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=certificado.pdf")
                    .contentType(MediaType.APPLICATION_PDF)
                    .body(pdf);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}