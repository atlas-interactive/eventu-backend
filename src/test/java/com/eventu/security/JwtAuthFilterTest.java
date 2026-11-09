package com.eventu.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Base64;
import java.util.Date;
import java.util.Optional;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import com.eventu.models.Rol;
import com.eventu.models.Usuario;
import com.eventu.repositories.UsuarioRepository;

import io.jsonwebtoken.Claims;

/** Pruebas del filtro que autentica cada petición con el token JWT (HU-02) */
class JwtAuthFilterTest {

    private static final String SECRETO = Base64.getEncoder().encodeToString("clave-de-pruebas-de-32-bytes-o-mas!".getBytes());

    private final JwtService jwtService = new JwtService(SECRETO, 60_000);
    private final UsuarioRepository usuarioRepository = mock(UsuarioRepository.class);
    private final JwtAuthFilter filtro = new JwtAuthFilter(jwtService, usuarioRepository);

    @AfterEach
    void limpiarSesion() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void peticionConTokenValidoDeCuentaActiva_quedaAutenticadaConSuRol() throws Exception {
        Usuario usuario = usuarioRegistrado(true);
        MockFilterChain cadena = new MockFilterChain();

        filtro.doFilter(peticionCon("Bearer " + jwtService.generarToken(2L, Rol.ORGANIZADOR)), new MockHttpServletResponse(), cadena);

        Authentication autenticacion = SecurityContextHolder.getContext().getAuthentication();
        assertNotNull(autenticacion);
        assertEquals(new UsuarioAutenticado(2L, usuario.getCorreo(), Rol.ORGANIZADOR), autenticacion.getPrincipal());
        assertEquals("ROLE_ORGANIZADOR", autenticacion.getAuthorities().iterator().next().getAuthority());
        assertNotNull(cadena.getRequest());
    }

    @Test
    void peticionSinToken_continuaSinAutenticar() throws Exception {
        MockFilterChain cadena = new MockFilterChain();

        filtro.doFilter(new MockHttpServletRequest("GET", "/api/eventos"), new MockHttpServletResponse(), cadena);

        assertNull(SecurityContextHolder.getContext().getAuthentication());
        assertNotNull(cadena.getRequest());
        verifyNoInteractions(usuarioRepository);
    }

    @Test
    void peticionConTokenInvalido_continuaSinAutenticar() throws Exception {
        MockFilterChain cadena = new MockFilterChain();

        filtro.doFilter(peticionCon("Bearer token-alterado"), new MockHttpServletResponse(), cadena);

        assertNull(SecurityContextHolder.getContext().getAuthentication());
        assertNotNull(cadena.getRequest());
    }

    @Test
    void peticionConTokenDeCuentaDesactivada_noSeAutentica() throws Exception {
        usuarioRegistrado(false);

        filtro.doFilter(peticionCon("Bearer " + jwtService.generarToken(2L, Rol.ORGANIZADOR)), new MockHttpServletResponse(), new MockFilterChain());

        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }

    @Test
    void peticionConTokenEmitidoAntesDelCambioDeContrasena_noSeAutentica() throws Exception {
        Usuario usuario = usuarioRegistrado(true);
        usuario.setPasswordActualizadaEn(LocalDateTime.now().plusMinutes(1));

        filtro.doFilter(peticionCon("Bearer " + jwtService.generarToken(2L, Rol.ORGANIZADOR)), new MockHttpServletResponse(), new MockFilterChain());

        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }

    private MockHttpServletRequest peticionCon(String cabecera) {
        MockHttpServletRequest peticion = new MockHttpServletRequest("GET", "/api/eventos");
        peticion.addHeader("Authorization", cabecera);
        return peticion;
    }

    private Usuario usuarioRegistrado(boolean activo) {
        Usuario usuario = new Usuario();
        usuario.setId(2L);
        usuario.setCorreo("org.a@unillanos.edu.co");
        usuario.setRol(Rol.ORGANIZADOR);
        usuario.setActivo(activo);
        when(usuarioRepository.findById(2L)).thenReturn(Optional.of(usuario));
        return usuario;
    }

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
