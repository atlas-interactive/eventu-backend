package com.eventu.services;

import com.eventu.exceptions.AccesoDenegadoException;
import com.eventu.exceptions.RecursoNoEncontradoException;
import com.eventu.models.Evento;
import com.eventu.models.Rol;
import com.eventu.models.Usuario;
import com.eventu.repositories.UsuarioRepository;
import com.eventu.util.Mensajes;
import org.springframework.stereotype.Service;

/** Verificaciones de rol y de propiedad que comparten varios servicios */
@Service
public class AutorizacionService {

    private final UsuarioRepository usuarioRepository;

    public AutorizacionService(UsuarioRepository usuarioRepository) {
        this.usuarioRepository = usuarioRepository;
    }

    /**
     * Busca un usuario por su id
     *
     * @throws RecursoNoEncontradoException si el usuario no existe
     */
    public Usuario obtenerUsuario(Long usuarioId) {
        return usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new RecursoNoEncontradoException(Mensajes.USUARIO_NO_ENCONTRADO));
    }

    /**
     * Devuelve al usuario solo si puede gestionar eventos (organizador o administrador)
     *
     * @throws AccesoDenegadoException si su rol es USUARIO
     */
    public Usuario obtenerGestorDeEventos(Long usuarioId) {
        Usuario usuario = obtenerUsuario(usuarioId);
        if (usuario.getRol() != Rol.ORGANIZADOR && usuario.getRol() != Rol.ADMIN) {
            throw new AccesoDenegadoException(Mensajes.SIN_PERMISO_GESTIONAR_EVENTOS);
        }
        return usuario;
    }

    /**
     * Devuelve al usuario solo si es administrador
     *
     * @throws AccesoDenegadoException si su rol no es ADMIN
     */
    public Usuario obtenerAdministrador(Long usuarioId) {
        Usuario usuario = obtenerUsuario(usuarioId);
        if (usuario.getRol() != Rol.ADMIN) {
            throw new AccesoDenegadoException(Mensajes.SOLO_ADMINISTRADOR);
        }
        return usuario;
    }

    /** Un administrador puede modificar cualquier evento; un organizador, solo los suyos */
    public boolean puedeModificarEvento(Usuario gestor, Evento evento) {
        return gestor.getRol() == Rol.ADMIN || evento.getOrganizador().getId().equals(gestor.getId());
    }
}
