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

/** Gestión del rol de organizador. Solo para administradores*/
@RestController
@RequestMapping("/api/usuarios")
@PreAuthorize("hasRole('ADMIN')")
public class UsuarioController {

    private final UsuarioService usuarioService;

    public UsuarioController(UsuarioService usuarioService) {
        this.usuarioService = usuarioService;
    }

    /**
     * Busca usuarios por nombre o correo
     *
     * @param criterio texto a buscar
     * @return 200 con los usuarios que coinciden; 404 si ninguno; 400 si el criterio está vacío
     */
    @GetMapping
    public ResponseEntity<List<UsuarioResponseDTO>> buscarUsuarios(@RequestParam String criterio) {
        return ResponseEntity.ok(usuarioService.buscarUsuarios(criterio));
    }

    /**
     * Lista los organizadores actuales
     *
     * @return 200 con la lista (vacía si no hay organizadores)
     */
    @GetMapping("/organizadores")
    public ResponseEntity<List<UsuarioResponseDTO>> listarOrganizadores() {
        return ResponseEntity.ok(usuarioService.listarOrganizadores());
    }

    /**
     * Asigna el rol de organizador a un usuario
     *
     * @param id usuario que recibirá el rol
     * @return 200 con el mensaje, el id y el nuevo rol; 404 si no existe; 409 si ya es organizador
     */
    @PutMapping("/{id}/asignar-organizador")
    public ResponseEntity<Map<String, Object>> asignarOrganizador(@PathVariable Long id, @AuthenticationPrincipal UsuarioAutenticado actual) {
        UsuarioResponseDTO usuario = usuarioService.asignarOrganizador(id, actual.id());
        return ResponseEntity.ok(Map.<String, Object>of(
                "mensaje", "Rol de organizador asignado exitosamente.",
                "usuarioId", usuario.id(),
                "rol", usuario.rol()));
    }

    /**
     * Revoca el rol de organizador de un usuario; conserva su cuenta y sus eventos
     *
     * @param id usuario al que se le quita el rol
     * @return 200 con el mensaje, el id y el nuevo rol; 404 si no existe; 409 si no es organizador
     */
    @PutMapping("/{id}/revocar-organizador")
    public ResponseEntity<Map<String, Object>> revocarOrganizador(@PathVariable Long id, @AuthenticationPrincipal UsuarioAutenticado actual) {
        UsuarioResponseDTO usuario = usuarioService.revocarOrganizador(id, actual.id());
        return ResponseEntity.ok(Map.<String, Object>of(
                "mensaje", "Rol de organizador revocado exitosamente.",
                "usuarioId", usuario.id(),
                "rol", usuario.rol()));
    }
}
