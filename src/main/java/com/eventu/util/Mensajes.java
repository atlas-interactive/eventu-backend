package com.eventu.util;

/**
 * Mensajes de error que se repiten en varias clases del backend.
 * Los textos de uso único se escriben directamente donde se usan
 */
public final class Mensajes {

    public static final String CORREO_INSTITUCIONAL = "Debe ingresar su correo institucional (@unillanos.edu.co).";
    public static final String USUARIO_NO_ENCONTRADO = "Usuario no encontrado.";
    public static final String EVENTO_NO_ENCONTRADO = "Evento no encontrado.";
    public static final String CATEGORIA_NO_ENCONTRADA = "Categoría no encontrada.";
    public static final String INSCRIPCION_NO_ENCONTRADA = "Inscripción no encontrada.";
    public static final String RANGO_FECHAS_INVALIDO = "La fecha inicial no puede ser posterior a la fecha final.";
    public static final String SIN_PERMISO_GESTIONAR_EVENTOS = "No tienes permisos para gestionar eventos.";
    public static final String SIN_PERMISO_ACCION = "No tiene permisos para esta acción.";
    public static final String SOLO_ADMINISTRADOR = "Solo un administrador puede realizar esta acción.";
    public static final String EVENTO_AJENO = "Solo el organizador que creó el evento puede realizar esta acción.";
    public static final String CATEGORIA_INACTIVA = "La categoría seleccionada no está activa.";
    public static final String LONGITUD_CONTRASENA = "La contraseña debe tener entre 8 y 72 caracteres.";
    public static final String CONTRASENA_INCORRECTA = "La contraseña ingresada no es correcta.";
    public static final String REVISA_BANDEJA_Y_SPAM = "Revisa tu bandeja de entrada y, si no lo ves, la carpeta de spam.";
    public static final String ERROR_INTERNO = "Ocurrió un error inesperado. Intente nuevamente.";

    /** Clase de utilidad: no se instancia */
    private Mensajes() {
    }
}
