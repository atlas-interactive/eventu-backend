package com.eventu.security;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;

import org.junit.jupiter.api.Test;

import com.eventu.models.Usuario;

import io.jsonwebtoken.Claims;

/** Pruebas del rechazo de tokens emitidos antes del último cambio de contraseña */
class JwtAuthFilterTest {

    private static final LocalDateTime CAMBIO = LocalDateTime.of(2026, 11, 2, 14, 5, 30, 400_000_000);

    @Test
    void tokenEmitidoAntesDelCambio_seRechaza() {
        assertTrue(JwtAuthFilter.emitidoAntesDelCambioDeContrasena(claimsEmitidos(CAMBIO.minusMinutes(5)), usuarioConCambio(CAMBIO)));
    }

    @Test
    void tokenEmitidoEnElMismoSegundoDelCambio_seAcepta() {
        // El token nuevo que devuelve el cambio de contraseña se emite en el mismo segundo, sin milisegundos
        assertFalse(JwtAuthFilter.emitidoAntesDelCambioDeContrasena(claimsEmitidos(CAMBIO.withNano(0)), usuarioConCambio(CAMBIO)));
    }

    @Test
    void tokenEmitidoDespuesDelCambio_seAcepta() {
        assertFalse(JwtAuthFilter.emitidoAntesDelCambioDeContrasena(claimsEmitidos(CAMBIO.plusSeconds(3)), usuarioConCambio(CAMBIO)));
    }

    @Test
    void usuarioQueNuncaCambioSuContrasena_seAcepta() {
        assertFalse(JwtAuthFilter.emitidoAntesDelCambioDeContrasena(claimsEmitidos(CAMBIO), usuarioConCambio(null)));
    }

    private Claims claimsEmitidos(LocalDateTime fecha) {
        Claims claims = mock(Claims.class);
        when(claims.getIssuedAt()).thenReturn(Date.from(fecha.atZone(ZoneId.systemDefault()).toInstant()));
        return claims;
    }

    private Usuario usuarioConCambio(LocalDateTime cambio) {
        Usuario usuario = new Usuario();
        usuario.setPasswordActualizadaEn(cambio);
        return usuario;
    }
}
