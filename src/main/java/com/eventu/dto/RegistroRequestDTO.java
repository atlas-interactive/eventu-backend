package com.eventu.dto;

import com.eventu.util.Mensajes;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** Datos para registrar una cuenta nueva*/
public class RegistroRequestDTO {

    /** Solo correos del dominio institucional, sin distinguir mayúsculas */
    public static final String REGEX_CORREO_INSTITUCIONAL = "(?i)^[a-z0-9._%+-]+@unillanos\\.edu\\.co$";

    @NotBlank(message = "El nombre es obligatorio.")
    @Size(max = 100, message = "El nombre no puede superar los 100 caracteres.")
    private String nombre;

    @NotBlank(message = "El correo es obligatorio.")
    @Size(max = 100, message = "El correo no puede superar los 100 caracteres.")
    @Pattern(regexp = REGEX_CORREO_INSTITUCIONAL, message = Mensajes.CORREO_INSTITUCIONAL)
    private String correo;

    // BCrypt solo procesa los primeros 72 bytes, por eso ese es el máximo
    @NotBlank(message = "La contraseña es obligatoria.")
    @Size(min = 8, max = 72, message = "La contraseña debe tener entre 8 y 72 caracteres.")
    private String password;

    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }
    public String getCorreo() { return correo; }
    public void setCorreo(String correo) { this.correo = correo; }
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
}
