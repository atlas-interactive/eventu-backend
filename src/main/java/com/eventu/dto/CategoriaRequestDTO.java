package com.eventu.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Datos para crear o editar el nombre de una categoría. Activar y desactivar tienen sus propios endpoints */
public class CategoriaRequestDTO {

    @NotBlank(message = "El nombre de la categoría es obligatorio.")
    @Size(max = 50, message = "El nombre de la categoría no puede superar los 50 caracteres.")
    private String nombre;

    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }
}