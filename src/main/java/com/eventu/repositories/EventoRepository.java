package com.eventu.repositories;

import com.eventu.models.EstadoEvento;
import com.eventu.models.Evento;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

/** Acceso a los datos de la tabla "eventos" */
@Repository
public interface EventoRepository extends JpaRepository<Evento, Long>, JpaSpecificationExecutor<Evento> {

    /** Indica si el organizador tiene eventos en ese estado que todavía no han empezado */
    boolean existsByOrganizadorIdAndEstadoAndFechaInicioAfter(Long organizadorId, EstadoEvento estado, LocalDateTime fecha);

    /** Cuenta los eventos de una categoría en un estado dado (por ejemplo, los publicados) */
    long countByCategoriaIdAndEstado(Long categoriaId, EstadoEvento estado);

    /**
     * Busca el evento bloqueando su fila hasta que termine la transacción.
     * Se usa al cambiar los cupos para que dos inscripciones simultáneas no ocupen el mismo cupo.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT e FROM Evento e WHERE e.id = :id")
    Optional<Evento> buscarPorIdConBloqueo(@Param("id") Long id);
}
