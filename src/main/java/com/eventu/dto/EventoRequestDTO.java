package com.eventu.dto;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;

/** Datos para crear un evento. Todos los campos son obligatorios excepto "certificable" */
public class EventoRequestDTO {

    @NotBlank(message = "El título es obligatorio.")
    @Size(max = 150, message = "El título no puede superar los 150 caracteres.")
    private String titulo;

    @NotBlank(message = "La descripción es obligatoria.")
    private String descripcion;

    @NotNull(message = "La fecha y hora de inicio son obligatorias.")
    @Future(message = "La fecha de inicio debe ser futura.")
    private LocalDateTime fechaInicio;

    @NotBlank(message = "La ubicación es obligatoria.")
    @Size(max = 150, message = "La ubicación no puede superar los 150 caracteres.")
    private String ubicacion;

    @NotNull(message = "El cupo máximo es obligatorio.")
    @Min(value = 1, message = "El cupo máximo debe ser mayor a cero.")
    private Integer cuposMaximos;

    private Boolean certificable;

    @NotNull(message = "La categoría es obligatoria.")
    private Long categoriaId;

    public String getTitulo() { return titulo; }
    public void setTitulo(String titulo) { this.titulo = titulo; }
    public String getDescripcion() { return descripcion; }
    public void setDescripcion(String descripcion) { this.descripcion = descripcion; }
    public LocalDateTime getFechaInicio() { return fechaInicio; }
    public void setFechaInicio(LocalDateTime fechaInicio) { this.fechaInicio = fechaInicio; }
    public String getUbicacion() { return ubicacion; }
    public void setUbicacion(String ubicacion) { this.ubicacion = ubicacion; }
    public Integer getCuposMaximos() { return cuposMaximos; }
    public void setCuposMaximos(Integer cuposMaximos) { this.cuposMaximos = cuposMaximos; }
    public Boolean getCertificable() { return certificable; }
    public void setCertificable(Boolean certificable) { this.certificable = certificable; }
    public Long getCategoriaId() { return categoriaId; }
    public void setCategoriaId(Long categoriaId) { this.categoriaId = categoriaId; }
}
