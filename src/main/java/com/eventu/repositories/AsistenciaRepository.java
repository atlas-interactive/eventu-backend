package com.eventu.repositories;

import com.eventu.models.Asistencia;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface AsistenciaRepository extends JpaRepository<Asistencia, Long> {
    boolean existsByInscripcionId(Long inscripcionId);
}