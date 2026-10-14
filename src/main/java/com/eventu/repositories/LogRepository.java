package com.eventu.repositories;

import com.eventu.models.Log;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/** Acceso a los datos de la tabla "logs" */
@Repository
public interface LogRepository extends JpaRepository<Log, Long> {
}
