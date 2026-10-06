package com.eventu.controllers;

import com.eventu.dto.LoginRequestDTO;
import com.eventu.dto.RegistroRequestDTO;
import com.eventu.models.Usuario;
import com.eventu.security.JwtService;
import com.eventu.services.AuthService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/** Registro e inicio de sesión (HU-01 y HU-02). */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;
    private final JwtService jwtService;
    

    public AuthController(AuthService authService, JwtService jwtService) {
        this.authService = authService;
        this.jwtService = jwtService;
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
        Usuario usuario = authService.autenticar(request.getCorreo(), request.getPassword());
        return ResponseEntity.ok(Map.<String, Object>of(
                "token", jwtService.generarToken(usuario.getId(), usuario.getRol()),
                "usuarioId", usuario.getId(),
                "nombre", usuario.getNombre(),
                "correo", usuario.getCorreo(),
                "rol", usuario.getRol().name()));
    }
}
