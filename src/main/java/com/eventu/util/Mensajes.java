package com.eventu.util;

/**
 * Mensajes de error que se repiten en varias clases del backend
 * Los textos de uso único se escriben directamente donde se usan
 */
public final class Mensajes {

    private Mensajes() {
    }

    public static final String CORREO_INSTITUCIONAL = "Debe ingresar su correo institucional (@unillanos.edu.co).";
    public static final String USUARIO_NO_ENCONTRADO = "Usuario no encontrado.";
    public static final String EVENTO_NO_ENCONTRADO = "Evento no encontrado.";
    public static final String CATEGORIA_NO_ENCONTRADA = "Categoría no encontrada.";
    public static final String INSCRIPCION_NO_ENCONTRADA = "Inscripción no encontrada.";
    public static final String SIN_PERMISO_GESTIONAR_EVENTOS = "No tienes permisos para gestionar eventos.";
    public static final String EVENTO_AJENO = "Solo el organizador que creó el evento puede realizar esta acción.";
    public static final String CATEGORIA_INACTIVA = "La categoría seleccionada no está activa.";
    public static final String ERROR_INTERNO = "Ocurrió un error inesperado. Intente nuevamente.";
}
