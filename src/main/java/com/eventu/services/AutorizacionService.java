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
 * actual el usuario llega como parámetro de la petición; cuando exista el JWT --> cambiar de dónde se obtiene su id
 */
@Service
public class AutorizacionService {

    private final UsuarioRepository usuarioRepository;

    public AutorizacionService(UsuarioRepository usuarioRepository) {
        this.usuarioRepository = usuarioRepository;
    }

    /**
     * Busca un usuario existente y con la cuenta habilitada
     *
     * @throws RecursoNoEncontradoException si el usuario no existe
     * @throws AccesoDenegadoException si la cuenta está deshabilitada
     */
    public Usuario obtenerUsuarioActivo(Long usuarioId) {
        Usuario usuario = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new RecursoNoEncontradoException(Mensajes.USUARIO_NO_ENCONTRADO));
        if (!Boolean.TRUE.equals(usuario.getActivo())) {
            throw new AccesoDenegadoException(Mensajes.CUENTA_DESHABILITADA);
        }
        return usuario;
    }

    /**
     * Busca un usuario activo que pueda gestionar eventos
     * @throws AccesoDenegadoException si su rol no permite gestionar eventos
     */
    public Usuario obtenerGestorDeEventos(Long usuarioId) {
        Usuario usuario = obtenerUsuarioActivo(usuarioId);
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
