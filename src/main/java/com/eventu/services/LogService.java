package com.eventu.services;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import com.eventu.models.Log;
import com.eventu.repositories.LogRepository;

/**
 * Registra en la tabla "logs" las operaciones críticas del sistema:
 * fecha, usuario, acción, entidad afectada y resultado
 */
@Service
public class LogService {

    public static final String EXITO = "EXITO";
    public static final String FALLIDO = "FALLIDO";

    public static final String ACCION_REGISTRO = "REGISTRO";
    public static final String ACCION_LOGIN = "LOGIN";
    public static final String ACCION_CREAR_EVENTO = "CREAR_EVENTO";
    public static final String ACCION_EDITAR_EVENTO = "EDITAR_EVENTO";
    public static final String ACCION_INSCRIBIR = "INSCRIBIR";
    public static final String ACCION_CANCELAR_INSCRIPCION = "CANCELAR_INSCRIPCION";
    public static final String ACCION_REGISTRAR_ASISTENCIA = "REGISTRAR_ASISTENCIA";

    public static final String ENTIDAD_USUARIO = "USUARIO";
    public static final String ENTIDAD_EVENTO = "EVENTO";
    public static final String ENTIDAD_INSCRIPCION = "INSCRIPCION";
    public static final String ENTIDAD_ASISTENCIA = "ASISTENCIA";

    public static final String ACCION_CREAR_CATEGORIA = "CREAR_CATEGORIA";
    public static final String ACCION_EDITAR_CATEGORIA = "EDITAR_CATEGORIA";
    public static final String ACCION_ACTIVAR_CATEGORIA = "ACTIVAR_CATEGORIA";
    public static final String ACCION_DESACTIVAR_CATEGORIA = "DESACTIVAR_CATEGORIA";
    public static final String ENTIDAD_CATEGORIA = "CATEGORIA";

    public static final String ACCION_ASIGNAR_ORGANIZADOR = "ASIGNAR_ORGANIZADOR";
    public static final String ACCION_REVOCAR_ORGANIZADOR = "REVOCAR_ORGANIZADOR";
    
    private static final Logger LOG = LoggerFactory.getLogger(LogService.class);

    private final LogRepository logRepository;
    private final TransactionTemplate transaccionIndependiente;

    public LogService(LogRepository logRepository, PlatformTransactionManager transactionManager) {
        this.logRepository = logRepository;
        this.transaccionIndependiente = new TransactionTemplate(transactionManager);
        this.transaccionIndependiente.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    }

    /**
     * Registra una operación exitosa dentro de la misma transacción de la operación y el log solo queda guardado si la operación también se guarda
     */
    @Transactional
    public void registrarExito(Long usuarioId, String accion, String entidad, Long entidadId) {
        logRepository.save(construir(usuarioId, accion, entidad, entidadId, EXITO, null));
    }

    /**
     * Registra un intento rechazado en una transacción independiente, porque la transacción de la operación se revierte cuando se lanza la excepción.
     * Un error al guardar el log no interrumpe la respuesta al usuario
     */
    public void registrarFallo(Long usuarioId, String accion, String entidad, Long entidadId, String motivo) {
        try {
            transaccionIndependiente.executeWithoutResult(estado ->
                    logRepository.save(construir(usuarioId, accion, entidad, entidadId, FALLIDO, motivo)));
        } catch (RuntimeException e) {
            LOG.error("No se pudo registrar el log de la acción {}", accion, e);
        }
    }

    private Log construir(Long usuarioId, String accion, String entidad, Long entidadId, String resultado, String motivo) {
        Log log = new Log();
        log.setUsuarioId(usuarioId);
        log.setAccion(accion);
        log.setEntidad(entidad);
        log.setEntidadId(entidadId);
        log.setResultado(resultado);
        log.setMotivo(motivo);
        return log;
    }
}
