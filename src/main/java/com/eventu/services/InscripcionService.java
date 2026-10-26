package com.eventu.services;

import com.eventu.dto.InscripcionResponseDTO;
import com.eventu.dto.InscritoResponseDTO;
import com.eventu.exceptions.AccesoDenegadoException;
import com.eventu.exceptions.ConflictoException;
import com.eventu.exceptions.RecursoNoEncontradoException;
import com.eventu.models.EstadoEvento;
import com.eventu.models.EstadoInscripcion;
import com.eventu.models.Evento;
import com.eventu.models.Inscripcion;
import com.eventu.models.Rol;
import com.eventu.models.Usuario;
import com.eventu.repositories.AsistenciaRepository;
import com.eventu.repositories.EventoRepository;
import com.eventu.repositories.InscripcionRepository;
import com.eventu.util.Mensajes;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/** Inscripción, cancelación y consulta de inscripciones de un usuario*/
@Service
public class InscripcionService {

    private static final String PREFIJO_QR = "EVENTU-QR-";

    private final InscripcionRepository inscripcionRepository;
    private final EventoRepository eventoRepository;
    private final AsistenciaRepository asistenciaRepository;
    private final AutorizacionService autorizacionService;
    private final LogService logService;

    public InscripcionService(InscripcionRepository inscripcionRepository, EventoRepository eventoRepository,
                              AsistenciaRepository asistenciaRepository, AutorizacionService autorizacionService,
                              LogService logService) {
        this.inscripcionRepository = inscripcionRepository;
        this.eventoRepository = eventoRepository;
        this.asistenciaRepository = asistenciaRepository;
        this.autorizacionService = autorizacionService;
        this.logService = logService;
    }

    /**
     * Inscribe a un usuario en un evento y genera su código QR único
     * Pueden inscribirse los usuarios con rol USUARIO u ORGANIZADOR; el administrador no
     *
     * @throws AccesoDenegadoException si el usuario es administrador
     * @throws ConflictoException si el evento está cancelado o ya inició, si el usuario ya tiene una inscripción activa o si no quedan cupos
     */
    @Transactional
    public Inscripcion inscribirUsuario(Long usuarioId, Long eventoId) {
        Usuario usuario = autorizacionService.obtenerUsuario(usuarioId);
        if (usuario.getRol() == Rol.ADMIN) {
            logService.registrarFallo(usuarioId, LogService.ACCION_INSCRIBIR, LogService.ENTIDAD_EVENTO, eventoId,
                    "Un administrador no puede inscribirse");
            throw new AccesoDenegadoException("Los administradores no pueden inscribirse en eventos.");
        }
        // La fila del evento se bloquea: dos inscripciones simultáneas al último cupo se atienden una tras otra
        Evento evento = eventoRepository.buscarPorIdConBloqueo(eventoId)
                .orElseThrow(() -> new RecursoNoEncontradoException(Mensajes.EVENTO_NO_ENCONTRADO));

        if (evento.getEstado() != EstadoEvento.PUBLICADO) {
            throw rechazarInscripcion(usuarioId, eventoId, "No se permiten inscripciones en eventos que no estén publicados.");
        }
        if (evento.getFechaInicio().isBefore(LocalDateTime.now())) {
            throw rechazarInscripcion(usuarioId, eventoId, "El evento ya inició; no es posible inscribirse.");
        }
        if (inscripcionRepository.existsByUsuarioIdAndEventoIdAndEstado(usuarioId, eventoId, EstadoInscripcion.ACTIVA)) {
            throw rechazarInscripcion(usuarioId, eventoId, "Ya tienes una inscripción activa para este evento.");
        }
        if (evento.getCuposDisponibles() <= 0) {
            throw rechazarInscripcion(usuarioId, eventoId, "El evento ya no cuenta con cupos disponibles.");
        }

        evento.setCuposDisponibles(evento.getCuposDisponibles() - 1);
        eventoRepository.save(evento);

        Inscripcion inscripcion = new Inscripcion();
        inscripcion.setUsuario(usuario);
        inscripcion.setEvento(evento);
        inscripcion.setEstado(EstadoInscripcion.ACTIVA);
        inscripcion.setCodigoQr(PREFIJO_QR + UUID.randomUUID().toString().toUpperCase());
        Inscripcion guardada = inscripcionRepository.save(inscripcion);

        logService.registrarExito(usuarioId, LogService.ACCION_INSCRIBIR, LogService.ENTIDAD_INSCRIPCION, guardada.getId());
        return guardada;
    }

    /**
     * Cancela una inscripción y libera su cupo. La inscripción se conserva como CANCELADA
     *
     * @throws AccesoDenegadoException si la inscripción pertenece a otro usuario
     * @throws ConflictoException si ya estaba cancelada o si ya tiene asistencia registrada
     */
    @Transactional
    public Inscripcion cancelarInscripcion(Long inscripcionId, Long usuarioId) {
        Inscripcion inscripcion = buscarInscripcion(inscripcionId);

        if (!inscripcion.getUsuario().getId().equals(usuarioId)) {
            logService.registrarFallo(usuarioId, LogService.ACCION_CANCELAR_INSCRIPCION,
                    LogService.ENTIDAD_INSCRIPCION, inscripcionId, "Inscripción de otro usuario");
            throw new AccesoDenegadoException("No tienes permiso para cancelar esta inscripción.");
        }
        if (inscripcion.getEstado() == EstadoInscripcion.CANCELADA) {
            logService.registrarFallo(usuarioId, LogService.ACCION_CANCELAR_INSCRIPCION,
                    LogService.ENTIDAD_INSCRIPCION, inscripcionId, "Inscripción ya cancelada");
            throw new ConflictoException("Esta inscripción ya estaba cancelada.");
        }
        if (asistenciaRepository.existsByInscripcionId(inscripcionId)) {
            logService.registrarFallo(usuarioId, LogService.ACCION_CANCELAR_INSCRIPCION,
                    LogService.ENTIDAD_INSCRIPCION, inscripcionId, "Asistencia ya registrada");
            throw new ConflictoException("No puedes cancelar tu inscripción porque ya asististe al evento.");
        }

        Evento evento = eventoRepository.buscarPorIdConBloqueo(inscripcion.getEvento().getId())
                .orElseThrow(() -> new RecursoNoEncontradoException(Mensajes.EVENTO_NO_ENCONTRADO));
        inscripcion.setEstado(EstadoInscripcion.CANCELADA);
        evento.setCuposDisponibles(evento.getCuposDisponibles() + 1);
        eventoRepository.save(evento);
        Inscripcion guardada = inscripcionRepository.save(inscripcion);

        logService.registrarExito(usuarioId, LogService.ACCION_CANCELAR_INSCRIPCION, LogService.ENTIDAD_INSCRIPCION, inscripcionId);
        return guardada;
    }

    /**
     * Lista los participantes con inscripción activa en un evento, con su estado de asistencia.
     * Lo consultan el organizador dueño del evento o un administrador
     *
     * @throws AccesoDenegadoException si el usuario no puede gestionar eventos o el evento es de otro organizador
     * @throws RecursoNoEncontradoException si el evento no existe
     */
    @Transactional(readOnly = true)
    public List<InscritoResponseDTO> listarInscritosDeEvento(Long eventoId, Long usuarioId) {
        Usuario gestor = autorizacionService.obtenerGestorDeEventos(usuarioId);
        Evento evento = eventoRepository.findById(eventoId)
                .orElseThrow(() -> new RecursoNoEncontradoException(Mensajes.EVENTO_NO_ENCONTRADO));
        if (!autorizacionService.puedeModificarEvento(gestor, evento)) {
            logService.registrarFallo(usuarioId, LogService.ACCION_CONSULTAR_INSCRITOS, LogService.ENTIDAD_EVENTO,
                    eventoId, "Intento de consultar los inscritos de un evento ajeno");
            throw new AccesoDenegadoException(Mensajes.EVENTO_AJENO);
        }

        // Una sola consulta con las asistencias del evento, en lugar de una por cada inscrito
        Set<Long> conAsistencia = new HashSet<>(asistenciaRepository.findInscripcionIdsConAsistenciaPorEvento(eventoId));
        return inscripcionRepository.findByEventoIdAndEstado(eventoId, EstadoInscripcion.ACTIVA).stream()
                .map(i -> InscritoResponseDTO.desde(i, conAsistencia.contains(i.getId())))
                .toList();
    }

    /** Lista todas las inscripciones de un usuario, activas e históricas*/
    @Transactional(readOnly = true)
    public List<InscripcionResponseDTO> listarInscripcionesDeUsuario(Long usuarioId) {
        return inscripcionRepository.findByUsuarioId(usuarioId).stream()
                .map(InscripcionResponseDTO::desde)
                .toList();
    }

    /**
     * Entrega el código QR de una inscripción activa a su dueño
     *
     * @return inscripcionId, codigoQr, estado y título del evento (se arma dentro de la transacción porque lee el evento, que es una relación perezosa)
     * @throws AccesoDenegadoException si la inscripción pertenece a otro usuario
     * @throws ConflictoException si la inscripción está cancelada
     */
    @Transactional(readOnly = true)
    public Map<String, Object> obtenerCodigoQr(Long inscripcionId, Long usuarioId) {
        Inscripcion inscripcion = buscarInscripcion(inscripcionId);

        if (!inscripcion.getUsuario().getId().equals(usuarioId)) {
            throw new AccesoDenegadoException("No tienes permiso para ver este código QR.");
        }
        if (inscripcion.getEstado() == EstadoInscripcion.CANCELADA) {
            throw new ConflictoException("La inscripción está cancelada; el código QR ya no es válido.");
        }
        return Map.<String, Object>of(
                "inscripcionId", inscripcion.getId(),
                "codigoQr", inscripcion.getCodigoQr(),
                "estado", inscripcion.getEstado().name(),
                "evento", inscripcion.getEvento().getTitulo());
    }

    private Inscripcion buscarInscripcion(Long inscripcionId) {
        return inscripcionRepository.findById(inscripcionId)
                .orElseThrow(() -> new RecursoNoEncontradoException(Mensajes.INSCRIPCION_NO_ENCONTRADA));
    }

    private ConflictoException rechazarInscripcion(Long usuarioId, Long eventoId, String motivo) {
        logService.registrarFallo(usuarioId, LogService.ACCION_INSCRIBIR, LogService.ENTIDAD_EVENTO, eventoId, motivo);
        return new ConflictoException(motivo);
    }
}
