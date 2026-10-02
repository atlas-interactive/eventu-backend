package com.eventu.exceptions;

/**
 * El recurso solicitado no existe.
 * Se convierte en una respuesta HTTP 404 en {@link ManejadorExcepciones}
 */
public class RecursoNoEncontradoException extends RuntimeException {

    public RecursoNoEncontradoException(String mensaje) {
        super(mensaje);
    }
}
