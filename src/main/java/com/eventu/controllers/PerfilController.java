package com.eventu.controllers;

import com.eventu.dto.CambioContrasenaDTO;
import com.eventu.dto.DesactivacionCuentaDTO;
import com.eventu.dto.PerfilActualizacionDTO;
import com.eventu.dto.PerfilResponseDTO;
import com.eventu.security.JwtService;
import com.eventu.security.UsuarioAutenticado;
import com.eventu.services.PerfilService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/** Perfil del usuario autenticado (HU-19). Cualquier rol accede solo a su propia cuenta, identificada por el token */
@RestController
@RequestMapping("/api/perfil")
public class PerfilController {

    private final PerfilService perfilService;
    private final JwtService jwtService;

    public PerfilController(PerfilService perfilService, JwtService jwtService) {
        this.perfilService = perfilService;
        this.jwtService = jwtService;
    }

    /** Devuelve nombre, correo, rol, código institucional y fecha de registro */
    @GetMapping
    public ResponseEntity<PerfilResponseDTO> obtenerPerfil(@AuthenticationPrincipal UsuarioAutenticado actual) {
        return ResponseEntity.ok(perfilService.obtenerPerfil(actual.id()));
    }

    /** Actualiza el nombre y el código institucional; responde 400 si se intenta cambiar el correo */
    @PutMapping
    public ResponseEntity<PerfilResponseDTO> actualizarPerfil(@Valid @RequestBody PerfilActualizacionDTO request,
                                                              @AuthenticationPrincipal UsuarioAutenticado actual) {
        return ResponseEntity.ok(perfilService.actualizarPerfil(actual.id(), request));
    }

    /**
     * Cambia la contraseña. El token con el que se hizo la petición deja de servir, por eso la respuesta
     * trae uno nuevo para que la sesión actual continúe
     */
    @PutMapping("/contrasena")
    public ResponseEntity<Map<String, Object>> cambiarContrasena(@Valid @RequestBody CambioContrasenaDTO request,
                                                                 @AuthenticationPrincipal UsuarioAutenticado actual) {
        perfilService.cambiarContrasena(actual.id(), request.getPasswordActual(), request.getPasswordNueva());
        return ResponseEntity.ok(Map.of(
                "mensaje", "Contraseña actualizada exitosamente.",
                "token", jwtService.generarToken(actual.id(), actual.rol())));
    }

    /** Desactiva la cuenta propia; el token deja de servir y la cuenta se reactiva al volver a iniciar sesión */
    @PatchMapping("/desactivar")
    public ResponseEntity<Map<String, Object>> desactivarCuenta(@Valid @RequestBody DesactivacionCuentaDTO request,
                                                                @AuthenticationPrincipal UsuarioAutenticado actual) {
        perfilService.desactivarCuenta(actual.id(), request.getPassword());
        return ResponseEntity.ok(Map.of("mensaje", "Tu cuenta fue desactivada. Puedes reactivarla iniciando sesión de nuevo."));
    }
}
