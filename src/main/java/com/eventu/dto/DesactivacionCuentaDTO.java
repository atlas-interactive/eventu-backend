package com.eventu.dto;

import jakarta.validation.constraints.NotBlank;

/** Contraseña con la que el usuario confirma que quiere desactivar su cuenta */
public class DesactivacionCuentaDTO {

    @NotBlank(message = "La contraseña es obligatoria.")
    private String password;

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
}
