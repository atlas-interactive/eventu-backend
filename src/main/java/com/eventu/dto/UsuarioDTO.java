package com.eventu.dto;

public class RegistroRequestDTO {
    private String nombre;
    private String correo;
    private String password;

    public String getNombre() { return nombre; }
<<<<<<< Updated upstream
    public String getCorreo() { return correo; }
    public String getPassword() { return password; }

    public void setNombre(String nombre) { this.nombre = nombre; }
    public void setCorreo(String correo) { this.correo = correo; }
=======
    public void setNombre(String nombre) { this.nombre = nombre; }
    public String getCorreo() { return correo; }
    public void setCorreo(String correo) { this.correo = correo; }
    public String getPassword() { return password; }
>>>>>>> Stashed changes
    public void setPassword(String password) { this.password = password; }
}