package com.eventu.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/** Datos para validar un código QR y registrar la asistencia */
public class ValidacionQrRequestDTO {

    @NotBlank(message = "El código QR es obligatorio.")
    private String codigoQr;

    @NotNull(message = "El organizador es obligatorio.")
    private Long organizadorId;

    public String getCodigoQr() { return codigoQr; }
    public void setCodigoQr(String codigoQr) { this.codigoQr = codigoQr; }
    public Long getOrganizadorId() { return organizadorId; }
    public void setOrganizadorId(Long organizadorId) { this.organizadorId = organizadorId; }
}
