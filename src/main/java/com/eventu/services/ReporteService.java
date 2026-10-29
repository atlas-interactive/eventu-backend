package com.eventu.services;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.springframework.stereotype.Service;

import com.eventu.dto.InscritoResponseDTO;
import com.eventu.exceptions.RecursoNoEncontradoException;
import com.eventu.models.Evento;
import com.eventu.repositories.EventoRepository;
import com.eventu.util.Mensajes;

/** Reporte en PDF de los inscritos y asistentes de un evento (HU-17) */
@Service
public class ReporteService {

    static final String SIN_REGISTROS = "No hay registros de inscritos ni asistentes para este evento.";

    private static final DateTimeFormatter FECHA_HORA = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private static final Locale ES_CO = Locale.of("es", "CO");

    private static final float MARGEN = 50;
    private static final float ALTO_FILA = 16;
    private static final float TAMANO_TEXTO = 10;
    private static final float X_NUMERO = MARGEN;
    private static final float X_NOMBRE = 80;
    private static final float X_CORREO = 280;
    private static final float X_ASISTIO = 495;
    private static final float ANCHO_NOMBRE = X_CORREO - X_NOMBRE - 10;
    private static final float ANCHO_CORREO = X_ASISTIO - X_CORREO - 10;

    private final InscripcionService inscripcionService;
    private final EventoRepository eventoRepository;
    private final LogService logService;

    public ReporteService(InscripcionService inscripcionService, EventoRepository eventoRepository,
                          LogService logService) {
        this.inscripcionService = inscripcionService;
        this.eventoRepository = eventoRepository;
        this.logService = logService;
    }

    /**
     * Genera el PDF con los inscritos activos de un evento y si asistieron.
     * La verificación de permisos es la misma de la consulta de inscritos (HU-13):
     * solo el organizador dueño del evento o un administrador
     *
     * @throws com.eventu.exceptions.AccesoDenegadoException si el usuario no puede gestionar eventos o el evento es ajeno
     * @throws RecursoNoEncontradoException si el evento no existe
     */
    public byte[] generarReporteInscritos(Long eventoId, Long usuarioId) {
        List<InscritoResponseDTO> inscritos = inscripcionService.listarInscritosDeEvento(eventoId, usuarioId);
        Evento evento = eventoRepository.findById(eventoId)
                .orElseThrow(() -> new RecursoNoEncontradoException(Mensajes.EVENTO_NO_ENCONTRADO));

        byte[] pdf;
        try {
            pdf = construirPdf(evento, inscritos);
        } catch (IOException e) {
            throw new IllegalStateException("Error al generar el reporte en PDF.", e);
        }
        logService.registrarExito(usuarioId, LogService.ACCION_GENERAR_REPORTE, LogService.ENTIDAD_EVENTO, eventoId);
        return pdf;
    }

    private byte[] construirPdf(Evento evento, List<InscritoResponseDTO> inscritos) throws IOException {
        PDFont negrita = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);
        PDFont normal = new PDType1Font(Standard14Fonts.FontName.HELVETICA);
        long asistentes = inscritos.stream().filter(InscritoResponseDTO::asistio).count();

        try (PDDocument documento = new PDDocument()) {
            PDPage pagina = new PDPage(PDRectangle.A4);
            documento.addPage(pagina);
            PDPageContentStream contenido = new PDPageContentStream(documento, pagina);
            float y = pagina.getMediaBox().getHeight() - MARGEN;

            escribir(contenido, negrita, 16, MARGEN, y, "EventU - Reporte de inscritos y asistentes");
            y -= 26;
            escribir(contenido, negrita, 12, MARGEN, y, recortar(evento.getTitulo(), negrita, 12, 495));
            y -= 18;
            escribir(contenido, normal, TAMANO_TEXTO, MARGEN, y, "Fecha del evento: "
                    + (evento.getFechaInicio() != null ? evento.getFechaInicio().format(FECHA_HORA) : "-"));
            y -= 14;
            escribir(contenido, normal, TAMANO_TEXTO, MARGEN, y,
                    recortar("Lugar: " + valor(evento.getUbicacion()), normal, TAMANO_TEXTO, 495));
            y -= 14;
            escribir(contenido, normal, TAMANO_TEXTO, MARGEN, y,
                    "Generado el: " + LocalDateTime.now().format(FECHA_HORA));
            y -= 22;
            escribir(contenido, negrita, TAMANO_TEXTO, MARGEN, y, String.format(ES_CO,
                    "Inscritos activos: %d     Asistentes: %d     Porcentaje de asistencia: %.2f %%",
                    inscritos.size(), asistentes, porcentaje(asistentes, inscritos.size())));
            y -= 26;

            if (inscritos.isEmpty()) {
                escribir(contenido, normal, 11, MARGEN, y, SIN_REGISTROS);
            } else {
                y = encabezadoTabla(contenido, negrita, y);
                int numero = 1;
                for (InscritoResponseDTO inscrito : inscritos) {
                    if (y < MARGEN + ALTO_FILA) {
                        contenido.close();
                        pagina = new PDPage(PDRectangle.A4);
                        documento.addPage(pagina);
                        contenido = new PDPageContentStream(documento, pagina);
                        y = encabezadoTabla(contenido, negrita, pagina.getMediaBox().getHeight() - MARGEN);
                    }
                    escribir(contenido, normal, TAMANO_TEXTO, X_NUMERO, y, String.valueOf(numero++));
                    escribir(contenido, normal, TAMANO_TEXTO, X_NOMBRE, y,
                            recortar(valor(inscrito.nombre()), normal, TAMANO_TEXTO, ANCHO_NOMBRE));
                    escribir(contenido, normal, TAMANO_TEXTO, X_CORREO, y,
                            recortar(valor(inscrito.correo()), normal, TAMANO_TEXTO, ANCHO_CORREO));
                    escribir(contenido, normal, TAMANO_TEXTO, X_ASISTIO, y, inscrito.asistio() ? "Sí" : "No");
                    y -= ALTO_FILA;
                }
            }
            contenido.close();

            numerarPaginas(documento, normal);

            ByteArrayOutputStream salida = new ByteArrayOutputStream();
            documento.save(salida);
            return salida.toByteArray();
        }
    }

    /** Escribe los títulos de las columnas y una línea debajo; devuelve la altura donde empieza la primera fila */
    private static float encabezadoTabla(PDPageContentStream contenido, PDFont negrita, float y) throws IOException {
        escribir(contenido, negrita, TAMANO_TEXTO, X_NUMERO, y, "#");
        escribir(contenido, negrita, TAMANO_TEXTO, X_NOMBRE, y, "Nombre");
        escribir(contenido, negrita, TAMANO_TEXTO, X_CORREO, y, "Correo");
        escribir(contenido, negrita, TAMANO_TEXTO, X_ASISTIO, y, "Asistió");
        contenido.moveTo(MARGEN, y - 4);
        contenido.lineTo(PDRectangle.A4.getWidth() - MARGEN, y - 4);
        contenido.stroke();
        return y - ALTO_FILA - 2;
    }

    /** Agrega "Página i de n" al pie de cada página; se hace al final porque antes no se sabe cuántas hay */
    private static void numerarPaginas(PDDocument documento, PDFont fuente) throws IOException {
        int total = documento.getNumberOfPages();
        for (int i = 0; i < total; i++) {
            PDPage pagina = documento.getPage(i);
            try (PDPageContentStream pie = new PDPageContentStream(documento, pagina,
                    PDPageContentStream.AppendMode.APPEND, true, true)) {
                escribir(pie, fuente, 8, MARGEN, MARGEN / 2, "Página " + (i + 1) + " de " + total);
            }
        }
    }

    private static void escribir(PDPageContentStream contenido, PDFont fuente, float tamano, float x, float y,
                                 String texto) throws IOException {
        contenido.beginText();
        contenido.setFont(fuente, tamano);
        contenido.newLineAtOffset(x, y);
        contenido.showText(compatible(texto, fuente));
        contenido.endText();
    }

    /** Corta el texto con "..." si no cabe en el ancho de la columna */
    private static String recortar(String texto, PDFont fuente, float tamano, float ancho) throws IOException {
        String limpio = compatible(texto, fuente);
        if (anchoDe(limpio, fuente, tamano) <= ancho) {
            return limpio;
        }
        String puntos = "...";
        int fin = limpio.length();
        while (fin > 0 && anchoDe(limpio.substring(0, fin) + puntos, fuente, tamano) > ancho) {
            fin--;
        }
        return limpio.substring(0, fin) + puntos;
    }

    private static float anchoDe(String texto, PDFont fuente, float tamano) throws IOException {
        return fuente.getStringWidth(texto) / 1000 * tamano;
    }

    /**
     * Las fuentes estándar del PDF solo traen los caracteres de WinAnsi (incluye tildes y ñ).
     * Cualquier otro carácter (emojis, otros alfabetos) haría fallar todo el reporte, así que se cambia por "?"
     */
    private static String compatible(String texto, PDFont fuente) {
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

    private static String valor(String texto) {
        return texto == null || texto.isBlank() ? "-" : texto;
    }

    private static double porcentaje(long asistentes, long inscritos) {
        return inscritos == 0 ? 0.0 : asistentes * 100.0 / inscritos;
    }
}
