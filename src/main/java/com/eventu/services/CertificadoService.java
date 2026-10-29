package com.eventu.services;

import com.eventu.exceptions.ConflictoException;
import com.eventu.exceptions.RecursoNoEncontradoException;
import com.eventu.models.EstadoInscripcion;
import com.eventu.models.Evento;
import com.eventu.models.Inscripcion;
import com.eventu.repositories.AsistenciaRepository;
import com.eventu.repositories.EventoRepository;
import com.eventu.repositories.InscripcionRepository;
import com.eventu.util.Mensajes;
import com.eventu.util.TextoPdf;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/** Generación del certificado de asistencia en PDF */
@Service
public class CertificadoService {

    private static final DateTimeFormatter FECHA_LARGA =
            DateTimeFormatter.ofPattern("d 'de' MMMM 'de' yyyy", Locale.of("es", "CO"));

    private final EventoRepository eventoRepository;
    private final InscripcionRepository inscripcionRepository;
    private final AsistenciaRepository asistenciaRepository;

    public CertificadoService(EventoRepository eventoRepository,InscripcionRepository inscripcionRepository, AsistenciaRepository asistenciaRepository) {
        this.eventoRepository = eventoRepository;
        this.inscripcionRepository = inscripcionRepository;
        this.asistenciaRepository = asistenciaRepository;
    }

    /**
     * Genera el certificado de un usuario para un evento
     * @throws RecursoNoEncontradoException si el evento o la inscripción activa no existen
     * @throws ConflictoException si el evento no es certificable o la asistencia no está verificada
     */
    @Transactional(readOnly = true)
    public byte[] generarCertificadoPdf(Long usuarioId, Long eventoId) {
        Evento evento = eventoRepository.findById(eventoId)
                .orElseThrow(() -> new RecursoNoEncontradoException(Mensajes.EVENTO_NO_ENCONTRADO));

        if (!Boolean.TRUE.equals(evento.getCertificable())) {
            throw new ConflictoException("Este evento no está configurado como certificable.");
        }

        Inscripcion inscripcion = inscripcionRepository
                .findByUsuarioIdAndEventoIdAndEstado(usuarioId, eventoId, EstadoInscripcion.ACTIVA)
                .orElseThrow(() -> new RecursoNoEncontradoException("No tienes una inscripción activa para este evento."));

        if (!asistenciaRepository.existsByInscripcionId(inscripcion.getId())) { 
            throw new ConflictoException("No se puede generar el certificado sin asistencia verificada.");
        }

        try {
            return construirPdf(inscripcion.getUsuario().getNombre(), evento);
        } catch (IOException e) {
            throw new IllegalStateException("Error al generar el certificado en PDF.", e);
        }
    }

    /** Certificado en una hoja A4 horizontal, con doble marco y el texto centrado */
    private byte[] construirPdf(String nombreUsuario, Evento evento) throws IOException {
        PDFont negrita = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);
        PDFont normal = new PDType1Font(Standard14Fonts.FontName.HELVETICA);
        PDRectangle hoja = new PDRectangle(PDRectangle.A4.getHeight(), PDRectangle.A4.getWidth());
        float ancho = hoja.getWidth();
        float alto = hoja.getHeight();

        try (PDDocument documento = new PDDocument()) {
            PDPage pagina = new PDPage(hoja);
            documento.addPage(pagina);

            try (PDPageContentStream contenido = new PDPageContentStream(documento, pagina)) {
                contenido.setLineWidth(3);
                contenido.addRect(25, 25, ancho - 50, alto - 50);
                contenido.stroke();
                contenido.setLineWidth(1);
                contenido.addRect(35, 35, ancho - 70, alto - 70);
                contenido.stroke();

                float y = alto - 150;
                centrar(contenido, negrita, 30, ancho, y, "CERTIFICADO DE ASISTENCIA");
                y -= 70;
                centrar(contenido, normal, 14, ancho, y, "EventU certifica que");
                y -= 50;
                centrar(contenido, negrita, 26, ancho, y, nombreUsuario);
                y -= 45;
                centrar(contenido, normal, 14, ancho, y, "asistió al evento");
                y -= 42;
                centrar(contenido, negrita, 20, ancho, y, evento.getTitulo());
                y -= 40;
                centrar(contenido, normal, 13, ancho, y, detalleDelEvento(evento));
                y -= 24;
                if (evento.getOrganizador() != null && evento.getOrganizador().getNombre() != null) {
                    centrar(contenido, normal, 13, ancho, y, "Organizado por " + evento.getOrganizador().getNombre());
                }

                centrar(contenido, normal, 10, ancho, 60,
                        "Fecha de emisión: " + LocalDate.now().format(FECHA_LARGA));
            }

            ByteArrayOutputStream salida = new ByteArrayOutputStream();
            documento.save(salida);
            return salida.toByteArray();
        }
    }

    /** "Realizado el 20 de noviembre de 2026 en Auditorio", omitiendo lo que no tenga el evento */
    private static String detalleDelEvento(Evento evento) {
        StringBuilder detalle = new StringBuilder("Realizado");
        if (evento.getFechaInicio() != null) {
            detalle.append(" el ").append(evento.getFechaInicio().format(FECHA_LARGA));
        }
        if (evento.getUbicacion() != null && !evento.getUbicacion().isBlank()) {
            detalle.append(" en ").append(evento.getUbicacion().trim());
        }
        return detalle.toString();
    }

    /** Escribe el texto centrado; si es más ancho que la hoja, reduce la letra hasta que quepa */
    private static void centrar(PDPageContentStream contenido, PDFont fuente, float tamano, float anchoHoja, float y,
                                String texto) throws IOException {
        String limpio = TextoPdf.compatible(texto, fuente);
        float tamanoFinal = TextoPdf.tamanoQueCabe(limpio, fuente, tamano, 9, anchoHoja - 120);
        float x = (anchoHoja - TextoPdf.ancho(limpio, fuente, tamanoFinal)) / 2;
        contenido.beginText();
        contenido.setFont(fuente, tamanoFinal);
        contenido.newLineAtOffset(x, y);
        contenido.showText(limpio);
        contenido.endText();
    }
}
