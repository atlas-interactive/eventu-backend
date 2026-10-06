package com.eventu.security;

import com.eventu.models.Rol;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.util.Date;

@Service
public class JwtService {

    private static final long MAXIMO_EXPIRACION_MS = 24L * 60 * 60 * 1000;

    private final SecretKey clave;
    private final long expiracionMs;

    public JwtService(@Value("${eventu.jwt.secret}") String secreto,
                      @Value("${eventu.jwt.expiracion-ms}") long expiracionMs) {
        if (expiracionMs <= 0 || expiracionMs > MAXIMO_EXPIRACION_MS) {
            throw new IllegalStateException("La expiración del JWT debe estar entre 1 ms y 24 horas.");
        }
        this.clave = Keys.hmacShaKeyFor(Decoders.BASE64.decode(secreto));
        this.expiracionMs = expiracionMs;
    }

    public String generarToken(Long usuarioId, Rol rol) {
        Date ahora = new Date();
        return Jwts.builder()
                .subject(String.valueOf(usuarioId))
                .claim("rol", rol.name())
                .issuedAt(ahora)
                .expiration(new Date(ahora.getTime() + expiracionMs))
                .signWith(clave)
                .compact();
    }

    public Claims validarYObtenerClaims(String token) {
        return Jwts.parser().verifyWith(clave).build().parseSignedClaims(token).getPayload();
    }
}