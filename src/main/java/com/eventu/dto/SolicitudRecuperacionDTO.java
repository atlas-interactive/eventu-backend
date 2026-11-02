package com.eventu.dto;

import com.eventu.util.Mensajes;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** Correo de la cuenta que olvidó su contraseña */
public class SolicitudRecuperacionDTO {

    @NotBlank(message = "El correo es obligatorio.")
    @Size(max = 100, message = "El correo no puede superar los 100 caracteres.")
    @Pattern(regexp = RegistroRequestDTO.REGEX_CORREO_INSTITUCIONAL, message = Mensajes.CORREO_INSTITUCIONAL)
    private String correo;

    public String getCorreo() { return correo; }
    public void setCorreo(String correo) { this.correo = correo; }
}
