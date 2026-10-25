package com.eventu.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.eventu.exceptions.AccesoDenegadoException;
import com.eventu.exceptions.ConflictoException;
import com.eventu.exceptions.RecursoNoEncontradoException;
import com.eventu.models.EstadoInscripcion;
import com.eventu.models.Evento;
import com.eventu.models.Inscripcion;
import com.eventu.models.Usuario;
import com.eventu.repositories.AsistenciaRepository;
import com.eventu.repositories.EventoRepository;
import com.eventu.repositories.InscripcionRepository;

/** Pruebas de HU-12 (cancelar inscripción). Cada prueba indica el escenario que cubre */
@ExtendWith(MockitoExtension.class)
class CancelarInscripcionServiceTest {

    private static final Long USUARIO_ID = 20L;
    private static final Long OTRO_ID = 21L;
    private static final Long EVENTO_ID = 100L;
    private static final Long INSCRIPCION_ID = 300L;

    @Mock private InscripcionRepository inscripcionRepository;
    @Mock private EventoRepository eventoRepository;
    @Mock private AsistenciaRepository asistenciaRepository;
    @Mock private AutorizacionService autorizacionService;
    @Mock private LogService logService;

    private InscripcionService servicio;

    @BeforeEach
    void preparar() {
        servicio = new InscripcionService(inscripcionRepository, eventoRepository, asistenciaRepository,
                autorizacionService, logService);
    }

    // Escenario: cancelación exitosa

    @Test
    void cancelar_inscripcionActiva_laMarcaCanceladaLiberaElCupoYRegistraElLog() {
        Evento evento = evento(4);
        Inscripcion inscripcion = inscripcion(USUARIO_ID, evento, EstadoInscripcion.ACTIVA);
        when(inscripcionRepository.findById(INSCRIPCION_ID)).thenReturn(Optional.of(inscripcion));
        when(asistenciaRepository.existsByInscripcionId(INSCRIPCION_ID)).thenReturn(false);
        when(eventoRepository.buscarPorIdConBloqueo(EVENTO_ID)).thenReturn(Optional.of(evento));
        when(inscripcionRepository.save(any(Inscripcion.class))).thenAnswer(inv -> inv.getArgument(0));

        Inscripcion resultado = servicio.cancelarInscripcion(INSCRIPCION_ID, USUARIO_ID);

        assertEquals(EstadoInscripcion.CANCELADA, resultado.getEstado());
        assertEquals(5, evento.getCuposDisponibles());
        verify(eventoRepository).save(evento);
        verify(logService).registrarExito(USUARIO_ID, LogService.ACCION_CANCELAR_INSCRIPCION,
                LogService.ENTIDAD_INSCRIPCION, INSCRIPCION_ID);
    }

    // Escenario: la inscripción no existe

    @Test
    void cancelar_inscripcionInexistente_lanzaRecursoNoEncontradoYNoTocaCupos() {
        when(inscripcionRepository.findById(INSCRIPCION_ID)).thenReturn(Optional.empty());

        assertThrows(RecursoNoEncontradoException.class,
                () -> servicio.cancelarInscripcion(INSCRIPCION_ID, USUARIO_ID));

        verify(eventoRepository, never()).save(any(Evento.class));
        verify(inscripcionRepository, never()).save(any(Inscripcion.class));
    }

    // Escenario: intentar cancelar la inscripción de otro usuario

    @Test
    void cancelar_inscripcionDeOtroUsuario_lanzaAccesoDenegadoSinCambiosYRegistraElFallo() {
        Evento evento = evento(4);
        Inscripcion inscripcion = inscripcion(OTRO_ID, evento, EstadoInscripcion.ACTIVA);
        when(inscripcionRepository.findById(INSCRIPCION_ID)).thenReturn(Optional.of(inscripcion));

        assertThrows(AccesoDenegadoException.class,
                () -> servicio.cancelarInscripcion(INSCRIPCION_ID, USUARIO_ID));

        assertEquals(EstadoInscripcion.ACTIVA, inscripcion.getEstado());
        assertEquals(4, evento.getCuposDisponibles());
        verify(eventoRepository, never()).save(any(Evento.class));
        verify(logService).registrarFallo(USUARIO_ID, LogService.ACCION_CANCELAR_INSCRIPCION,
                LogService.ENTIDAD_INSCRIPCION, INSCRIPCION_ID, "Inscripción de otro usuario");
    }

    // Escenario: cancelar una inscripción que ya estaba cancelada

    @Test
    void cancelar_inscripcionYaCancelada_lanzaConflictoYNoLiberaOtroCupo() {
        Evento evento = evento(5);
        Inscripcion inscripcion = inscripcion(USUARIO_ID, evento, EstadoInscripcion.CANCELADA);
        when(inscripcionRepository.findById(INSCRIPCION_ID)).thenReturn(Optional.of(inscripcion));

        assertThrows(ConflictoException.class,
                () -> servicio.cancelarInscripcion(INSCRIPCION_ID, USUARIO_ID));

        assertEquals(5, evento.getCuposDisponibles());
        verify(eventoRepository, never()).save(any(Evento.class));
        verify(logService).registrarFallo(USUARIO_ID, LogService.ACCION_CANCELAR_INSCRIPCION,
                LogService.ENTIDAD_INSCRIPCION, INSCRIPCION_ID, "Inscripción ya cancelada");
    }

    // Escenario: no se puede cancelar si ya hay asistencia registrada

    @Test
    void cancelar_conAsistenciaRegistrada_lanzaConflictoYConservaLaInscripcionActiva() {
        Evento evento = evento(4);
        Inscripcion inscripcion = inscripcion(USUARIO_ID, evento, EstadoInscripcion.ACTIVA);
        when(inscripcionRepository.findById(INSCRIPCION_ID)).thenReturn(Optional.of(inscripcion));
        when(asistenciaRepository.existsByInscripcionId(INSCRIPCION_ID)).thenReturn(true);

        assertThrows(ConflictoException.class,
                () -> servicio.cancelarInscripcion(INSCRIPCION_ID, USUARIO_ID));

        assertEquals(EstadoInscripcion.ACTIVA, inscripcion.getEstado());
        assertEquals(4, evento.getCuposDisponibles());
        verify(eventoRepository, never()).save(any(Evento.class));
        verify(logService).registrarFallo(USUARIO_ID, LogService.ACCION_CANCELAR_INSCRIPCION,
                LogService.ENTIDAD_INSCRIPCION, INSCRIPCION_ID, "Asistencia ya registrada");
    }

    // Escenario: el evento de la inscripción ya no existe al liberar el cupo

    @Test
    void cancelar_siElEventoDesaparece_lanzaRecursoNoEncontradoSinGuardarLaInscripcion() {
        Evento evento = evento(4);
        Inscripcion inscripcion = inscripcion(USUARIO_ID, evento, EstadoInscripcion.ACTIVA);
        when(inscripcionRepository.findById(INSCRIPCION_ID)).thenReturn(Optional.of(inscripcion));
        when(asistenciaRepository.existsByInscripcionId(INSCRIPCION_ID)).thenReturn(false);
        when(eventoRepository.buscarPorIdConBloqueo(EVENTO_ID)).thenReturn(Optional.empty());

        assertThrows(RecursoNoEncontradoException.class,
                () -> servicio.cancelarInscripcion(INSCRIPCION_ID, USUARIO_ID));

        verify(inscripcionRepository, never()).save(any(Inscripcion.class));
        verify(logService, never()).registrarExito(any(), anyString(), anyString(), any());
    }

    private static Evento evento(int cuposDisponibles) {
        Evento evento = new Evento();
        evento.setId(EVENTO_ID);
        evento.setCuposDisponibles(cuposDisponibles);
        return evento;
    }

    private static Inscripcion inscripcion(Long duenoId, Evento evento, EstadoInscripcion estado) {
        Usuario dueno = new Usuario();
        dueno.setId(duenoId);
        Inscripcion inscripcion = new Inscripcion();
        inscripcion.setId(INSCRIPCION_ID);
        inscripcion.setUsuario(dueno);
        inscripcion.setEvento(evento);
        inscripcion.setEstado(estado);
        return inscripcion;
    }
}
