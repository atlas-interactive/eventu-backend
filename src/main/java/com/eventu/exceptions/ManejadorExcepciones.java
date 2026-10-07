package com.eventu.exceptions;

import com.eventu.util.Mensajes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.ErrorResponse;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.util.Map;

/**
 * Punto único donde las excepciones se convierten en respuestas HTTP.
 * Todas las respuestas de error tienen la forma {@code {"error": "mensaje"}}
 */
@RestControllerAdvice
public class ManejadorExcepciones {

    private static final Logger LOG = LoggerFactory.getLogger(ManejadorExcepciones.class);

    @ExceptionHandler(org.springframework.security.access.AccessDeniedException.class)
    public ResponseEntity<Map<String, String>> manejarAccesoDenegadoSpring(
            org.springframework.security.access.AccessDeniedException ex) {
        return responder(HttpStatus.FORBIDDEN, "No tiene permisos para esta acción.");
    }

    @ExceptionHandler(RecursoNoEncontradoException.class)
    public ResponseEntity<Map<String, String>> manejarNoEncontrado(RecursoNoEncontradoException ex) {
        return responder(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    @ExceptionHandler(ConflictoException.class)
    public ResponseEntity<Map<String, String>> manejarConflicto(ConflictoException ex) {
        return responder(HttpStatus.CONFLICT, ex.getMessage());
    }

    @ExceptionHandler(AccesoDenegadoException.class)
    public ResponseEntity<Map<String, String>> manejarAccesoDenegado(AccesoDenegadoException ex) {
        return responder(HttpStatus.FORBIDDEN, ex.getMessage());
    }

    @ExceptionHandler(CredencialesInvalidasException.class)
    public ResponseEntity<Map<String, String>> manejarCredencialesInvalidas(CredencialesInvalidasException ex) {
        return responder(HttpStatus.UNAUTHORIZED, ex.getMessage());
    }

    @ExceptionHandler(SolicitudInvalidaException.class)
    public ResponseEntity<Map<String, String>> manejarSolicitudInvalida(SolicitudInvalidaException ex) {
        return responder(HttpStatus.BAD_REQUEST, ex.getMessage());
    }

    /** Errores de las anotaciones de validación (@NotBlank, @Size, etc.) de los DTO */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, String>> manejarValidacion(MethodArgumentNotValidException ex) {
        String mensaje = ex.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .sorted()
                .findFirst()
                .orElse("Los datos enviados no son válidos.");
        return responder(HttpStatus.BAD_REQUEST, mensaje);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Map<String, String>> manejarCuerpoInvalido(HttpMessageNotReadableException ex) {
        return responder(HttpStatus.BAD_REQUEST, "El cuerpo de la petición no es válido.");
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<Map<String, String>> manejarParametroFaltante(MissingServletRequestParameterException ex) {
        return responder(HttpStatus.BAD_REQUEST, "Falta el parámetro obligatorio: " + ex.getParameterName() + ".");
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<Map<String, String>> manejarTipoInvalido(MethodArgumentTypeMismatchException ex) {
        return responder(HttpStatus.BAD_REQUEST, "El valor del parámetro '" + ex.getName() + "' no es válido.");
    }

    /** Red de seguridad: restricciones de la base de datos (por ejemplo, dos inscripciones simultáneas) */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<Map<String, String>> manejarIntegridadDatos(DataIntegrityViolationException ex) {
        LOG.warn("Restricción de base de datos violada", ex);
        return responder(HttpStatus.CONFLICT, "La operación no se pudo completar porque ya existe un registro igual o un dato no es válido.");
    }

    /**
     * Última red de seguridad. Los errores propios de Spring MVC (ruta inexistente, método no permitido,
     * tipo de contenido no soportado) conservan su código HTTP en lugar de convertirse en un 500
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, String>> manejarErrorInesperado(Exception ex) {
        if (ex instanceof ErrorResponse errorHttp) {
            int codigo = errorHttp.getStatusCode().value();
            return ResponseEntity.status(codigo).body(Map.of("error", mensajeParaCodigo(codigo)));
        }
        LOG.error("Error no controlado", ex);
        return responder(HttpStatus.INTERNAL_SERVER_ERROR, Mensajes.ERROR_INTERNO);
    }

    private static String mensajeParaCodigo(int codigo) {
        return switch (codigo) {
            case 404 -> "El recurso solicitado no existe.";
            case 405 -> "El método HTTP no está permitido para esta ruta.";
            case 415 -> "El tipo de contenido de la petición no es compatible.";
            default -> codigo >= 500 ? Mensajes.ERROR_INTERNO : "La petición no es válida.";
        };
    }

    private static ResponseEntity<Map<String, String>> responder(HttpStatus estado, String mensaje) {
        String texto = mensaje == null ? Mensajes.ERROR_INTERNO : mensaje;
        return ResponseEntity.status(estado).body(Map.of("error", texto));
    }
}
