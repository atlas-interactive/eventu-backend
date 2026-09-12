package com.eventu.repositories;

import com.eventu.models.Evento;
import com.eventu.models.EstadoEvento;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface EventoRepository extends JpaRepository<Evento, Long> {
    List<Evento> findByEstado(EstadoEvento estado);
    List<Evento> findByOrganizadorId(Long organizadorId);
}