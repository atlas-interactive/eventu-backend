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
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/** Generación del certificado de asistencia en PDF*/
@Service
public class CertificadoService {

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
            return construirPdf(inscripcion.getUsuario().getNombre(), evento.getTitulo(), evento.getFechaInicio());
        } catch (IOException e) {
            throw new IllegalStateException("Error al generar el certificado en PDF.", e);
        }
    }

    private byte[] construirPdf(String nombreUsuario, String tituloEvento, LocalDateTime fechaEvento) throws IOException {
        try (PDDocument document = new PDDocument()) {
            PDPage page = new PDPage(PDRectangle.A4);
            document.addPage(page);

            try (PDPageContentStream content = new PDPageContentStream(document, page)) {
                PDType1Font tituloFont = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);
                PDType1Font textoFont = new PDType1Font(Standard14Fonts.FontName.HELVETICA);

                content.beginText();
                content.setFont(tituloFont, 22);
                content.newLineAtOffset(80, 720);
                content.showText("Certificado de Participacion");
                content.endText();

                content.beginText();
                content.setFont(textoFont, 13);
                content.newLineAtOffset(80, 660);
                content.showText("EventU certifica que");
                content.endText();

                content.beginText();
                content.setFont(tituloFont, 16);
                content.newLineAtOffset(80, 630);
                content.showText(nombreUsuario);
                content.endText();

                content.beginText();
                content.setFont(textoFont, 13);
                content.newLineAtOffset(80, 600);
                content.showText("asistio y participo en el evento:");
                content.endText();

                content.beginText();
                content.setFont(tituloFont, 15);
                content.newLineAtOffset(80, 570);
                content.showText(tituloEvento);
                content.endText();

                content.beginText();
                content.setFont(textoFont, 12);
                content.newLineAtOffset(80, 540);
                content.showText("Fecha: " + fechaEvento.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")));
                content.endText();
            }

            ByteArrayOutputStream salida = new ByteArrayOutputStream();
            document.save(salida);
            return salida.toByteArray();
        }
    }
}