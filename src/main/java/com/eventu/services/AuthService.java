package com.eventu.services;

import com.eventu.dto.RegistroRequestDTO;
import com.eventu.exceptions.AccesoDenegadoException;
import com.eventu.exceptions.ConflictoException;
import com.eventu.exceptions.CredencialesInvalidasException;
import com.eventu.models.Rol;
import com.eventu.models.Usuario;
import com.eventu.repositories.UsuarioRepository;
import com.eventu.util.Mensajes;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Registro e inicio de sesión de usuarios */
@Service
public class AuthService {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final LogService logService;

    public AuthService(UsuarioRepository usuarioRepository, PasswordEncoder passwordEncoder, LogService logService) {
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
        this.logService = logService;
    }

    /**
     * Registra una cuenta nueva. Toda cuenta nace con rol ESTUDIANTE (usuario normal); el rol de organizador lo asigna después un administrador (HU-03).
     * @param request datos ya validados (correo institucional, contraseña de 8 a 72 caracteres)
     * @return el usuario guardado
     * @throws ConflictoException si el correo ya está registrado
     */
    @Transactional
    public Usuario registrarUsuario(RegistroRequestDTO request) {
        String correo = normalizarCorreo(request.getCorreo());
        if (usuarioRepository.existsByCorreoIgnoreCase(correo)) {
            throw new ConflictoException("El correo ya se encuentra registrado.");
        }

        Usuario nuevoUsuario = new Usuario();
        nuevoUsuario.setNombre(request.getNombre().trim());
        nuevoUsuario.setCorreo(correo);
        nuevoUsuario.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        nuevoUsuario.setRol(Rol.ESTUDIANTE);
        Usuario guardado = usuarioRepository.save(nuevoUsuario);

        logService.registrarExito(guardado.getId(), LogService.ACCION_REGISTRO, LogService.ENTIDAD_USUARIO, guardado.getId());
        return guardado;
    }

    /**
     * Valida las credenciales de un usuario.
     *
     * @throws CredencialesInvalidasException si el correo o la contraseña no coinciden
     * @throws AccesoDenegadoException si la cuenta está deshabilitada
     */
    public Usuario autenticar(String correo, String password) {
        Usuario usuario = usuarioRepository.findByCorreoIgnoreCase(normalizarCorreo(correo)).orElse(null);
        Long usuarioId = usuario == null ? null : usuario.getId();

        // Mismo mensaje para correo inexistente y contraseña errónea, para no revelar qué cuentas existen
        if (usuario == null || !passwordEncoder.matches(password, usuario.getPasswordHash())) {
            logService.registrarFallo(usuarioId, LogService.ACCION_LOGIN, LogService.ENTIDAD_USUARIO, usuarioId,
                    "Credenciales inválidas");
            throw new CredencialesInvalidasException("Correo o contraseña incorrectos.");
        }
        if (!Boolean.TRUE.equals(usuario.getActivo())) {
            logService.registrarFallo(usuarioId, LogService.ACCION_LOGIN, LogService.ENTIDAD_USUARIO, usuarioId,
                    "Cuenta deshabilitada");
            throw new AccesoDenegadoException(Mensajes.CUENTA_DESHABILITADA);
        }

        logService.registrarExito(usuarioId, LogService.ACCION_LOGIN, LogService.ENTIDAD_USUARIO, usuarioId);
        return usuario;
    }

    private String normalizarCorreo(String correo) {
        return correo.trim().toLowerCase();
    }
}
