package com.eventu.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.eventu.models.Asistencia;

import java.util.List;

@Repository
public interface AsistenciaRepository extends JpaRepository<Asistencia, Long> {
    boolean existsByInscripcionId(Long inscripcionId);

    /** Ids de las inscripciones de un evento que ya tienen asistencia registrada (una sola consulta para todo el evento) */
    @Query("SELECT a.inscripcion.id FROM Asistencia a WHERE a.inscripcion.evento.id = :eventoId")
    List<Long> findInscripcionIdsConAsistenciaPorEvento(@Param("eventoId") Long eventoId);
}