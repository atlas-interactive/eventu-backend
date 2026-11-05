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

    static final String CORREO_SIN_CONFIRMAR = "Debes confirmar tu correo antes de iniciar sesión. " + Mensajes.REVISA_BANDEJA_Y_SPAM;
    static final String REGISTRO_SIN_CONFIRMAR = "Este correo ya está registrado, pero falta confirmarlo. "
            + Mensajes.REVISA_BANDEJA_Y_SPAM + " También puedes pedir un nuevo enlace.";

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final LogService logService;
    private final ConfirmacionCorreoService confirmacionService;

    public AuthService(UsuarioRepository usuarioRepository, PasswordEncoder passwordEncoder, LogService logService,
                       ConfirmacionCorreoService confirmacionService) {
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
        this.logService = logService;
        this.confirmacionService = confirmacionService;
    }

    /**
     * Registra una cuenta nueva. Toda cuenta nace con rol USUARIO; el rol de organizador lo asigna después un administrador (HU-03).
     * Si la confirmación de correo está activa, envía el enlace para confirmarlo
     * @param request datos ya validados (correo institucional, contraseña de 8 a 72 caracteres)
     * @return el usuario guardado
     * @throws ConflictoException si el correo ya está registrado (confirmado o no) o pertenece a una cuenta desactivada
     */
    @Transactional
    public Usuario registrarUsuario(RegistroRequestDTO request) {
        String correo = normalizarCorreo(request.getCorreo());
        Usuario existente = usuarioRepository.findByCorreoIgnoreCase(correo).orElse(null);
        if (existente != null) {
            if (Boolean.FALSE.equals(existente.getActivo())) {
                throw new ConflictoException("Este correo pertenece a una cuenta desactivada. Inicia sesión para reactivarla.");
            }
            // No se reemplaza la cuenta sin confirmar: otra persona podría registrarse encima con su propia contraseña
            if (!confirmacionService.puedeIniciarSesion(existente)) {
                throw new ConflictoException(REGISTRO_SIN_CONFIRMAR);
            }
            throw new ConflictoException("El correo ya se encuentra registrado.");
        }

        Usuario nuevoUsuario = new Usuario();
        nuevoUsuario.setNombre(request.getNombre().trim());
        nuevoUsuario.setCorreo(correo);
        nuevoUsuario.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        nuevoUsuario.setRol(Rol.USUARIO);
        Usuario guardado = usuarioRepository.save(nuevoUsuario);

        logService.registrarExito(guardado.getId(), LogService.ACCION_REGISTRO, LogService.ENTIDAD_USUARIO, guardado.getId());
        if (confirmacionService.estaExigida()) {
            confirmacionService.enviarEnlace(guardado);
        }
        return guardado;
    }

    /**
     * Valida las credenciales de un usuario. Si la cuenta estaba desactivada y las credenciales son correctas, la reactiva
     *
     * @return el usuario autenticado y si su cuenta estaba desactivada y fue reactivada en este inicio de sesión
     * @throws CredencialesInvalidasException si el correo o la contraseña no coinciden
     * @throws AccesoDenegadoException si la confirmación de correo está activa y la cuenta no la ha hecho
     */
    @Transactional
    public ResultadoAutenticacion autenticar(String correo, String password) {
        Usuario usuario = usuarioRepository.findByCorreoIgnoreCase(normalizarCorreo(correo)).orElse(null);
        Long usuarioId = usuario == null ? null : usuario.getId();

        // Mismo mensaje para correo inexistente y contraseña errónea, para no revelar qué cuentas existen
        if (usuario == null || !passwordEncoder.matches(password, usuario.getPasswordHash())) {
            logService.registrarFallo(usuarioId, LogService.ACCION_LOGIN, LogService.ENTIDAD_USUARIO, usuarioId, "Credenciales inválidas");
            throw new CredencialesInvalidasException("Correo o contraseña incorrectos.");
        }

        // Solo llega aquí quien demostró ser el dueño de la cuenta (contraseña correcta)
        if (!confirmacionService.puedeIniciarSesion(usuario)) {
            logService.registrarFallo(usuarioId, LogService.ACCION_LOGIN, LogService.ENTIDAD_USUARIO, usuarioId, "Correo sin confirmar");
            throw new AccesoDenegadoException(CORREO_SIN_CONFIRMAR);
        }

        boolean cuentaReactivada = Boolean.FALSE.equals(usuario.getActivo());
        if (cuentaReactivada) {
            usuario.setActivo(true);
            usuarioRepository.save(usuario);
            logService.registrarExito(usuarioId, LogService.ACCION_REACTIVAR_CUENTA, LogService.ENTIDAD_USUARIO, usuarioId);
        }

        logService.registrarExito(usuarioId, LogService.ACCION_LOGIN, LogService.ENTIDAD_USUARIO, usuarioId);
        return new ResultadoAutenticacion(usuario, cuentaReactivada);
    }

    /**
     * Resultado de iniciar sesión. La bandera existe porque, una vez reactivada, la cuenta ya figura como activa
     * y quien responde la petición no podría saber que estaba desactivada
     *
     * @param usuario usuario autenticado
     * @param cuentaReactivada true si la cuenta estaba desactivada y se reactivó con este inicio de sesión
     */
    public record ResultadoAutenticacion(Usuario usuario, boolean cuentaReactivada) { }

    /** Los correos se guardan y se comparan en minúsculas y sin espacios en los extremos */
    private String normalizarCorreo(String correo) {
        return correo.trim().toLowerCase();
    }
}
