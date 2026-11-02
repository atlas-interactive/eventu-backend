package com.eventu.dto;

import com.eventu.util.Mensajes;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Token recibido por correo y la contraseña nueva */
public class RestablecerContrasenaDTO {

    @NotBlank(message = "El enlace de recuperación no es válido.")
    private String token;

    @NotBlank(message = "La contraseña es obligatoria.")
    @Size(min = 8, max = 72, message = Mensajes.LONGITUD_CONTRASENA)
    private String password;

    public String getToken() { return token; }
    public void setToken(String token) { this.token = token; }
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
}
