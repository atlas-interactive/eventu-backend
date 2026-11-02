package com.eventu.dto;

import com.eventu.util.Mensajes;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Cambio de contraseña desde el perfil: exige la actual para confirmar que es el dueño de la cuenta */
public class CambioContrasenaDTO {

    @NotBlank(message = "La contraseña actual es obligatoria.")
    private String passwordActual;

    @NotBlank(message = "La contraseña nueva es obligatoria.")
    @Size(min = 8, max = 72, message = Mensajes.LONGITUD_CONTRASENA)
    private String passwordNueva;

    public String getPasswordActual() { return passwordActual; }
    public void setPasswordActual(String passwordActual) { this.passwordActual = passwordActual; }
    public String getPasswordNueva() { return passwordNueva; }
    public void setPasswordNueva(String passwordNueva) { this.passwordNueva = passwordNueva; }
}
