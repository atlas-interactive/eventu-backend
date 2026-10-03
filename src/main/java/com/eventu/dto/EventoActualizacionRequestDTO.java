package com.eventu.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;

/**
 * Datos para editar un evento. Todos los campos son opcionales, solo se modifican los que vienen en la petición
 */
public class EventoActualizacionRequestDTO {

    @Pattern(regexp = "(?s).*\\S.*", message = "El título no puede estar vacío.")
    @Size(max = 150, message = "El título no puede superar los 150 caracteres.")
    private String titulo;

    @Pattern(regexp = "(?s).*\\S.*", message = "La descripción no puede estar vacía.")
    private String descripcion;

    private LocalDateTime fechaInicio;

    @Pattern(regexp = "(?s).*\\S.*", message = "La ubicación no puede estar vacía.")
    @Size(max = 150, message = "La ubicación no puede superar los 150 caracteres.")
    private String ubicacion;

    @Min(value = 1, message = "El cupo máximo debe ser mayor a cero.")
    private Integer cuposMaximos;

    private Boolean certificable;

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
