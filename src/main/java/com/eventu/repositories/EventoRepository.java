package com.eventu.repositories;

import com.eventu.models.EstadoEvento;
import com.eventu.models.Evento;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface EventoRepository extends JpaRepository<Evento, Long> {

    List<Evento> findByEstado(EstadoEvento estado);

    List<Evento> findByOrganizadorId(Long organizadorId);

    /**
     * Busca el evento bloqueando su fila hasta que termine la transacción.
     * Se usa al cambiar los cupos para que dos inscripciones simultáneas no ocupen el mismo cupo.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT e FROM Evento e WHERE e.id = :id")
    Optional<Evento> buscarPorIdConBloqueo(@Param("id") Long id);
}
