package com.eventu.models;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "eventos")
public class Evento {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 150)
    private String titulo;

    @Column(columnDefinition = "TEXT")
    private String descripcion;

    @Column(name = "fecha_inicio", nullable = false)
    private LocalDateTime fechaInicio;

    @Column(nullable = false, length = 150)
    private String ubicacion;

    @Column(name = "cupos_maximos", nullable = false)
    private Integer cuposMaximos;

    @Column(name = "cupos_disponibles", nullable = false)
    private Integer cuposDisponibles;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EstadoEvento estado = EstadoEvento.PUBLICADO;

    @Column(nullable = false, columnDefinition = "boolean default false")
    private Boolean certificable = false;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "organizador_id", nullable = false)
    private Usuario organizador;

    @Column(name = "creado_en", updatable = false)
    private LocalDateTime creadoEn;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "categoria_id")
    private Categoria categoria;

    @PrePersist
    protected void onCreate() {
        this.creadoEn = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

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

    public Integer getCuposDisponibles() { return cuposDisponibles; }
    public void setCuposDisponibles(Integer cuposDisponibles) { this.cuposDisponibles = cuposDisponibles; }

    public EstadoEvento getEstado() { return estado; }
    public void setEstado(EstadoEvento estado) { this.estado = estado; }

    public Boolean getCertificable() { return certificable; }
    public void setCertificable(Boolean certificable) { this.certificable = certificable; }

    public Usuario getOrganizador() { return organizador; }
    public void setOrganizador(Usuario organizador) { this.organizador = organizador; }

    public LocalDateTime getCreadoEn() { return creadoEn; }
    public void setCreadoEn(LocalDateTime creadoEn) { this.creadoEn = creadoEn; }

    public Categoria getCategoria() { return categoria; }
    public void setCategoria(Categoria categoria) { this.categoria = categoria; }
}