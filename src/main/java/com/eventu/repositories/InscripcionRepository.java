package com.eventu.repositories;

import com.eventu.models.EstadoInscripcion;
import com.eventu.models.Inscripcion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface InscripcionRepository extends JpaRepository<Inscripcion, Long> {
    boolean existsByEstudianteIdAndEventoIdAndEstado(Long estudianteId, Long eventoId, EstadoInscripcion estado);
    List<Inscripcion> findByEstudianteId(Long estudianteId);
    Optional<Inscripcion> findByCodigoQr(String codigoQr);
    Optional<Inscripcion> findByEstudianteIdAndEventoId(Long estudianteId, Long eventoId);
    Optional<Inscripcion> findByEstudianteIdAndEventoIdAndEstado(Long estudianteId, Long eventoId, EstadoInscripcion estado);
}