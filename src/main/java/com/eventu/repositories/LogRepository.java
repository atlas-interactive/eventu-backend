package com.eventu.repositories;

import com.eventu.models.Log;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

/** Acceso a los datos de la tabla "logs" */
@Repository
public interface LogRepository extends JpaRepository<Log, Long>, JpaSpecificationExecutor<Log> {

    /** Acciones que aparecen en los logs, para ofrecerlas como opciones de filtro (HU-20) */
    @Query("SELECT DISTINCT l.accion FROM Log l ORDER BY l.accion")
    List<String> listarAcciones();

    /** Entidades que aparecen en los logs, para ofrecerlas como opciones de filtro (HU-20) */
    @Query("SELECT DISTINCT l.entidad FROM Log l ORDER BY l.entidad")
    List<String> listarEntidades();
}
