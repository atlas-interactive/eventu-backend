package com.eventu.services;

import org.springframework.stereotype.Service;

import com.eventu.exceptions.AccesoDenegadoException;
import com.eventu.exceptions.RecursoNoEncontradoException;
import com.eventu.models.Evento;
import com.eventu.models.Rol;
import com.eventu.models.Usuario;
import com.eventu.repositories.UsuarioRepository;
import com.eventu.util.Mensajes;

@Service
public class AutorizacionService {

    private final UsuarioRepository usuarioRepository;

    public AutorizacionService(UsuarioRepository usuarioRepository) {
        this.usuarioRepository = usuarioRepository;
    }

    public Usuario obtenerUsuario(Long usuarioId) {
        return usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new RecursoNoEncontradoException(Mensajes.USUARIO_NO_ENCONTRADO));
    }

    public Usuario obtenerGestorDeEventos(Long usuarioId) {
        Usuario usuario = obtenerUsuario(usuarioId);
        if (usuario.getRol() != Rol.ORGANIZADOR && usuario.getRol() != Rol.ADMIN) {
            throw new AccesoDenegadoException(Mensajes.SIN_PERMISO_GESTIONAR_EVENTOS);
        }
        return usuario;
    }

    public Usuario obtenerAdministrador(Long usuarioId) {
        Usuario usuario = obtenerUsuario(usuarioId);
        if (usuario.getRol() != Rol.ADMIN) {
            throw new AccesoDenegadoException(Mensajes.SOLO_ADMINISTRADOR);
        }
        return usuario;
    }

    public boolean puedeModificarEvento(Usuario gestor, Evento evento) {
        return gestor.getRol() == Rol.ADMIN || evento.getOrganizador().getId().equals(gestor.getId());
    }
}
