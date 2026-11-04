package com.eventu.dto;

import jakarta.validation.constraints.NotBlank;

/** Token del enlace de confirmación recibido por correo */
public class ConfirmacionCorreoDTO {

    @NotBlank(message = "El enlace de confirmación no es válido.")
    private String token;

    public String getToken() { return token; }
    public void setToken(String token) { this.token = token; }
}
