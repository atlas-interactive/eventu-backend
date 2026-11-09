package com.eventu.exceptions;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.BindingResult;
import org.springframework.web.ErrorResponseException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.NoHandlerFoundException;

import com.eventu.dto.RegistroRequestDTO;
import com.eventu.util.Mensajes;

/** Conversión de cada excepción en la respuesta {"error": "mensaje"} con su código HTTP */
class ManejadorExcepcionesTest {

    private final ManejadorExcepciones manejador = new ManejadorExcepciones();

    @Test
    void excepcionesDeNegocio_conservanSuMensajeYSuCodigo() {
        comprobar(manejador.manejarNoEncontrado(new RecursoNoEncontradoException("Evento no encontrado.")),
                HttpStatus.NOT_FOUND, "Evento no encontrado.");
        comprobar(manejador.manejarConflicto(new ConflictoException("El correo ya se encuentra registrado.")),
                HttpStatus.CONFLICT, "El correo ya se encuentra registrado.");
        comprobar(manejador.manejarAccesoDenegado(new AccesoDenegadoException(Mensajes.SOLO_ADMINISTRADOR)),
                HttpStatus.FORBIDDEN, Mensajes.SOLO_ADMINISTRADOR);
        comprobar(manejador.manejarCredencialesInvalidas(new CredencialesInvalidasException("Correo o contraseña incorrectos.")),
                HttpStatus.UNAUTHORIZED, "Correo o contraseña incorrectos.");
        comprobar(manejador.manejarSolicitudInvalida(new SolicitudInvalidaException("La fecha de inicio debe ser futura.")),
                HttpStatus.BAD_REQUEST, "La fecha de inicio debe ser futura.");
    }

    @Test
    void accesoDenegadoPorRol_respondeProhibidoConElMensajeGeneral() {
        comprobar(manejador.manejarAccesoDenegadoSpring(new AccessDeniedException("Access Denied")),
                HttpStatus.FORBIDDEN, Mensajes.SIN_PERMISO_ACCION);
    }

    @Test
    void excepcionSinMensaje_respondeConElMensajeDeErrorInterno() {
        comprobar(manejador.manejarConflicto(new ConflictoException(null)), HttpStatus.CONFLICT, Mensajes.ERROR_INTERNO);
    }

    @Test
    void validacionDelDto_respondeElPrimerMensajeEnOrdenAlfabetico() {
        BindingResult errores = new BeanPropertyBindingResult(new RegistroRequestDTO(), "registro");
        errores.rejectValue("password", "Size", Mensajes.LONGITUD_CONTRASENA);
        errores.rejectValue("nombre", "NotBlank", "El nombre es obligatorio.");

        comprobar(manejador.manejarValidacion(new MethodArgumentNotValidException(null, errores)),
                HttpStatus.BAD_REQUEST, "El nombre es obligatorio.");
    }

    @Test
    void validacionSinErroresDeCampo_respondeUnMensajeGeneral() {
        BindingResult errores = new BeanPropertyBindingResult(new RegistroRequestDTO(), "registro");
        errores.reject("Global", "Error global");

        comprobar(manejador.manejarValidacion(new MethodArgumentNotValidException(null, errores)),
                HttpStatus.BAD_REQUEST, "Los datos enviados no son válidos.");
    }

    @Test
    void erroresDeLaPeticion_respondenSolicitudInvalidaConMensajeEnEspanol() {
        comprobar(manejador.manejarCuerpoInvalido(mock(HttpMessageNotReadableException.class)),
                HttpStatus.BAD_REQUEST, "El cuerpo de la petición no es válido.");
        comprobar(manejador.manejarParametroFaltante(new MissingServletRequestParameterException("organizadorId", "Long")),
                HttpStatus.BAD_REQUEST, "Falta el parámetro obligatorio: organizadorId.");

        MethodArgumentTypeMismatchException tipoInvalido = mock(MethodArgumentTypeMismatchException.class);
        when(tipoInvalido.getName()).thenReturn("id");
        comprobar(manejador.manejarTipoInvalido(tipoInvalido), HttpStatus.BAD_REQUEST, "El valor del parámetro 'id' no es válido.");
    }

    @Test
    void restriccionDeLaBaseDeDatos_respondeConflicto() {
        comprobar(manejador.manejarIntegridadDatos(new DataIntegrityViolationException("uk_inscripcion")),
                HttpStatus.CONFLICT, "La operación no se pudo completar porque ya existe un registro igual o un dato no es válido.");
    }

    @Test
    void erroresDeSpringMvc_conservanSuCodigoConMensajeEnEspanol() {
        comprobar(manejador.manejarErrorInesperado(new NoHandlerFoundException("GET", "/api/no-existe", new HttpHeaders())),
                HttpStatus.NOT_FOUND, "El recurso solicitado no existe.");
        comprobar(manejador.manejarErrorInesperado(new HttpRequestMethodNotSupportedException("DELETE")),
                HttpStatus.METHOD_NOT_ALLOWED, "El método HTTP no está permitido para esta ruta.");
        comprobar(manejador.manejarErrorInesperado(new HttpMediaTypeNotSupportedException("text/plain")),
                HttpStatus.UNSUPPORTED_MEDIA_TYPE, "El tipo de contenido de la petición no es compatible.");
        comprobar(manejador.manejarErrorInesperado(new ErrorResponseException(HttpStatus.NOT_ACCEPTABLE)),
                HttpStatus.NOT_ACCEPTABLE, "La petición no es válida.");
        comprobar(manejador.manejarErrorInesperado(new ErrorResponseException(HttpStatus.SERVICE_UNAVAILABLE)),
                HttpStatus.SERVICE_UNAVAILABLE, Mensajes.ERROR_INTERNO);
    }

    @Test
    void errorNoControlado_respondeErrorInternoSinRevelarElDetalle() {
        comprobar(manejador.manejarErrorInesperado(new IllegalStateException("detalle interno")),
                HttpStatus.INTERNAL_SERVER_ERROR, Mensajes.ERROR_INTERNO);
    }

    private void comprobar(ResponseEntity<Map<String, String>> respuesta, HttpStatus estado, String mensaje) {
        assertEquals(estado.value(), respuesta.getStatusCode().value());
        assertEquals(Map.of("error", mensaje), respuesta.getBody());
    }
}
