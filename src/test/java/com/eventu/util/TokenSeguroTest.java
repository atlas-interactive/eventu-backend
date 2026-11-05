package com.eventu.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/** Pruebas de los tokens que viajan en los enlaces de los correos */
class TokenSeguroTest {

    @Test
    void generar_devuelveTokensDistintosSeguroParaUnaUrl() {
        String uno = TokenSeguro.generar();
        String otro = TokenSeguro.generar();

        assertNotEquals(uno, otro);
        assertTrue(uno.length() >= 43, "32 bytes en Base64 sin relleno");
        assertTrue(uno.matches("[A-Za-z0-9_-]+"));
    }

    @Test
    void hash_esSha256EnHexadecimalYSiempreIgualParaElMismoToken() {
        String hash = TokenSeguro.hash("token-del-enlace");

        assertEquals(64, hash.length());
        assertEquals(hash, TokenSeguro.hash("token-del-enlace"));
        assertNotEquals(hash, TokenSeguro.hash("token-del-enlacex"));
    }
}
