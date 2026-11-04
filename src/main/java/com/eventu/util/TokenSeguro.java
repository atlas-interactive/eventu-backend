package com.eventu.util;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.HexFormat;

/**
 * Tokens de un solo uso que viajan en los enlaces de los correos (recuperación de contraseña y confirmación de correo).
 * En la base de datos solo se guarda su hash
 */
public final class TokenSeguro {

    private static final int BYTES_TOKEN = 32;
    private static final SecureRandom ALEATORIO = new SecureRandom();

    /** Clase de utilidad: no se instancia */
    private TokenSeguro() {
    }

    /** 32 bytes aleatorios en Base64 para URL: imposible de adivinar y seguro dentro de un enlace */
    public static String generar() {
        byte[] bytes = new byte[BYTES_TOKEN];
        ALEATORIO.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    /**
     * SHA-256 en hexadecimal. Basta un hash rápido (no BCrypt) porque el token ya es aleatorio y largo:
     * no hay diccionario con qué adivinarlo
     */
    public static String hash(String token) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(token.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 no está disponible en esta JVM", e);
        }
    }
}
