package com.eventu.security;

import com.eventu.models.Rol;

public record UsuarioAutenticado(Long id, String correo, Rol rol) { }