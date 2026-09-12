package com.eventu.models;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "inscripciones", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"estudiante_id", "evento_id"})
})
public class Inscripcion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "estudiante_id", nullable = false)
    private Usuario estudiante;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "evento_id", nullable = false)
    private Evento evento;

    @Column(name = "codigo_qr", nullable = false, unique = true, columnDefinition = "TEXT")
    private String codigoQr;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EstadoInscripcion estado = EstadoInscripcion.ACTIVA;

    @Column(name = "fecha_inscripcion", updatable = false)
    private LocalDateTime fechaInscripcion;

    @PrePersist
    protected void onCreate() {
        this.fechaInscripcion = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Usuario getEstudiante() { return estudiante; }
    public void setEstudiante(Usuario estudiante) { this.estudiante = estudiante; }

    public Evento getEvento() { return evento; }
    public void setEvento(Evento evento) { this.evento = evento; }

    public String getCodigoQr() { return codigoQr; }
    public void setCodigoQr(String codigoQr) { this.codigoQr = codigoQr; }

    public EstadoInscripcion getEstado() { return estado; }
    public void setEstado(EstadoInscripcion estado) { this.estado = estado; }

    public LocalDateTime getFechaInscripcion() { return fechaInscripcion; }
    public void setFechaInscripcion(LocalDateTime fechaInscripcion) { this.fechaInscripcion = fechaInscripcion; }
}