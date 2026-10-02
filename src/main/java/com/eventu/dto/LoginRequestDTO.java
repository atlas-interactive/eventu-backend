package com.eventu.dto;

import jakarta.validation.constraints.NotBlank;

/** Credenciales para iniciar sesión*/
public class LoginRequestDTO {

    @NotBlank(message = "El correo es obligatorio.")
    private String correo;

    @NotBlank(message = "La contraseña es obligatoria.")
    private String password;

    public String getCorreo() { return correo; }
    public void setCorreo(String correo) { this.correo = correo; }
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
}
