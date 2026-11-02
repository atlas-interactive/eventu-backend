package com.eventu.services;

import com.eventu.dto.PerfilActualizacionDTO;
import com.eventu.dto.PerfilResponseDTO;
import com.eventu.exceptions.ConflictoException;
import com.eventu.exceptions.RecursoNoEncontradoException;
import com.eventu.exceptions.SolicitudInvalidaException;
import com.eventu.models.EstadoEvento;
import com.eventu.models.EstadoInscripcion;
import com.eventu.models.Evento;
import com.eventu.models.Inscripcion;
import com.eventu.models.Rol;
import com.eventu.models.Usuario;
import com.eventu.notificaciones.ContrasenaCambiada;
import com.eventu.repositories.EventoRepository;
import com.eventu.repositories.InscripcionRepository;
import com.eventu.repositories.UsuarioRepository;
import com.eventu.util.Mensajes;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/** Consulta y edición del perfil propio, cambio de contraseña y desactivación de la cuenta (HU-19) */
@Service
public class PerfilService {

    static final String CORREO_NO_EDITABLE = "El correo institucional no se puede modificar.";
    static final String MISMA_CONTRASENA = "La contraseña nueva debe ser diferente de la actual.";
    static final String ADMINISTRADOR_UNICO = "No puedes desactivar tu cuenta porque eres el único administrador activo.";
    static final String EVENTOS_PUBLICADOS = "Tienes eventos publicados. Cancélalos o espera a que finalicen para desactivar tu cuenta.";

    private final UsuarioRepository usuarioRepository;
    private final EventoRepository eventoRepository;
    private final InscripcionRepository inscripcionRepository;
    private final PasswordEncoder passwordEncoder;
    private final RecuperacionContrasenaService recuperacionService;
    private final LogService logService;
    private final ApplicationEventPublisher publicador;

    public PerfilService(UsuarioRepository usuarioRepository, EventoRepository eventoRepository,
                         InscripcionRepository inscripcionRepository, PasswordEncoder passwordEncoder,
                         RecuperacionContrasenaService recuperacionService, LogService logService,
                         ApplicationEventPublisher publicador) {
        this.usuarioRepository = usuarioRepository;
        this.eventoRepository = eventoRepository;
        this.inscripcionRepository = inscripcionRepository;
        this.passwordEncoder = passwordEncoder;
        this.recuperacionService = recuperacionService;
        this.logService = logService;
        this.publicador = publicador;
    }

    /**
     * Devuelve los datos del usuario autenticado
     *
     * @throws RecursoNoEncontradoException si el usuario no existe
     */
    @Transactional(readOnly = true)
    public PerfilResponseDTO obtenerPerfil(Long usuarioId) {
        return PerfilResponseDTO.desde(buscarUsuario(usuarioId));
    }

    /**
     * Actualiza el nombre y el código institucional. El correo es de solo lectura
     *
     * @return el perfil actualizado
     * @throws SolicitudInvalidaException si se envía un correo distinto al registrado
     * @throws RecursoNoEncontradoException si el usuario no existe
     */
    @Transactional
    public PerfilResponseDTO actualizarPerfil(Long usuarioId, PerfilActualizacionDTO datos) {
        Usuario usuario = buscarUsuario(usuarioId);
        if (datos.getCorreo() != null && !datos.getCorreo().trim().equalsIgnoreCase(usuario.getCorreo())) {
            logService.registrarFallo(usuarioId, LogService.ACCION_EDITAR_PERFIL, LogService.ENTIDAD_USUARIO, usuarioId, "Intento de cambiar el correo");
            throw new SolicitudInvalidaException(CORREO_NO_EDITABLE);
        }

        usuario.setNombre(datos.getNombre().trim());
        String codigo = datos.getCodigoInstitucional();
        usuario.setCodigoInstitucional(codigo == null || codigo.isBlank() ? null : codigo.trim());
        Usuario guardado = usuarioRepository.save(usuario);

        logService.registrarExito(usuarioId, LogService.ACCION_EDITAR_PERFIL, LogService.ENTIDAD_USUARIO, usuarioId);
        return PerfilResponseDTO.desde(guardado);
    }

    /**
     * Cambia la contraseña confirmando la actual. Invalida los enlaces de recuperación pendientes y las sesiones
     * abiertas con la contraseña anterior, y avisa por correo
     *
     * @throws SolicitudInvalidaException si la contraseña actual no coincide o la nueva es igual a la actual
     * @throws RecursoNoEncontradoException si el usuario no existe
     */
    @Transactional
    public void cambiarContrasena(Long usuarioId, String passwordActual, String passwordNueva) {
        Usuario usuario = buscarUsuario(usuarioId);
        exigirContrasena(usuario, passwordActual, LogService.ACCION_CAMBIAR_CONTRASENA);
        if (passwordEncoder.matches(passwordNueva, usuario.getPasswordHash())) {
            throw new SolicitudInvalidaException(MISMA_CONTRASENA);
        }

        LocalDateTime ahora = LocalDateTime.now();
        usuario.setPasswordHash(passwordEncoder.encode(passwordNueva));
        usuario.setPasswordActualizadaEn(ahora);
        usuarioRepository.save(usuario);
        recuperacionService.invalidarTokensPendientes(usuarioId, ahora);

        logService.registrarExito(usuarioId, LogService.ACCION_CAMBIAR_CONTRASENA, LogService.ENTIDAD_USUARIO, usuarioId);
        publicador.publishEvent(new ContrasenaCambiada(usuario.getCorreo(), usuario.getNombre(), ahora));
    }

    /**
     * Desactiva la cuenta propia confirmando la contraseña. La cuenta no se borra: se reactiva al volver a iniciar sesión (HU-02).
     * Cancela sus inscripciones activas en eventos que no han empezado y libera esos cupos; las de eventos ya realizados
     * se conservan porque respaldan la asistencia, los certificados y las estadísticas
     *
     * @throws SolicitudInvalidaException si la contraseña no coincide
     * @throws ConflictoException si es el único administrador activo o tiene eventos publicados que no han empezado
     * @throws RecursoNoEncontradoException si el usuario no existe
     */
    @Transactional
    public void desactivarCuenta(Long usuarioId, String password) {
        Usuario usuario = buscarUsuario(usuarioId);
        exigirContrasena(usuario, password, LogService.ACCION_DESACTIVAR_CUENTA);

        if (usuario.getRol() == Rol.ADMIN && usuarioRepository.countByRolAndActivoTrue(Rol.ADMIN) <= 1) {
            rechazarDesactivacion(usuarioId, ADMINISTRADOR_UNICO);
        }
        LocalDateTime ahora = LocalDateTime.now();
        if (eventoRepository.existsByOrganizadorIdAndEstadoAndFechaInicioAfter(usuarioId, EstadoEvento.PUBLICADO, ahora)) {
            rechazarDesactivacion(usuarioId, EVENTOS_PUBLICADOS);
        }

        cancelarInscripcionesPendientes(usuarioId, ahora);
        usuario.setActivo(false);
        usuarioRepository.save(usuario);
        logService.registrarExito(usuarioId, LogService.ACCION_DESACTIVAR_CUENTA, LogService.ENTIDAD_USUARIO, usuarioId);
    }

    /** Pasa a CANCELADA cada inscripción activa en un evento por empezar y le devuelve el cupo al evento */
    private void cancelarInscripcionesPendientes(Long usuarioId, LocalDateTime ahora) {
        for (Inscripcion inscripcion : inscripcionRepository.findByUsuarioIdAndEstadoAndEventoFechaInicioAfter(
                usuarioId, EstadoInscripcion.ACTIVA, ahora)) {
            // Bloquea la fila del evento, igual que al cancelar una inscripción (HU-12), para no perder un cupo con inscripciones simultáneas
            Evento evento = eventoRepository.buscarPorIdConBloqueo(inscripcion.getEvento().getId())
                    .orElseThrow(() -> new RecursoNoEncontradoException(Mensajes.EVENTO_NO_ENCONTRADO));
            evento.setCuposDisponibles(evento.getCuposDisponibles() + 1);
            eventoRepository.save(evento);
            inscripcion.setEstado(EstadoInscripcion.CANCELADA);
            inscripcionRepository.save(inscripcion);
            logService.registrarExito(usuarioId, LogService.ACCION_CANCELAR_INSCRIPCION, LogService.ENTIDAD_INSCRIPCION, inscripcion.getId());
        }
    }

    /** Confirma que quien hace la operación conoce la contraseña; si no, registra el intento y la rechaza */
    private void exigirContrasena(Usuario usuario, String password, String accion) {
        if (!passwordEncoder.matches(password, usuario.getPasswordHash())) {
            logService.registrarFallo(usuario.getId(), accion, LogService.ENTIDAD_USUARIO, usuario.getId(), "Contraseña incorrecta");
            throw new SolicitudInvalidaException(Mensajes.CONTRASENA_INCORRECTA);
        }
    }

    private void rechazarDesactivacion(Long usuarioId, String motivo) {
        logService.registrarFallo(usuarioId, LogService.ACCION_DESACTIVAR_CUENTA, LogService.ENTIDAD_USUARIO, usuarioId, motivo);
        throw new ConflictoException(motivo);
    }

    private Usuario buscarUsuario(Long usuarioId) {
        return usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new RecursoNoEncontradoException(Mensajes.USUARIO_NO_ENCONTRADO));
    }
}
