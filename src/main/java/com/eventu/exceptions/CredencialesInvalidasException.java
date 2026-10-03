package com.eventu.exceptions;

/**
 * Las credenciales de inicio de sesión no son correctas.
 * Se convierte en una respuesta HTTP 401 en {@link ManejadorExcepciones}
 */
public class CredencialesInvalidasException extends RuntimeException {

    public CredencialesInvalidasException(String mensaje) {
        super(mensaje);
    }
}
