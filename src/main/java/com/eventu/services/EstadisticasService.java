package com.eventu.services;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.eventu.dto.CategoriaConcurridaDTO;
import com.eventu.dto.EstadisticasResponseDTO;
import com.eventu.dto.EventoConMasInscritosDTO;
import com.eventu.models.EstadoInscripcion;
import com.eventu.models.Rol;
import com.eventu.models.Usuario;
import com.eventu.repositories.AsistenciaRepository;
import com.eventu.repositories.InscripcionRepository;

/** Dashboard de estadísticas de eventos, inscripciones y asistencias (HU-16) */
@Service
public class EstadisticasService {

    /** Cuántas categorías y eventos se muestran en los rankings del administrador */
    static final int TAMANO_RANKING = 5;

    private final InscripcionRepository inscripcionRepository;
    private final AsistenciaRepository asistenciaRepository;
    private final AutorizacionService autorizacionService;

    public EstadisticasService(InscripcionRepository inscripcionRepository,
                               AsistenciaRepository asistenciaRepository,
                               AutorizacionService autorizacionService) {
        this.inscripcionRepository = inscripcionRepository;
        this.asistenciaRepository = asistenciaRepository;
        this.autorizacionService = autorizacionService;
    }

    /**
     * Un organizador ve los indicadores solo de sus eventos. Un administrador los ve de toda la plataforma
     * y además los rankings de categorías y eventos con más inscritos.
     * El porcentaje de asistencia es asistentes / inscripciones activas × 100, o 0 si no hay inscripciones
     *
     * @throws com.eventu.exceptions.AccesoDenegadoException si el usuario tiene rol USUARIO
     */
    @Transactional(readOnly = true)
    public EstadisticasResponseDTO obtenerEstadisticas(Long usuarioId) {
        Usuario gestor = autorizacionService.obtenerGestorDeEventos(usuarioId);

        if (gestor.getRol() == Rol.ADMIN) {
            long inscripciones = inscripcionRepository.countByEstado(EstadoInscripcion.ACTIVA);
            long asistentes = asistenciaRepository.count();
            List<CategoriaConcurridaDTO> categorias = inscripcionRepository
                    .findCategoriasMasConcurridas(EstadoInscripcion.ACTIVA, PageRequest.of(0, TAMANO_RANKING));
            List<EventoConMasInscritosDTO> eventos = inscripcionRepository
                    .findEventosConMasInscritos(EstadoInscripcion.ACTIVA, PageRequest.of(0, TAMANO_RANKING));
            return new EstadisticasResponseDTO(inscripciones, asistentes, porcentaje(asistentes, inscripciones),
                    categorias, eventos);
        }

        long inscripciones = inscripcionRepository
                .countByEventoOrganizadorIdAndEstado(usuarioId, EstadoInscripcion.ACTIVA);
        long asistentes = inscripcionRepository.contarAsistenciasDeOrganizador(usuarioId);
        return new EstadisticasResponseDTO(inscripciones, asistentes, porcentaje(asistentes, inscripciones),
                List.of(), List.of());
    }

    private static double porcentaje(long asistentes, long inscripciones) {
        if (inscripciones == 0) {
            return 0.0;
        }
        return BigDecimal.valueOf(asistentes * 100.0 / inscripciones).setScale(2, RoundingMode.HALF_UP).doubleValue();
    }
}
