package com.eventu.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.eventu.dto.InscritoResponseDTO;
import com.eventu.exceptions.AccesoDenegadoException;
import com.eventu.exceptions.RecursoNoEncontradoException;
import com.eventu.models.Evento;
import com.eventu.repositories.EventoRepository;
import com.eventu.util.Mensajes;

/** Pruebas de HU-17 (reporte en PDF de inscritos y asistentes). Cada prueba indica el escenario que cubre */
@ExtendWith(MockitoExtension.class)
class ReporteServiceTest {

    private static final Long ORGANIZADOR_ID = 10L;
    private static final Long OTRO_ID = 11L;
    private static final Long EVENTO_ID = 100L;

    @Mock private InscripcionService inscripcionService;
    @Mock private EventoRepository eventoRepository;
    @Mock private LogService logService;

    private ReporteService servicio;

    @BeforeEach
    void preparar() {
        servicio = new ReporteService(inscripcionService, eventoRepository, logService);
    }

    // HU-17 escenario 1.0: evento propio

    @Test
    void generarReporte_deUnEventoPropio_incluyeLosInscritosSuAsistenciaYElResumen() throws IOException {
        when(inscripcionService.listarInscritosDeEvento(EVENTO_ID, ORGANIZADOR_ID)).thenReturn(List.of(
                inscrito(1L, "María Peñaloza", "maria@unillanos.edu.co", true),
                inscrito(2L, "Luis Gómez", "luis@unillanos.edu.co", false)));
        when(eventoRepository.findById(EVENTO_ID)).thenReturn(Optional.of(evento()));

        String texto = textoDe(servicio.generarReporteInscritos(EVENTO_ID, ORGANIZADOR_ID));

        assertTrue(texto.contains("Congreso de Ingeniería"));
        assertTrue(texto.contains("María Peñaloza maria@unillanos.edu.co Sí"));
        assertTrue(texto.contains("Luis Gómez luis@unillanos.edu.co No"));
        assertTrue(texto.contains("Inscritos activos: 2"));
        assertTrue(texto.contains("Asistentes: 1"));
        assertTrue(texto.contains("50,00 %"));
        assertFalse(texto.contains(ReporteService.SIN_REGISTROS));
        verify(logService).registrarExito(ORGANIZADOR_ID, LogService.ACCION_GENERAR_REPORTE,
                LogService.ENTIDAD_EVENTO, EVENTO_ID);
    }

    @Test
    void generarReporte_conMuchosInscritos_reparteLaTablaEnVariasPaginasSinPerderFilas() throws IOException {
        List<InscritoResponseDTO> inscritos = new ArrayList<>();
        for (long i = 1; i <= 80; i++) {
            inscritos.add(inscrito(i, "Persona " + i, "p" + i + "@unillanos.edu.co", i % 2 == 0));
        }
        when(inscripcionService.listarInscritosDeEvento(EVENTO_ID, ORGANIZADOR_ID)).thenReturn(inscritos);
        when(eventoRepository.findById(EVENTO_ID)).thenReturn(Optional.of(evento()));

        byte[] pdf = servicio.generarReporteInscritos(EVENTO_ID, ORGANIZADOR_ID);

        try (PDDocument documento = Loader.loadPDF(pdf)) {
            assertTrue(documento.getNumberOfPages() > 1);
            String texto = new PDFTextStripper().getText(documento);
            assertTrue(texto.contains("Persona 1 "));
            assertTrue(texto.contains("Persona 80 "));
            assertTrue(texto.contains("Página " + documento.getNumberOfPages() + " de " + documento.getNumberOfPages()));
        }
    }

    @Test
    void generarReporte_conCaracteresQueLaFuenteNoTiene_losReemplazaEnLugarDeFallar() throws IOException {
        when(inscripcionService.listarInscritosDeEvento(EVENTO_ID, ORGANIZADOR_ID)).thenReturn(List.of(
                inscrito(1L, "Ana 😀", "ana@unillanos.edu.co", true)));
        when(eventoRepository.findById(EVENTO_ID)).thenReturn(Optional.of(evento()));

        String texto = textoDe(servicio.generarReporteInscritos(EVENTO_ID, ORGANIZADOR_ID));

        assertTrue(texto.contains("Ana ?"));
    }

    // HU-17 escenario 2.0: evento ajeno

    @Test
    void generarReporte_deUnEventoAjeno_lanzaAccesoDenegadoSinGenerarNiRegistrarExito() {
        doThrow(new AccesoDenegadoException(Mensajes.EVENTO_AJENO))
                .when(inscripcionService).listarInscritosDeEvento(EVENTO_ID, OTRO_ID);

        AccesoDenegadoException error = assertThrows(AccesoDenegadoException.class,
                () -> servicio.generarReporteInscritos(EVENTO_ID, OTRO_ID));

        assertEquals(Mensajes.EVENTO_AJENO, error.getMessage());
        verifyNoInteractions(eventoRepository);
        verify(logService, never()).registrarExito(anyLong(), anyString(), anyString(), anyLong());
    }

    // HU-17 escenario 3.0: evento sin datos

    @Test
    void generarReporte_deUnEventoSinInscritos_generaElPdfIndicandoQueNoHayRegistros() throws IOException {
        when(inscripcionService.listarInscritosDeEvento(EVENTO_ID, ORGANIZADOR_ID)).thenReturn(List.of());
        when(eventoRepository.findById(EVENTO_ID)).thenReturn(Optional.of(evento()));

        String texto = textoDe(servicio.generarReporteInscritos(EVENTO_ID, ORGANIZADOR_ID));

        assertTrue(texto.contains(ReporteService.SIN_REGISTROS));
        assertTrue(texto.contains("Inscritos activos: 0"));
        assertTrue(texto.contains("0,00 %"));
    }

    // Evento inexistente

    @Test
    void generarReporte_deUnEventoInexistente_lanzaRecursoNoEncontrado() {
        doThrow(new RecursoNoEncontradoException(Mensajes.EVENTO_NO_ENCONTRADO))
                .when(inscripcionService).listarInscritosDeEvento(EVENTO_ID, ORGANIZADOR_ID);

        assertThrows(RecursoNoEncontradoException.class,
                () -> servicio.generarReporteInscritos(EVENTO_ID, ORGANIZADOR_ID));

        verifyNoInteractions(eventoRepository, logService);
    }

    private static String textoDe(byte[] pdf) throws IOException {
        try (PDDocument documento = Loader.loadPDF(pdf)) {
            return new PDFTextStripper().getText(documento);
        }
    }

    private static Evento evento() {
        Evento evento = new Evento();
        evento.setId(EVENTO_ID);
        evento.setTitulo("Congreso de Ingeniería");
        evento.setFechaInicio(LocalDateTime.of(2026, 11, 20, 8, 0));
        evento.setUbicacion("Auditorio");
        return evento;
    }

    private static InscritoResponseDTO inscrito(Long id, String nombre, String correo, boolean asistio) {
        return new InscritoResponseDTO(id, id, nombre, correo, LocalDateTime.of(2026, 10, 1, 9, 0), asistio);
    }
}
