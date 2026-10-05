package com.eventu.services;

import com.eventu.exceptions.AccesoDenegadoException;
import com.eventu.exceptions.RecursoNoEncontradoException;
import com.eventu.models.Evento;
import com.eventu.models.Rol;
import com.eventu.models.Usuario;
import com.eventu.repositories.UsuarioRepository;
import com.eventu.util.Mensajes;
import org.springframework.stereotype.Service;

/**
 * Reglas de permisos que comparten varios servicios.
 * Por ahora el usuario llega como parámetro de la petición; cuando exista el JWT habrá que cambiar de dónde se obtiene su id.
 */
@Service
public class AutorizacionService {

    private final UsuarioRepository usuarioRepository;

    public AutorizacionService(UsuarioRepository usuarioRepository) {
        this.usuarioRepository = usuarioRepository;
    }

    /**
     * Busca un usuario existente
     *
     * @throws RecursoNoEncontradoException si el usuario no existe
     */
    public Usuario obtenerUsuario(Long usuarioId) {
        return usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new RecursoNoEncontradoException(Mensajes.USUARIO_NO_ENCONTRADO));
    }

    /**
     * Busca un usuario que pueda gestionar eventos
     * @throws AccesoDenegadoException si su rol no permite gestionar eventos
     */
    public Usuario obtenerGestorDeEventos(Long usuarioId) {
        Usuario usuario = obtenerUsuario(usuarioId);
        if (usuario.getRol() != Rol.ORGANIZADOR && usuario.getRol() != Rol.ADMIN) {
            throw new AccesoDenegadoException(Mensajes.SIN_PERMISO_GESTIONAR_EVENTOS);
        }
        return usuario;
    }

    /** solo el organizador que creó el evento puede modificarlo; el administrador puede con todos */
    public boolean puedeModificarEvento(Usuario gestor, Evento evento) {
        return gestor.getRol() == Rol.ADMIN || evento.getOrganizador().getId().equals(gestor.getId());
    }
}
