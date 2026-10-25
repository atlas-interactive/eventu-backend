package com.eventu.repositories;

import com.eventu.models.EstadoInscripcion;
import com.eventu.models.Inscripcion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface InscripcionRepository extends JpaRepository<Inscripcion, Long> {

    boolean existsByUsuarioIdAndEventoIdAndEstado(Long usuarioId, Long eventoId, EstadoInscripcion estado);

    List<Inscripcion> findByUsuarioId(Long usuarioId);

    /** Inscripciones de un evento en un estado dado (por ejemplo, las activas al cancelarlo) */
    List<Inscripcion> findByEventoIdAndEstado(Long eventoId, EstadoInscripcion estado);

    Optional<Inscripcion> findByCodigoQr(String codigoQr);

    Optional<Inscripcion> findByUsuarioIdAndEventoIdAndEstado(Long usuarioId, Long eventoId, EstadoInscripcion estado);
}
