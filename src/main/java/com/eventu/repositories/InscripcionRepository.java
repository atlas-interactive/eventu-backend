package com.eventu.repositories;

import com.eventu.models.EstadoInscripcion;
import com.eventu.models.Inscripcion;
import com.eventu.dto.CategoriaConcurridaDTO;
import com.eventu.dto.EventoConMasInscritosDTO;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
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

    /** Inscripciones en un estado dado en toda la plataforma */
    long countByEstado(EstadoInscripcion estado);

    /** Inscripciones en un estado dado de los eventos creados por un organizador */
    long countByEventoOrganizadorIdAndEstado(Long organizadorId, EstadoInscripcion estado);

    /** Asistencias registradas en los eventos creados por un organizador */
    @Query("SELECT COUNT(a) FROM Asistencia a WHERE a.inscripcion.evento.organizador.id = :organizadorId")
    long contarAsistenciasDeOrganizador(@Param("organizadorId") Long organizadorId);

    /** Categorías con más inscripciones en un estado dado, de mayor a menor (el límite lo fija el Pageable) */
    @Query("""
            SELECT new com.eventu.dto.CategoriaConcurridaDTO(c.nombre, COUNT(i))
            FROM Inscripcion i JOIN i.evento e JOIN e.categoria c
            WHERE i.estado = :estado
            GROUP BY c.id, c.nombre
            ORDER BY COUNT(i) DESC, c.nombre ASC""")
    List<CategoriaConcurridaDTO> findCategoriasMasConcurridas(@Param("estado") EstadoInscripcion estado,
                                                              Pageable limite);

    /** Eventos con más inscripciones en un estado dado, de mayor a menor (el límite lo fija el Pageable) */
    @Query("""
            SELECT new com.eventu.dto.EventoConMasInscritosDTO(e.id, e.titulo, COUNT(i))
            FROM Inscripcion i JOIN i.evento e
            WHERE i.estado = :estado
            GROUP BY e.id, e.titulo
            ORDER BY COUNT(i) DESC, e.titulo ASC""")
    List<EventoConMasInscritosDTO> findEventosConMasInscritos(@Param("estado") EstadoInscripcion estado,
                                                              Pageable limite);
}
