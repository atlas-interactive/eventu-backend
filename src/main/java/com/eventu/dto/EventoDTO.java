package com.eventu.dto;

import java.time.LocalDateTime;

public class EventoDTO {
    private String titulo;
    private String descripcion;
    private LocalDateTime fechaInicio;
    private String ubicacion;
    private Integer cuposMaximos;
    private Boolean certificable;

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
}