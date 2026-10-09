package com.eventu.controllers;

import java.util.List;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.eventu.dto.UsuarioResponseDTO;
import com.eventu.security.UsuarioAutenticado;
import com.eventu.services.UsuarioService;

/** Gestión del rol de organizador. Solo para administradores (HU-03) */
@RestController
@RequestMapping("/api/usuarios")
@PreAuthorize("hasRole('ADMIN')")
public class UsuarioController {

    private final UsuarioService usuarioService;

    public UsuarioController(UsuarioService usuarioService) {
        this.usuarioService = usuarioService;
    }

    @GetMapping
    public ResponseEntity<List<UsuarioResponseDTO>> buscarUsuarios(@RequestParam String criterio) {
        return ResponseEntity.ok(usuarioService.buscarUsuarios(criterio));
    }

    @PutMapping("/{id}/asignar-organizador")
    public ResponseEntity<Map<String, Object>> asignarOrganizador(@PathVariable Long id,
                                                                  @AuthenticationPrincipal UsuarioAutenticado actual) {
        UsuarioResponseDTO usuario = usuarioService.asignarOrganizador(id, actual.id());
        return ResponseEntity.ok(Map.<String, Object>of(
                "mensaje", "Rol de organizador asignado exitosamente.",
                "usuarioId", usuario.id(),
                "rol", usuario.rol()));
    }

    @PutMapping("/{id}/revocar-organizador")
    public ResponseEntity<Map<String, Object>> revocarOrganizador(@PathVariable Long id,
                                                                  @AuthenticationPrincipal UsuarioAutenticado actual) {
        UsuarioResponseDTO usuario = usuarioService.revocarOrganizador(id, actual.id());
        return ResponseEntity.ok(Map.<String, Object>of(
                "mensaje", "Rol de organizador revocado exitosamente.",
                "usuarioId", usuario.id(),
                "rol", usuario.rol()));
    }
}