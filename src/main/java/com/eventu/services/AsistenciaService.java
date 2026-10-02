package com.eventu.services;

import com.eventu.exceptions.AccesoDenegadoException;
import com.eventu.exceptions.ConflictoException;
import com.eventu.exceptions.RecursoNoEncontradoException;
import com.eventu.models.Asistencia;
import com.eventu.models.EstadoEvento;
import com.eventu.models.EstadoInscripcion;
import com.eventu.models.Evento;
import com.eventu.models.Inscripcion;
import com.eventu.models.Usuario;
import com.eventu.repositories.AsistenciaRepository;
import com.eventu.repositories.InscripcionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

/** Validación de códigos QR y registro de asistencia */
@Service
public class AsistenciaService {

    private final AsistenciaRepository asistenciaRepository;
    private final InscripcionRepository inscripcionRepository;
    private final AutorizacionService autorizacionService;
    private final LogService logService;

    public AsistenciaService(AsistenciaRepository asistenciaRepository, InscripcionRepository inscripcionRepository,
                             AutorizacionService autorizacionService, LogService logService) {
        this.asistenciaRepository = asistenciaRepository;
        this.inscripcionRepository = inscripcionRepository;
        this.autorizacionService = autorizacionService;
        this.logService = logService;
    }

    /**
     * Valida un código QR y registra la asistencia de su inscripción.
     * Solo puede hacerlo el organizador del evento (o un administrador).
     *
     * @param codigoQr código leído del QR del usuario
     * @param organizadorId usuario que valida
     * @return mensaje, asistenciaId, usuario, evento y fechaRegistro (se arma dentro de la transacción porque lee relaciones perezosas)
     * @throws AccesoDenegadoException si el usuario no gestiona eventos o el evento no es suyo
     * @throws RecursoNoEncontradoException si el código no corresponde a ninguna inscripción
     * @throws ConflictoException si el evento o la inscripción están cancelados o la asistencia ya se registró 
     */
    @Transactional
    public Map<String, Object> registrarAsistenciaPorQr(String codigoQr, Long organizadorId) {
        Usuario gestor = autorizacionService.obtenerGestorDeEventos(organizadorId);
        Inscripcion inscripcion = inscripcionRepository.findByCodigoQr(codigoQr)
                .orElseThrow(() -> new RecursoNoEncontradoException("Código QR inválido o inscripción no encontrada."));
        Evento evento = inscripcion.getEvento();

        if (!autorizacionService.puedeModificarEvento(gestor, evento)) {
            throw rechazar(new AccesoDenegadoException("Solo el organizador del evento puede registrar asistencia."),
                    organizadorId, inscripcion, "Intento de validar un QR de un evento ajeno");
        }
        if (evento.getEstado() == EstadoEvento.CANCELADO) {
            throw rechazar(new ConflictoException("El evento está cancelado; no se puede registrar asistencia."),
                    organizadorId, inscripcion, "Evento cancelado");
        }
        if (inscripcion.getEstado() == EstadoInscripcion.CANCELADA) { // RN08
            throw rechazar(new ConflictoException("La inscripción se encuentra cancelada; no se puede registrar asistencia."),
                    organizadorId, inscripcion, "Inscripción cancelada");
        }
        if (asistenciaRepository.existsByInscripcionId(inscripcion.getId())) { // RN07
            throw rechazar(new ConflictoException("La asistencia de esta inscripción ya fue registrada."),
                    organizadorId, inscripcion, "Asistencia duplicada");
        }

        Asistencia asistencia = new Asistencia();
        asistencia.setInscripcion(inscripcion);
        Asistencia guardada = asistenciaRepository.save(asistencia);

        logService.registrarExito(organizadorId, LogService.ACCION_REGISTRAR_ASISTENCIA, LogService.ENTIDAD_ASISTENCIA, guardada.getId());
        return Map.<String, Object>of(
                "mensaje", "Asistencia registrada exitosamente.",
                "asistenciaId", guardada.getId(),
                "usuario", inscripcion.getUsuario().getNombre(),
                "evento", evento.getTitulo(),
                "fechaRegistro", guardada.getFechaRegistro());
    }

    /** Registra el intento rechazado en el log y devuelve la excepción para lanzarla */
    private <T extends RuntimeException> T rechazar(T excepcion, Long organizadorId, Inscripcion inscripcion, String motivo) {
        logService.registrarFallo(organizadorId, LogService.ACCION_REGISTRAR_ASISTENCIA, LogService.ENTIDAD_INSCRIPCION, inscripcion.getId(), motivo);
        return excepcion;
    }
}
