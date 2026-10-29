package com.eventu.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Optional;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.eventu.exceptions.ConflictoException;
import com.eventu.exceptions.RecursoNoEncontradoException;
import com.eventu.models.EstadoInscripcion;
import com.eventu.models.Evento;
import com.eventu.models.Inscripcion;
import com.eventu.models.Rol;
import com.eventu.models.Usuario;
import com.eventu.repositories.AsistenciaRepository;
import com.eventu.repositories.EventoRepository;
import com.eventu.repositories.InscripcionRepository;

/** Pruebas de HU-18 (certificado de asistencia en PDF). Cada prueba indica el escenario que cubre */
@ExtendWith(MockitoExtension.class)
class CertificadoServiceTest {

    private static final Long USUARIO_ID = 20L;
    private static final Long EVENTO_ID = 100L;
    private static final Long INSCRIPCION_ID = 500L;

    @Mock private EventoRepository eventoRepository;
    @Mock private InscripcionRepository inscripcionRepository;
    @Mock private AsistenciaRepository asistenciaRepository;

    private CertificadoService servicio;

    @BeforeEach
    void preparar() {
        servicio = new CertificadoService(eventoRepository, inscripcionRepository, asistenciaRepository);
    }

    // HU-18 escenario 1.0: evento certificable y asistencia verificada

    @Test
    void generarCertificado_conAsistenciaVerificada_incluyeElNombreYLosDatosDelEvento() throws IOException {
        Evento evento = evento(true);
        when(eventoRepository.findById(EVENTO_ID)).thenReturn(Optional.of(evento));
        when(inscripcionRepository.findByUsuarioIdAndEventoIdAndEstado(USUARIO_ID, EVENTO_ID, EstadoInscripcion.ACTIVA))
                .thenReturn(Optional.of(inscripcion(evento)));
        when(asistenciaRepository.existsByInscripcionId(INSCRIPCION_ID)).thenReturn(true);

        String texto = textoDe(servicio.generarCertificadoPdf(USUARIO_ID, EVENTO_ID));

        assertTrue(texto.contains("CERTIFICADO DE ASISTENCIA"));
        assertTrue(texto.contains("María Peñaloza"));
        assertTrue(texto.contains("asistió al evento"));
        assertTrue(texto.contains("Congreso de Ingeniería"));
        assertTrue(texto.contains("20 de noviembre de 2026"));
        assertTrue(texto.contains("Auditorio"));
        assertTrue(texto.contains("Organizado por Martín Pineda"));
    }

    // HU-18 escenario 1.0: evento no certificable

    @Test
    void generarCertificado_deUnEventoNoCertificable_lanzaConflictoSinBuscarLaInscripcion() {
        when(eventoRepository.findById(EVENTO_ID)).thenReturn(Optional.of(evento(false)));

        ConflictoException error = assertThrows(ConflictoException.class,
                () -> servicio.generarCertificadoPdf(USUARIO_ID, EVENTO_ID));

        assertEquals("Este evento no está configurado como certificable.", error.getMessage());
        verifyNoInteractions(inscripcionRepository, asistenciaRepository);
    }

    // HU-18 escenario 1.0: asistencia no verificada

    @Test
    void generarCertificado_sinAsistenciaRegistrada_lanzaConflicto() {
        Evento evento = evento(true);
        when(eventoRepository.findById(EVENTO_ID)).thenReturn(Optional.of(evento));
        when(inscripcionRepository.findByUsuarioIdAndEventoIdAndEstado(USUARIO_ID, EVENTO_ID, EstadoInscripcion.ACTIVA))
                .thenReturn(Optional.of(inscripcion(evento)));
        when(asistenciaRepository.existsByInscripcionId(INSCRIPCION_ID)).thenReturn(false);

        ConflictoException error = assertThrows(ConflictoException.class,
                () -> servicio.generarCertificadoPdf(USUARIO_ID, EVENTO_ID));

        assertEquals("No se puede generar el certificado sin asistencia verificada.", error.getMessage());
    }

    // Sin inscripción activa en el evento

    @Test
    void generarCertificado_sinInscripcionActiva_lanzaRecursoNoEncontrado() {
        when(eventoRepository.findById(EVENTO_ID)).thenReturn(Optional.of(evento(true)));
        when(inscripcionRepository.findByUsuarioIdAndEventoIdAndEstado(USUARIO_ID, EVENTO_ID, EstadoInscripcion.ACTIVA))
                .thenReturn(Optional.empty());

        assertThrows(RecursoNoEncontradoException.class, () -> servicio.generarCertificadoPdf(USUARIO_ID, EVENTO_ID));

        verifyNoInteractions(asistenciaRepository);
    }

    // Evento inexistente

    @Test
    void generarCertificado_deUnEventoInexistente_lanzaRecursoNoEncontrado() {
        when(eventoRepository.findById(EVENTO_ID)).thenReturn(Optional.empty());

        assertThrows(RecursoNoEncontradoException.class, () -> servicio.generarCertificadoPdf(USUARIO_ID, EVENTO_ID));

        verifyNoInteractions(inscripcionRepository, asistenciaRepository);
    }

    private static String textoDe(byte[] pdf) throws IOException {
        try (PDDocument documento = Loader.loadPDF(pdf)) {
            return new PDFTextStripper().getText(documento);
        }
    }

    private static Evento evento(boolean certificable) {
        Usuario organizador = new Usuario();
        organizador.setId(10L);
        organizador.setRol(Rol.ORGANIZADOR);
        organizador.setNombre("Martín Pineda");
        Evento evento = new Evento();
        evento.setId(EVENTO_ID);
        evento.setTitulo("Congreso de Ingeniería");
        evento.setFechaInicio(LocalDateTime.of(2026, 11, 20, 8, 0));
        evento.setUbicacion("Auditorio");
        evento.setCertificable(certificable);
        evento.setOrganizador(organizador);
        return evento;
    }

    private static Inscripcion inscripcion(Evento evento) {
        Usuario usuario = new Usuario();
        usuario.setId(USUARIO_ID);
        usuario.setNombre("María Peñaloza");
        Inscripcion inscripcion = new Inscripcion();
        inscripcion.setId(INSCRIPCION_ID);
        inscripcion.setUsuario(usuario);
        inscripcion.setEvento(evento);
        inscripcion.setEstado(EstadoInscripcion.ACTIVA);
        return inscripcion;
    }
}
