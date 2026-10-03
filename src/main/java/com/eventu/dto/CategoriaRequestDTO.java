package com.eventu.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Datos para crear o editar una categoría. "activo" solo se usa al editar (activar o desactivar) */
public class CategoriaRequestDTO {

    @NotBlank(message = "El nombre de la categoría es obligatorio.")
    @Size(max = 50, message = "El nombre de la categoría no puede superar los 50 caracteres.")
    private String nombre;

    private Boolean activo;

    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }
    public Boolean getActivo() { return activo; }
    public void setActivo(Boolean activo) { this.activo = activo; }
}
