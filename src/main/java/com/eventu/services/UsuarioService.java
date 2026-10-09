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