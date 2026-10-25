package com.eventu.services;

import com.eventu.dto.EventoActualizacionRequestDTO;
import com.eventu.dto.EventoRequestDTO;
import com.eventu.dto.EventoResponseDTO;
import com.eventu.exceptions.AccesoDenegadoException;
import com.eventu.exceptions.ConflictoException;
import com.eventu.exceptions.RecursoNoEncontradoException;
import com.eventu.exceptions.SolicitudInvalidaException;
import com.eventu.models.Categoria;
import com.eventu.models.EstadoEvento;
import com.eventu.models.EstadoInscripcion;
import com.eventu.models.Evento;
import com.eventu.models.Inscripcion;
import com.eventu.models.Usuario;
import com.eventu.notificaciones.CancelacionEventoNotificada;
import com.eventu.repositories.CategoriaRepository;
import com.eventu.repositories.EventoRepository;
import com.eventu.repositories.InscripcionRepository;
import com.eventu.util.Mensajes;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/** Creación, edición y consulta de eventos */
@Service
public class EventoService {

    private final EventoRepository eventoRepository;
    private final CategoriaRepository categoriaRepository;
    private final InscripcionRepository inscripcionRepository;
    private final AutorizacionService autorizacionService;
    private final LogService logService;
    private final ApplicationEventPublisher publicador;

    public EventoService(EventoRepository eventoRepository, CategoriaRepository categoriaRepository,
                         InscripcionRepository inscripcionRepository, AutorizacionService autorizacionService,
                         LogService logService, ApplicationEventPublisher publicador) {
        this.eventoRepository = eventoRepository;
        this.categoriaRepository = categoriaRepository;
        this.inscripcionRepository = inscripcionRepository;
        this.autorizacionService = autorizacionService;
        this.logService = logService;
        this.publicador = publicador;
    }

    /**
     * Crea un evento publicado, con todos los cupos disponibles
     * @param request datos del evento ya validados
     * @param organizadorId usuario que lo crea
     * @return el evento guardado
     * @throws AccesoDenegadoException si el usuario no puede gestionar eventos
     * @throws SolicitudInvalidaException si la categoría no existe o está inactiva
     */
    @Transactional
    public Evento crearEvento(EventoRequestDTO request, Long organizadorId) {
        Usuario organizador = autorizacionService.obtenerGestorDeEventos(organizadorId);
        Categoria categoria = buscarCategoriaActiva(request.getCategoriaId());

        Evento nuevoEvento = new Evento();
        nuevoEvento.setTitulo(request.getTitulo().trim());
        nuevoEvento.setDescripcion(request.getDescripcion().trim());
        nuevoEvento.setFechaInicio(request.getFechaInicio());
        nuevoEvento.setUbicacion(request.getUbicacion().trim());
        nuevoEvento.setCuposMaximos(request.getCuposMaximos());
        nuevoEvento.setCuposDisponibles(request.getCuposMaximos());
        nuevoEvento.setCertificable(Boolean.TRUE.equals(request.getCertificable()));
        nuevoEvento.setEstado(EstadoEvento.PUBLICADO);
        nuevoEvento.setOrganizador(organizador);
        nuevoEvento.setCategoria(categoria);

        Evento guardado = eventoRepository.save(nuevoEvento);
        logService.registrarExito(organizadorId, LogService.ACCION_CREAR_EVENTO, LogService.ENTIDAD_EVENTO, guardado.getId());
        return guardado;
    }

    /** Lista los eventos publicados. Los cancelados no aparecen en la consulta */
    @Transactional(readOnly = true)
    public List<EventoResponseDTO> listarEventosActivos() {
        return eventoRepository.findByEstado(EstadoEvento.PUBLICADO).stream()
                .map(EventoResponseDTO::desde)
                .toList();
    }

    /**
     * Edita los campos enviados de un evento
     * @throws AccesoDenegadoException si el usuario no es el dueño del evento
     * @throws ConflictoException si el evento está cancelado, o si el nuevo cupo es menor que los inscritos actuales
     */
    @Transactional
    public Evento actualizarEvento(Long eventoId, EventoActualizacionRequestDTO request, Long organizadorId) {
        Usuario gestor = autorizacionService.obtenerGestorDeEventos(organizadorId);
        // Se bloquea la fila para que una inscripción simultánea no desajuste los cupos
        Evento evento = eventoRepository.buscarPorIdConBloqueo(eventoId)
                .orElseThrow(() -> new RecursoNoEncontradoException(Mensajes.EVENTO_NO_ENCONTRADO));

        if (!autorizacionService.puedeModificarEvento(gestor, evento)) {
            logService.registrarFallo(organizadorId, LogService.ACCION_EDITAR_EVENTO, LogService.ENTIDAD_EVENTO,
                    eventoId, "Intento de editar un evento ajeno");
            throw new AccesoDenegadoException(Mensajes.EVENTO_AJENO);
        }
        if (evento.getEstado() == EstadoEvento.CANCELADO) {
            logService.registrarFallo(organizadorId, LogService.ACCION_EDITAR_EVENTO, LogService.ENTIDAD_EVENTO,
                    eventoId, "Evento cancelado");
            throw new ConflictoException("No se puede editar un evento cancelado.");
        }

        aplicarCambios(evento, request);

        Evento guardado = eventoRepository.save(evento);
        logService.registrarExito(organizadorId, LogService.ACCION_EDITAR_EVENTO, LogService.ENTIDAD_EVENTO, eventoId);
        return guardado;
    }

    /**
     * Cancela un evento: pasa sus inscripciones activas a CANCELADA (se conservan, no se borran)
     * y avisa por correo a esos usuarios cuando la transacción se confirma
     *
     * @throws AccesoDenegadoException si el usuario no es el dueño del evento ni administrador
     * @throws ConflictoException si el evento ya estaba cancelado
     */
    @Transactional
    public Evento cancelarEvento(Long eventoId, Long usuarioId) {
        Evento evento = obtenerEventoGestionable(eventoId, usuarioId, LogService.ACCION_CANCELAR_EVENTO);
        if (evento.getEstado() == EstadoEvento.CANCELADO) {
            logService.registrarFallo(usuarioId, LogService.ACCION_CANCELAR_EVENTO, LogService.ENTIDAD_EVENTO,
                    eventoId, "Evento ya cancelado");
            throw new ConflictoException("El evento ya está cancelado.");
        }

        List<Inscripcion> activas = inscripcionRepository.findByEventoIdAndEstado(eventoId, EstadoInscripcion.ACTIVA);
        List<String> correos = activas.stream().map(i -> i.getUsuario().getCorreo()).toList();
        activas.forEach(i -> i.setEstado(EstadoInscripcion.CANCELADA));
        inscripcionRepository.saveAll(activas);

        evento.setEstado(EstadoEvento.CANCELADO);
        Evento guardado = eventoRepository.save(evento);
        logService.registrarExito(usuarioId, LogService.ACCION_CANCELAR_EVENTO, LogService.ENTIDAD_EVENTO, eventoId);

        if (!correos.isEmpty()) {
            publicador.publishEvent(new CancelacionEventoNotificada(
                    eventoId, evento.getTitulo(), evento.getFechaInicio(), evento.getUbicacion(), correos));
        }
        return guardado;
    }

    /**
     * Busca el evento con su fila bloqueada y verifica que el usuario pueda gestionarlo
     * (organizador dueño o administrador)
     *
     * @param accion acción que se registra en el log si el acceso se deniega
     * @throws RecursoNoEncontradoException si el evento no existe
     * @throws AccesoDenegadoException si el usuario no puede gestionar eventos o el evento es de otro organizador
     */
    private Evento obtenerEventoGestionable(Long eventoId, Long usuarioId, String accion) {
        Usuario gestor = autorizacionService.obtenerGestorDeEventos(usuarioId);
        Evento evento = eventoRepository.buscarPorIdConBloqueo(eventoId)
                .orElseThrow(() -> new RecursoNoEncontradoException(Mensajes.EVENTO_NO_ENCONTRADO));
        if (!autorizacionService.puedeModificarEvento(gestor, evento)) {
            logService.registrarFallo(usuarioId, accion, LogService.ENTIDAD_EVENTO, eventoId, "Evento de otro organizador");
            throw new AccesoDenegadoException(Mensajes.EVENTO_AJENO);
        }
        return evento;
    }

    /**
     * Copia al evento solo los campos que vienen en la petición.
     * Al cambiar el cupo máximo conserva el número de inscritos y recalcula los cupos disponibles
     */
    private void aplicarCambios(Evento evento, EventoActualizacionRequestDTO request) {
        if (request.getTitulo() != null) {
            evento.setTitulo(request.getTitulo().trim());
        }
        if (request.getDescripcion() != null) {
            evento.setDescripcion(request.getDescripcion().trim());
        }
        if (request.getFechaInicio() != null) {
            evento.setFechaInicio(request.getFechaInicio());
        }
        if (request.getUbicacion() != null) {
            evento.setUbicacion(request.getUbicacion().trim());
        }
        if (request.getCertificable() != null) {
            evento.setCertificable(request.getCertificable());
        }

        if (request.getCuposMaximos() != null) {
            int inscritos = evento.getCuposMaximos() - evento.getCuposDisponibles();
            if (request.getCuposMaximos() < inscritos) {
                throw new ConflictoException("No se puede reducir la capacidad por debajo de los inscritos actuales (" + inscritos + ").");
            }
            evento.setCuposDisponibles(request.getCuposMaximos() - inscritos);
            evento.setCuposMaximos(request.getCuposMaximos());
        }

        if (request.getCategoriaId() != null) {
            Categoria nueva = buscarCategoria(request.getCategoriaId());
            boolean cambiaDeCategoria = evento.getCategoria() == null
                    || !evento.getCategoria().getId().equals(nueva.getId());
            if (cambiaDeCategoria && !Boolean.TRUE.equals(nueva.getActivo())) {
                throw new SolicitudInvalidaException(Mensajes.CATEGORIA_INACTIVA);
            }
            evento.setCategoria(nueva);
        }
    }

    /** @throws RecursoNoEncontradoException si la categoría no existe */
    private Categoria buscarCategoria(Long categoriaId) {
        return categoriaRepository.findById(categoriaId)
                .orElseThrow(() -> new RecursoNoEncontradoException(Mensajes.CATEGORIA_NO_ENCONTRADA));
    }

    /** Una categoría inactiva no se ofrece al crear eventos nuevos */
    private Categoria buscarCategoriaActiva(Long categoriaId) {
        Categoria categoria = buscarCategoria(categoriaId);
        if (!Boolean.TRUE.equals(categoria.getActivo())) {
            throw new SolicitudInvalidaException(Mensajes.CATEGORIA_INACTIVA);
        }
        return categoria;
    }
}
