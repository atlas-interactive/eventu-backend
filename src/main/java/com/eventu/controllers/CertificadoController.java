package com.eventu.controllers;

import com.eventu.services.CertificadoService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Descarga de certificados de asistencia */
@RestController
@RequestMapping("/api/certificados")
public class CertificadoController {

    private final CertificadoService certificadoService;

    public CertificadoController(CertificadoService certificadoService) {
        this.certificadoService = certificadoService;
    }

    /** Descarga el certificado en PDF de un usuario para un evento */
    @GetMapping("/evento/{eventoId}/usuario/{usuarioId}")
    public ResponseEntity<byte[]> descargarCertificado(@PathVariable Long eventoId, @PathVariable Long usuarioId) {
        byte[] pdf = certificadoService.generarCertificadoPdf(usuarioId, eventoId);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=certificado.pdf")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdf);
    }
}
