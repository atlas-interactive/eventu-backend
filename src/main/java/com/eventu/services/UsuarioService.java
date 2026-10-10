package com.eventu.services;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.eventu.dto.UsuarioResponseDTO;
import com.eventu.exceptions.ConflictoException;
import com.eventu.exceptions.RecursoNoEncontradoException;
import com.eventu.exceptions.SolicitudInvalidaException;
import com.eventu.models.Rol;
import com.eventu.models.Usuario;
import com.eventu.repositories.UsuarioRepository;

/** Gestión del rol de organizador por parte de un administrador */
@Service
public class UsuarioService {

    private final UsuarioRepository usuarioRepository;
    private final AutorizacionService autorizacionService;
    private final LogService logService;

    public UsuarioService(UsuarioRepository usuarioRepository, AutorizacionService autorizacionService, LogService logService) {
        this.usuarioRepository = usuarioRepository;
        this.autorizacionService = autorizacionService;
        this.logService = logService;
    }

    /**
     * Busca usuarios por coincidencia parcial en el nombre o en el correo, sin distinguir mayúsculas
     *
     * @param criterio texto a buscar
     * @return usuarios que coinciden
     * @throws SolicitudInvalidaException si el criterio está vacío
     * @throws RecursoNoEncontradoException si ningún usuario coincide
     */
    @Transactional(readOnly = true)
    public List<UsuarioResponseDTO> buscarUsuarios(String criterio) {
        if (criterio == null || criterio.isBlank()) {
            throw new SolicitudInvalidaException("Ingrese un correo o un nombre para buscar.");
        }
        String texto = criterio.trim();
        List<UsuarioResponseDTO> encontrados = usuarioRepository
                .findByCorreoContainingIgnoreCaseOrNombreContainingIgnoreCase(texto, texto).stream()
                .map(UsuarioResponseDTO::desde)
                .toList();
        if (encontrados.isEmpty()) {
            throw new RecursoNoEncontradoException("No se encontró ningún usuario con ese criterio.");
        }
        return encontrados;
    }

    /**
     * Lista a los usuarios que hoy tienen el rol de organizador, ordenados por nombre
     *
     * @return organizadores actuales; lista vacía si no hay ninguno
     */
    @Transactional(readOnly = true)
    public List<UsuarioResponseDTO> listarOrganizadores() {
        return usuarioRepository.findByRolOrderByNombreAsc(Rol.ORGANIZADOR).stream()
                .map(UsuarioResponseDTO::desde)
                .toList();
    }

    /**
     * Asigna el rol de organizador a un usuario con rol USUARIO
     *
     * @param usuarioId usuario que recibirá el rol
     * @param administradorId administrador que hace el cambio
     * @return el usuario con su nuevo rol
     * @throws RecursoNoEncontradoException si el usuario no existe
     * @throws ConflictoException si el usuario ya es organizador o es administrador
     */
    @Transactional
    public UsuarioResponseDTO asignarOrganizador(Long usuarioId, Long administradorId) {
        Usuario usuario = obtenerParaCambioDeRol(usuarioId, administradorId, LogService.ACCION_ASIGNAR_ORGANIZADOR);
        if (usuario.getRol() != Rol.USUARIO) {
            String motivo = usuario.getRol() == Rol.ORGANIZADOR
                    ? "El usuario ya es organizador."
                    : "Un administrador no puede pasar a organizador.";
            throw rechazar(administradorId, LogService.ACCION_ASIGNAR_ORGANIZADOR, usuarioId, motivo);
        }
        return cambiarRol(usuario, Rol.ORGANIZADOR, administradorId, LogService.ACCION_ASIGNAR_ORGANIZADOR);
    }

    /**
     * Revoca el rol de organizador: el usuario conserva su cuenta y sus eventos
     *
     * @param usuarioId usuario al que se le quita el rol
     * @param administradorId administrador que hace el cambio
     * @return el usuario con su nuevo rol
     * @throws RecursoNoEncontradoException si el usuario no existe
     * @throws ConflictoException si el usuario no es organizador
     */
    @Transactional
    public UsuarioResponseDTO revocarOrganizador(Long usuarioId, Long administradorId) {
        Usuario usuario = obtenerParaCambioDeRol(usuarioId, administradorId, LogService.ACCION_REVOCAR_ORGANIZADOR);
        if (usuario.getRol() != Rol.ORGANIZADOR) {
            throw rechazar(administradorId, LogService.ACCION_REVOCAR_ORGANIZADOR, usuarioId, "El usuario no es organizador.");
        }
        return cambiarRol(usuario, Rol.USUARIO, administradorId, LogService.ACCION_REVOCAR_ORGANIZADOR);
    }

    private Usuario obtenerParaCambioDeRol(Long usuarioId, Long administradorId, String accion) {
        autorizacionService.obtenerAdministrador(administradorId);
        return usuarioRepository.findById(usuarioId).orElseThrow(() -> {
            logService.registrarFallo(administradorId, accion, LogService.ENTIDAD_USUARIO, usuarioId, "Usuario no encontrado");
            return new RecursoNoEncontradoException("Usuario no encontrado.");
        });
    }

    private UsuarioResponseDTO cambiarRol(Usuario usuario, Rol nuevoRol, Long administradorId, String accion) {
        usuario.setRol(nuevoRol);
        Usuario guardado = usuarioRepository.save(usuario);
        logService.registrarExito(administradorId, accion, LogService.ENTIDAD_USUARIO, guardado.getId());
        return UsuarioResponseDTO.desde(guardado);
    }

    private ConflictoException rechazar(Long administradorId, String accion, Long usuarioId, String motivo) {
        logService.registrarFallo(administradorId, accion, LogService.ENTIDAD_USUARIO, usuarioId, motivo);
        return new ConflictoException(motivo);
    }
}
