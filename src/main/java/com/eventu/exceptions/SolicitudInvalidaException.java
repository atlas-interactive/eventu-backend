package com.eventu.exceptions;

/**
 * Los datos enviados no son válidos.
 * Se convierte en una respuesta HTTP 400 en {@link ManejadorExcepciones}
 */
public class SolicitudInvalidaException extends RuntimeException {

    public SolicitudInvalidaException(String mensaje) {
        super(mensaje);
    }
}
