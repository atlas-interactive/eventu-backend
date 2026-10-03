package com.eventu.exceptions;

/**
 * La operación choca con el estado actual de los datos (regla de negocio).
 * Se convierte en una respuesta HTTP 409 en {@link ManejadorExcepciones}
 */
public class ConflictoException extends RuntimeException {

    public ConflictoException(String mensaje) {
        super(mensaje);
    }
}
