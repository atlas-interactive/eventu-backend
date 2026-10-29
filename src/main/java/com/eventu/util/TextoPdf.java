package com.eventu.util;

import java.io.IOException;

import org.apache.pdfbox.pdmodel.font.PDFont;

/** Utilidades para escribir texto con las fuentes estándar de PDFBox */
public final class TextoPdf {

    private TextoPdf() {
    }

    /**
     * Las fuentes estándar del PDF solo traen los caracteres de WinAnsi (incluye tildes y ñ).
     * Cualquier otro carácter (emojis, otros alfabetos) haría fallar todo el documento, así que se cambia por "?"
     */
    public static String compatible(String texto, PDFont fuente) {
        StringBuilder resultado = new StringBuilder(texto.length());
        texto.codePoints().forEach(c -> {
            String caracter = new String(Character.toChars(c));
            try {
                fuente.encode(caracter);
                resultado.append(caracter);
            } catch (IllegalArgumentException | IOException e) {
                resultado.append('?');
            }
        });
        return resultado.toString();
    }

    /** Ancho en puntos que ocupa el texto con esa fuente y tamaño */
    public static float ancho(String texto, PDFont fuente, float tamano) throws IOException {
        return fuente.getStringWidth(texto) / 1000 * tamano;
    }

    /** Reduce el tamaño de letra hasta que el texto quepa en el ancho dado, sin bajar del mínimo */
    public static float tamanoQueCabe(String texto, PDFont fuente, float tamano, float minimo, float anchoMaximo)
            throws IOException {
        float actual = tamano;
        while (actual > minimo && ancho(texto, fuente, actual) > anchoMaximo) {
            actual -= 0.5f;
        }
        return actual;
    }
}
