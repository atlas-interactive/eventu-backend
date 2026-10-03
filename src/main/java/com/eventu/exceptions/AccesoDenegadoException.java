package com.eventu.exceptions;

/**
 * El usuario no tiene permiso para realizar la operación.
 * Se convierte en una respuesta HTTP 403 en {@link ManejadorExcepciones}
 */
public class AccesoDenegadoException extends RuntimeException {

    public AccesoDenegadoException(String mensaje) {
        super(mensaje);
    }
}
