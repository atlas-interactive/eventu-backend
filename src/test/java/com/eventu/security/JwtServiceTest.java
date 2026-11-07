package com.eventu.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Base64;

import org.junit.jupiter.api.Test;

import com.eventu.models.Rol;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;

/** Generación y validación del token de sesión (HU-02) */
class JwtServiceTest {

    private static final long VEINTICUATRO_HORAS_MS = 24L * 60 * 60 * 1000;
    private static final String SECRETO = Base64.getEncoder().encodeToString("clave-de-pruebas-de-32-bytes-o-mas!".getBytes());
    private static final String OTRO_SECRETO = Base64.getEncoder().encodeToString("otra-clave-distinta-de-32-bytes-o-mas".getBytes());

    @Test
    void generarToken_conUsuarioYRol_incluyeElIdComoAsuntoYElRol() {
        JwtService servicio = new JwtService(SECRETO, VEINTICUATRO_HORAS_MS);

        Claims claims = servicio.validarYObtenerClaims(servicio.generarToken(2L, Rol.ORGANIZADOR));

        assertEquals("2", claims.getSubject());
        assertEquals("ORGANIZADOR", claims.get("rol", String.class));
    }

    @Test
    void generarToken_venceEnLaDuracionConfigurada() {
        JwtService servicio = new JwtService(SECRETO, VEINTICUATRO_HORAS_MS);

        Claims claims = servicio.validarYObtenerClaims(servicio.generarToken(2L, Rol.ORGANIZADOR));

        long vigencia = claims.getExpiration().getTime() - claims.getIssuedAt().getTime();
        assertEquals(VEINTICUATRO_HORAS_MS, vigencia);
    }

    @Test
    void validarYObtenerClaims_tokenFirmadoConOtraClave_lanzaExcepcion() {
        String tokenAjeno = new JwtService(OTRO_SECRETO, VEINTICUATRO_HORAS_MS).generarToken(2L, Rol.ADMIN);
        JwtService servicio = new JwtService(SECRETO, VEINTICUATRO_HORAS_MS);

        assertThrows(JwtException.class, () -> servicio.validarYObtenerClaims(tokenAjeno));
    }

    @Test
    void validarYObtenerClaims_tokenConContenidoAlterado_lanzaExcepcion() {
        JwtService servicio = new JwtService(SECRETO, VEINTICUATRO_HORAS_MS);
        String[] partes = servicio.generarToken(4L, Rol.USUARIO).split("\\.");
        String contenidoAlterado = Base64.getUrlEncoder().withoutPadding()
                .encodeToString("{\"sub\":\"4\",\"rol\":\"ADMIN\"}".getBytes());
        String tokenAlterado = partes[0] + "." + contenidoAlterado + "." + partes[2];

        assertThrows(JwtException.class, () -> servicio.validarYObtenerClaims(tokenAlterado));
    }

    @Test
    void validarYObtenerClaims_tokenVencido_lanzaExcepcion() throws InterruptedException {
        JwtService servicio = new JwtService(SECRETO, 1);
        String token = servicio.generarToken(4L, Rol.USUARIO);
        Thread.sleep(1100);

        assertThrows(ExpiredJwtException.class, () -> servicio.validarYObtenerClaims(token));
    }

    @Test
    void constructor_conExpiracionFueraDeRango_lanzaExcepcion() {
        IllegalStateException cero = assertThrows(IllegalStateException.class, () -> new JwtService(SECRETO, 0));
        assertThrows(IllegalStateException.class, () -> new JwtService(SECRETO, VEINTICUATRO_HORAS_MS + 1));

        assertTrue(cero.getMessage().contains("24 horas"));
    }
}
