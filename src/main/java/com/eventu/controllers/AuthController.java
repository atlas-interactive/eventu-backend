package com.eventu.controllers;

import com.eventu.dto.RegistroRequestDTO;
import com.eventu.models.Usuario;
import com.eventu.services.AuthService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = "*")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/registro")
    public ResponseEntity<?> registrarEstudiante(@RequestBody RegistroRequestDTO request) {
        try {
            if (request.getNombre() == null || request.getNombre().isBlank() ||
                request.getCorreo() == null || request.getCorreo().isBlank() ||
                request.getPassword() == null || request.getPassword().isBlank()) {
                return ResponseEntity.badRequest().body(Map.of("error", "Todos los campos son obligatorios."));
            }

            if (!request.getCorreo().matches("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.(edu|edu\\.[a-z]{2})$")) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "Debe ingresar un correo institucional educativo válido."));
            }

            Usuario usuarioGuardado = authService.registrarEstudiante(request);

            return ResponseEntity.status(HttpStatus.CREATED).body(Map.of(
                "mensaje", "Usuario registrado exitosamente.",
                "usuarioId", usuarioGuardado.getId(),
                "correo", usuarioGuardado.getCorreo(),
                "rol", usuarioGuardado.getRol().name()
            ));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody Map<String, String> credenciales) {
        try {
            String correo = credenciales.get("correo");
            String password = credenciales.get("password");

            if (correo == null || password == null || correo.isBlank() || password.isBlank()) {
                return ResponseEntity.badRequest().body(Map.of("error", "Correo y contraseña son requeridos."));
            }

            Usuario usuario = authService.autenticar(correo, password);

            Map<String, Object> response = new HashMap<>();
            // Falta generar un jwt real
            response.put("token", "JWT_PENDIENTE_POR_GENERAR"); 
            response.put("usuarioId", usuario.getId());
            response.put("nombre", usuario.getNombre());
            response.put("correo", usuario.getCorreo());
            response.put("rol", usuario.getRol().name());

            return ResponseEntity.ok(response);
            
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", e.getMessage()));
        }
    }
}