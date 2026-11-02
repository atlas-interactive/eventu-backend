package com.eventu.controllers;

import com.eventu.dto.LoginRequestDTO;
import com.eventu.dto.RegistroRequestDTO;
import com.eventu.dto.RestablecerContrasenaDTO;
import com.eventu.dto.SolicitudRecuperacionDTO;
import com.eventu.models.Usuario;
import com.eventu.security.JwtService;
import com.eventu.services.AuthService;
import com.eventu.services.AuthService.ResultadoAutenticacion;
import com.eventu.services.RecuperacionContrasenaService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/** Registro, inicio de sesión (HU-01 y HU-02) y recuperación de la contraseña olvidada */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;
    private final JwtService jwtService;
    private final RecuperacionContrasenaService recuperacionService;

    public AuthController(AuthService authService, JwtService jwtService, RecuperacionContrasenaService recuperacionService) {
        this.authService = authService;
        this.jwtService = jwtService;
        this.recuperacionService = recuperacionService;
    }

    /** Crea una cuenta nueva con rol USUARIO */
    @PostMapping("/registro")
    public ResponseEntity<Map<String, Object>> registrarUsuario(@Valid @RequestBody RegistroRequestDTO request) {
        Usuario usuario = authService.registrarUsuario(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(Map.<String, Object>of(
                "mensaje", "Usuario registrado exitosamente.",
                "usuarioId", usuario.getId(),
                "correo", usuario.getCorreo(),
                "rol", usuario.getRol().name()));
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
                "mensaje", "Si el correo está registrado, te enviamos un enlace para restablecer tu contraseña. Revisa tu bandeja de entrada."));
    }

    /** Cambia la contraseña con el token del enlace; responde 400 si el enlace no es válido o venció */
    @PostMapping("/restablecer-contrasena")
    public ResponseEntity<Map<String, Object>> restablecerContrasena(@Valid @RequestBody RestablecerContrasenaDTO request) {
        recuperacionService.restablecerContrasena(request.getToken(), request.getPassword());
        return ResponseEntity.ok(Map.of("mensaje", "Tu contraseña se actualizó. Ya puedes iniciar sesión."));
    }
}
