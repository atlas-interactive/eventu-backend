package com.eventu.services;

import com.eventu.exceptions.SolicitudInvalidaException;
import com.eventu.models.TokenConfirmacionCorreo;
import com.eventu.models.Usuario;
import com.eventu.notificaciones.ConfirmacionCorreoSolicitada;
import com.eventu.repositories.TokenConfirmacionCorreoRepository;
import com.eventu.repositories.UsuarioRepository;
import com.eventu.util.TokenSeguro;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;

/**
 * Confirmación del correo de las cuentas nuevas con un enlace enviado al registrarse,
 * para que no se creen cuentas con correos que no existen.
 * Solo se exige si la propiedad {@code eventu.registro.confirmar-correo} es true; apagada, el registro funciona como antes
 */
@Service
public class ConfirmacionCorreoService {

    static final Duration VIGENCIA_ENLACE = Duration.ofHours(24);
    static final Duration ESPERA_ENTRE_SOLICITUDES = Duration.ofMinutes(1);
    static final String ENLACE_INVALIDO = "El enlace de confirmación no es válido o ya venció. Solicita uno nuevo.";

    private final UsuarioRepository usuarioRepository;
    private final TokenConfirmacionCorreoRepository tokenRepository;
    private final LogService logService;
    private final ApplicationEventPublisher publicador;
    private final boolean exigida;

    /** @param exigida valor de {@code eventu.registro.confirmar-correo}; false si no está definida */
    public ConfirmacionCorreoService(UsuarioRepository usuarioRepository, TokenConfirmacionCorreoRepository tokenRepository,
                                     LogService logService, ApplicationEventPublisher publicador,
                                     @Value("${eventu.registro.confirmar-correo:false}") boolean exigida) {
        this.usuarioRepository = usuarioRepository;
        this.tokenRepository = tokenRepository;
        this.logService = logService;
        this.publicador = publicador;
        this.exigida = exigida;
    }

    /** Indica si las cuentas nuevas deben confirmar el correo antes de iniciar sesión */
    public boolean estaExigida() {
        return exigida;
    }

    /** Una cuenta puede iniciar sesión si la confirmación no se exige o si ya confirmó su correo */
    public boolean puedeIniciarSesion(Usuario usuario) {
        return !exigida || usuario.getCorreoConfirmadoEn() != null;
    }

    /**
     * Genera el enlace de confirmación de una cuenta recién registrada y lo envía.
     * Corre en la misma transacción del registro: si algo falla, la cuenta tampoco se crea
     *
     * @param usuario cuenta ya guardada, con id
     */
    @Transactional
    public void enviarEnlace(Usuario usuario) {
        crearYEnviarToken(usuario, LocalDateTime.now());
    }

    /**
     * Envía otro enlace a una cuenta que todavía no confirmó su correo. Con la confirmación apagada no envía nada.
     * Si el correo no está registrado o ya está confirmado no hace nada y no lo indica, para no revelar qué cuentas existen.
     * Si la cuenta pidió otro enlace hace menos de un minuto, ignora la solicitud para evitar el envío masivo de correos
     *
     * @param correo correo institucional ya validado
     */
    @Transactional
    public void reenviarEnlace(String correo) {
        if (!exigida) {
            return;
        }
        Usuario usuario = usuarioRepository.findByCorreoIgnoreCase(correo.trim().toLowerCase()).orElse(null);
        if (usuario == null || usuario.getCorreoConfirmadoEn() != null) {
            return;
        }
        LocalDateTime ahora = LocalDateTime.now();
        if (tokenRepository.existsByUsuarioIdAndCreadoEnAfter(usuario.getId(), ahora.minus(ESPERA_ENTRE_SOLICITUDES))) {
            return;
        }
        crearYEnviarToken(usuario, ahora);
        logService.registrarExito(usuario.getId(), LogService.ACCION_SOLICITAR_CONFIRMACION, LogService.ENTIDAD_USUARIO, usuario.getId());
    }

    /**
     * Confirma el correo con el token del enlace. Si la cuenta ya estaba confirmada responde igual que si se
     * acabara de confirmar: el enlace se abre a veces dos veces (doble clic, recarga de la página)
     *
     * @param token valor del enlace
     * @throws SolicitudInvalidaException si el token no existe, o si venció o se reemplazó sin que la cuenta se confirmara
     */
    @Transactional
    public void confirmar(String token) {
        LocalDateTime ahora = LocalDateTime.now();
        TokenConfirmacionCorreo registro = tokenRepository.buscarPorHashConBloqueo(TokenSeguro.hash(token.trim())).orElse(null);
        Usuario usuario = registro == null ? null : usuarioRepository.findById(registro.getUsuarioId()).orElse(null);
        if (usuario == null) {
            logService.registrarFallo(null, LogService.ACCION_CONFIRMAR_CORREO, LogService.ENTIDAD_USUARIO, null, "Token inexistente");
            throw new SolicitudInvalidaException(ENLACE_INVALIDO);
        }
        if (usuario.getCorreoConfirmadoEn() != null) {
            return;
        }
        if (!registro.estaVigente(ahora)) {
            logService.registrarFallo(usuario.getId(), LogService.ACCION_CONFIRMAR_CORREO, LogService.ENTIDAD_USUARIO, usuario.getId(),
                    "Token usado o vencido");
            throw new SolicitudInvalidaException(ENLACE_INVALIDO);
        }

        usuario.setCorreoConfirmadoEn(ahora);
        usuarioRepository.save(usuario);
        invalidarTokensPendientes(usuario.getId(), ahora);
        logService.registrarExito(usuario.getId(), LogService.ACCION_CONFIRMAR_CORREO, LogService.ENTIDAD_USUARIO, usuario.getId());
    }

    /** Invalida los enlaces anteriores (solo sirve el último), guarda el hash del nuevo y publica el correo */
    private void crearYEnviarToken(Usuario usuario, LocalDateTime ahora) {
        invalidarTokensPendientes(usuario.getId(), ahora);
        String token = TokenSeguro.generar();
        TokenConfirmacionCorreo registro = new TokenConfirmacionCorreo();
        registro.setUsuarioId(usuario.getId());
        registro.setTokenHash(TokenSeguro.hash(token));
        registro.setCreadoEn(ahora);
        registro.setExpiraEn(ahora.plus(VIGENCIA_ENLACE));
        tokenRepository.save(registro);
        publicador.publishEvent(new ConfirmacionCorreoSolicitada(usuario.getCorreo(), usuario.getNombre(), token, registro.getExpiraEn()));
    }

    private void invalidarTokensPendientes(Long usuarioId, LocalDateTime ahora) {
        tokenRepository.findByUsuarioIdAndUsadoEnIsNull(usuarioId).forEach(pendiente -> {
            pendiente.setUsadoEn(ahora);
            tokenRepository.save(pendiente);
        });
    }
}
