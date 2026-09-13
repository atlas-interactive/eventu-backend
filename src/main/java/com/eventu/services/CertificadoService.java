package com.eventu.services;

import com.eventu.models.EstadoInscripcion;
import com.eventu.models.Evento;
import com.eventu.models.Inscripcion;
import com.eventu.repositories.AsistenciaRepository;
import com.eventu.repositories.EventoRepository;
import com.eventu.repositories.InscripcionRepository;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

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

    public byte[] generarCertificadoPdf(Long estudianteId, Long eventoId) {
        Evento evento = eventoRepository.findById(eventoId)
                .orElseThrow(() -> new RuntimeException("Evento no encontrado."));

        if (evento.getCertificable() == null || !evento.getCertificable()) {
            throw new RuntimeException("RN15: Este evento no esta configurado como certificable.");
        }

        Inscripcion inscripcion = inscripcionRepository.findByEstudianteIdAndEventoIdAndEstado(estudianteId, eventoId, EstadoInscripcion.ACTIVA).orElseThrow(() -> new RuntimeException("No tienes una inscripcion activa para este evento."));
        boolean asistio = asistenciaRepository.existsByInscripcionId(inscripcion.getId());
        if (!asistio) {
            throw new RuntimeException("RN15: No se puede generar el certificado sin asistencia verificada.");
        }

        try {
            return construirPdf(inscripcion.getEstudiante().getNombre(), evento.getTitulo(), evento.getFechaInicio());
        } catch (IOException e) {
            throw new RuntimeException("Error al generar el certificado en PDF.", e);
        }
    }

    private byte[] construirPdf(String nombreEstudiante, String tituloEvento, LocalDateTime fechaEvento) throws IOException {
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
                content.showText(nombreEstudiante);
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