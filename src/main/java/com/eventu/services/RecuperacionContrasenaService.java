package com.eventu.services;

import com.eventu.exceptions.SolicitudInvalidaException;
import com.eventu.models.TokenRecuperacion;
import com.eventu.models.Usuario;
import com.eventu.notificaciones.ContrasenaCambiada;
import com.eventu.notificaciones.RecuperacionContrasenaSolicitada;
import com.eventu.repositories.TokenRecuperacionRepository;
import com.eventu.repositories.UsuarioRepository;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.HexFormat;

/** Recuperación de la contraseña olvidada mediante un enlace enviado al correo institucional */
@Service
public class RecuperacionContrasenaService {

    static final Duration VIGENCIA_ENLACE = Duration.ofMinutes(30);
    static final Duration ESPERA_ENTRE_SOLICITUDES = Duration.ofMinutes(1);
    static final String ENLACE_INVALIDO = "El enlace para restablecer la contraseña no es válido o ya venció. Solicita uno nuevo.";
    private static final int BYTES_TOKEN = 32;

    private final UsuarioRepository usuarioRepository;
    private final TokenRecuperacionRepository tokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final LogService logService;
    private final ApplicationEventPublisher publicador;
    private final SecureRandom aleatorio = new SecureRandom();

    public RecuperacionContrasenaService(UsuarioRepository usuarioRepository, TokenRecuperacionRepository tokenRepository,
                                         PasswordEncoder passwordEncoder, LogService logService,
                                         ApplicationEventPublisher publicador) {
        this.usuarioRepository = usuarioRepository;
        this.tokenRepository = tokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.logService = logService;
        this.publicador = publicador;
    }

    /**
     * Genera un enlace de un solo uso y lo envía al correo de la cuenta.
     * Si el correo no está registrado no hace nada y no lo indica, para no revelar qué cuentas existen.
     * Una cuenta desactivada también puede recuperar su contraseña; al iniciar sesión se reactiva (HU-02).
     * Si la cuenta pidió otro enlace hace menos de un minuto, ignora la solicitud para evitar el envío masivo de correos
     *
     * @param correo correo institucional ya validado
     */
    @Transactional
    public void solicitarRecuperacion(String correo) {
        Usuario usuario = usuarioRepository.findByCorreoIgnoreCase(correo.trim().toLowerCase()).orElse(null);
        if (usuario == null) {
            return;
        }
        LocalDateTime ahora = LocalDateTime.now();
        if (tokenRepository.existsByUsuarioIdAndCreadoEnAfter(usuario.getId(), ahora.minus(ESPERA_ENTRE_SOLICITUDES))) {
            return;
        }

        invalidarTokensPendientes(usuario.getId(), ahora);
        String token = generarToken();
        TokenRecuperacion registro = new TokenRecuperacion();
        registro.setUsuarioId(usuario.getId());
        registro.setTokenHash(hash(token));
        registro.setCreadoEn(ahora);
        registro.setExpiraEn(ahora.plus(VIGENCIA_ENLACE));
        tokenRepository.save(registro);

        logService.registrarExito(usuario.getId(), LogService.ACCION_SOLICITAR_RECUPERACION, LogService.ENTIDAD_USUARIO, usuario.getId());
        publicador.publishEvent(new RecuperacionContrasenaSolicitada(usuario.getCorreo(), usuario.getNombre(), token, registro.getExpiraEn()));
    }

    /**
     * Cambia la contraseña con el token recibido por correo. El token queda usado y las sesiones abiertas
     * con la contraseña anterior dejan de ser válidas
     *
     * @param token valor del enlace
     * @param passwordNueva contraseña nueva, ya validada entre 8 y 72 caracteres
     * @throws SolicitudInvalidaException si el token no existe, ya se usó o venció
     */
    @Transactional
    public void restablecerContrasena(String token, String passwordNueva) {
        LocalDateTime ahora = LocalDateTime.now();
        TokenRecuperacion registro = tokenRepository.buscarPorHashConBloqueo(hash(token.trim())).orElse(null);
        if (registro == null || !registro.estaVigente(ahora)) {
            Long usuarioId = registro == null ? null : registro.getUsuarioId();
            logService.registrarFallo(usuarioId, LogService.ACCION_RESTABLECER_CONTRASENA, LogService.ENTIDAD_USUARIO, usuarioId,
                    registro == null ? "Token inexistente" : "Token usado o vencido");
            throw new SolicitudInvalidaException(ENLACE_INVALIDO);
        }
        Usuario usuario = usuarioRepository.findById(registro.getUsuarioId())
                .orElseThrow(() -> new SolicitudInvalidaException(ENLACE_INVALIDO));

        usuario.setPasswordHash(passwordEncoder.encode(passwordNueva));
        usuario.setPasswordActualizadaEn(ahora);
        usuarioRepository.save(usuario);
        invalidarTokensPendientes(usuario.getId(), ahora);

        logService.registrarExito(usuario.getId(), LogService.ACCION_RESTABLECER_CONTRASENA, LogService.ENTIDAD_USUARIO, usuario.getId());
        publicador.publishEvent(new ContrasenaCambiada(usuario.getCorreo(), usuario.getNombre(), ahora));
    }

    /**
     * Marca como usados los enlaces pendientes del usuario. Se llama al pedir uno nuevo (solo sirve el último),
     * al restablecer y al cambiar la contraseña desde el perfil
     */
    @Transactional
    public void invalidarTokensPendientes(Long usuarioId, LocalDateTime ahora) {
        tokenRepository.findByUsuarioIdAndUsadoEnIsNull(usuarioId).forEach(pendiente -> {
            pendiente.setUsadoEn(ahora);
            tokenRepository.save(pendiente);
        });
    }

    /** 32 bytes aleatorios en Base64 para URL: imposible de adivinar y seguro dentro de un enlace */
    private String generarToken() {
        byte[] bytes = new byte[BYTES_TOKEN];
        aleatorio.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    /**
     * SHA-256 en hexadecimal. Basta un hash rápido (no BCrypt) porque el token ya es aleatorio y largo:
     * no hay diccionario con qué adivinarlo
     */
    static String hash(String token) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(token.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 no está disponible en esta JVM", e);
        }
    }
}
