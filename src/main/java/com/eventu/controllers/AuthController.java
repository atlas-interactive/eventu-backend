package com.eventu.controllers;

import com.eventu.dto.ConfirmacionCorreoDTO;
import com.eventu.dto.LoginRequestDTO;
import com.eventu.dto.ReenvioConfirmacionDTO;
import com.eventu.dto.RegistroRequestDTO;
import com.eventu.dto.RestablecerContrasenaDTO;
import com.eventu.dto.SolicitudRecuperacionDTO;
import com.eventu.models.Usuario;
import com.eventu.security.JwtService;
import com.eventu.services.AuthService;
import com.eventu.services.AuthService.ResultadoAutenticacion;
import com.eventu.services.ConfirmacionCorreoService;
import com.eventu.services.RecuperacionContrasenaService;
import com.eventu.util.Mensajes;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/** Registro, inicio de sesión (HU-01 y HU-02), confirmación del correo y recuperación de la contraseña olvidada */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;
    private final JwtService jwtService;
    private final RecuperacionContrasenaService recuperacionService;
    private final ConfirmacionCorreoService confirmacionService;

    public AuthController(AuthService authService, JwtService jwtService, RecuperacionContrasenaService recuperacionService,
                          ConfirmacionCorreoService confirmacionService) {
        this.authService = authService;
        this.jwtService = jwtService;
        this.recuperacionService = recuperacionService;
        this.confirmacionService = confirmacionService;
    }

    /**
     * Crea una cuenta nueva con rol USUARIO. {@code confirmacionPendiente} indica si la cuenta debe confirmar
     * el correo antes de iniciar sesión
     */
    @PostMapping("/registro")
    public ResponseEntity<Map<String, Object>> registrarUsuario(@Valid @RequestBody RegistroRequestDTO request) {
        Usuario usuario = authService.registrarUsuario(request);
        boolean confirmacionPendiente = !confirmacionService.puedeIniciarSesion(usuario);
        String mensaje = confirmacionPendiente
                ? "Usuario registrado. Te enviamos un enlace a tu correo para confirmar la cuenta. " + Mensajes.REVISA_BANDEJA_Y_SPAM
                : "Usuario registrado exitosamente.";
        return ResponseEntity.status(HttpStatus.CREATED).body(Map.<String, Object>of(
                "mensaje", mensaje,
                "usuarioId", usuario.getId(),
                "correo", usuario.getCorreo(),
                "rol", usuario.getRol().name(),
                "confirmacionPendiente", confirmacionPendiente));
    }

    /** Confirma el correo con el token del enlace; responde 400 si el enlace no es válido o venció */
    @PostMapping("/confirmar-correo")
    public ResponseEntity<Map<String, Object>> confirmarCorreo(@Valid @RequestBody ConfirmacionCorreoDTO request) {
        confirmacionService.confirmar(request.getToken());
        return ResponseEntity.ok(Map.of("mensaje", "Tu correo quedó confirmado. Ya puedes iniciar sesión."));
    }

    /**
     * Envía otro enlace de confirmación. Responde lo mismo exista o no la cuenta,
     * para no revelar qué correos están registrados
     */
    @PostMapping("/reenviar-confirmacion")
    public ResponseEntity<Map<String, Object>> reenviarConfirmacion(@Valid @RequestBody ReenvioConfirmacionDTO request) {
        confirmacionService.reenviarEnlace(request.getCorreo());
        return ResponseEntity.ok(Map.of(
                "mensaje", "Si el correo está registrado y falta confirmarlo, te enviamos un nuevo enlace. " + Mensajes.REVISA_BANDEJA_Y_SPAM));
    }

    /** Valida las credenciales y devuelve los datos de la sesión */
    @PostMapping("/login")
    public ResponseEntity<Map<String, Object>> login(@Valid @RequestBody LoginRequestDTO request) {
        ResultadoAutenticacion resultado = authService.autenticar(request.getCorreo(), request.getPassword());
        Usuario usuario = resultado.usuario();
        return ResponseEntity.ok(Map.<String, Object>of(
                "token", jwtService.generarToken(usuario.getId(), usuario.getRol()),
                "usuarioId", usuario.getId(),
                "nombre", usuario.getNombre(),
                "correo", usuario.getCorreo(),
                "rol", usuario.getRol().name(),
                "cuentaReactivada", resultado.cuentaReactivada()));
    }

    /**
     * Envía al correo un enlace para restablecer la contraseña. Responde lo mismo exista o no la cuenta,
     * para no revelar qué correos están registrados
     */
    @PostMapping("/recuperar-contrasena")
    public ResponseEntity<Map<String, Object>> solicitarRecuperacion(@Valid @RequestBody SolicitudRecuperacionDTO request) {
        recuperacionService.solicitarRecuperacion(request.getCorreo());
        return ResponseEntity.ok(Map.of(
                "mensaje", "Si el correo está registrado, te enviamos un enlace para restablecer tu contraseña. " + Mensajes.REVISA_BANDEJA_Y_SPAM));
    }

    /** Cambia la contraseña con el token del enlace; responde 400 si el enlace no es válido o venció */
    @PostMapping("/restablecer-contrasena")
    public ResponseEntity<Map<String, Object>> restablecerContrasena(@Valid @RequestBody RestablecerContrasenaDTO request) {
        recuperacionService.restablecerContrasena(request.getToken(), request.getPassword());
        return ResponseEntity.ok(Map.of("mensaje", "Tu contraseña se actualizó. Ya puedes iniciar sesión."));
    }
}
