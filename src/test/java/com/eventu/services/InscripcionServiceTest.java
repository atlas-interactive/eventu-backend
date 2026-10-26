package com.eventu.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.eventu.exceptions.AccesoDenegadoException;
import com.eventu.exceptions.ConflictoException;
import com.eventu.exceptions.RecursoNoEncontradoException;
import com.eventu.models.EstadoEvento;
import com.eventu.models.EstadoInscripcion;
import com.eventu.models.Evento;
import com.eventu.models.Inscripcion;
import com.eventu.models.Rol;
import com.eventu.models.Usuario;
import com.eventu.repositories.AsistenciaRepository;
import com.eventu.repositories.EventoRepository;
import com.eventu.repositories.InscripcionRepository;

/** Pruebas de HU-10 (inscribirse en un evento). Cada prueba indica el escenario que cubre */
@ExtendWith(MockitoExtension.class)
class InscripcionServiceTest {

    private static final Long USUARIO_ID = 20L;
    private static final Long ADMIN_ID = 1L;
    private static final Long ORGANIZADOR_ID = 10L;
    private static final Long EVENTO_ID = 100L;

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

    // HU-10 escenario 1.0: inscripción con cupo disponible

    @Test
    void inscribir_conCupoDisponible_creaLaInscripcionDescuentaUnCupoYGeneraElQr() {
        Usuario usuario = usuario(USUARIO_ID, Rol.USUARIO);
        Evento evento = evento(10, 3);
        prepararInscripcion(usuario, evento);
        when(inscripcionRepository.existsByUsuarioIdAndEventoIdAndEstado(USUARIO_ID, EVENTO_ID, EstadoInscripcion.ACTIVA)).thenReturn(false);
        when(inscripcionRepository.save(any(Inscripcion.class))).thenAnswer(inv -> {
            Inscripcion i = inv.getArgument(0);
            i.setId(500L);
            return i;
        });

        Inscripcion creada = servicio.inscribirUsuario(USUARIO_ID, EVENTO_ID);

        assertEquals(EstadoInscripcion.ACTIVA, creada.getEstado());
        assertEquals(2, evento.getCuposDisponibles());
        assertNotNull(creada.getCodigoQr());
        assertTrue(creada.getCodigoQr().startsWith("EVENTU-QR-"));
        verify(eventoRepository).save(evento);
        verify(logService).registrarExito(USUARIO_ID, LogService.ACCION_INSCRIBIR, LogService.ENTIDAD_INSCRIPCION, 500L);
    }

    @Test
    void inscribir_unOrganizador_tambienPuedeInscribirse() {
        Usuario organizador = usuario(USUARIO_ID, Rol.ORGANIZADOR);
        Evento evento = evento(10, 3);
        prepararInscripcion(organizador, evento);
        when(inscripcionRepository.existsByUsuarioIdAndEventoIdAndEstado(USUARIO_ID, EVENTO_ID, EstadoInscripcion.ACTIVA)).thenReturn(false);
        when(inscripcionRepository.save(any(Inscripcion.class))).thenAnswer(inv -> inv.getArgument(0));

        Inscripcion creada = servicio.inscribirUsuario(USUARIO_ID, EVENTO_ID);

        assertEquals(organizador, creada.getUsuario());
    }

    // Regla: el organizador no puede inscribirse en su propio evento

    @Test
    void inscribir_elOrganizadorEnSuPropioEvento_lanzaAccesoDenegadoSinTocarCupos() {
        Usuario organizador = usuario(ORGANIZADOR_ID, Rol.ORGANIZADOR);
        Evento evento = evento(10, 3);
        prepararInscripcion(organizador, evento);

        assertThrows(AccesoDenegadoException.class, () -> servicio.inscribirUsuario(ORGANIZADOR_ID, EVENTO_ID));

        assertEquals(3, evento.getCuposDisponibles());
        verify(eventoRepository, never()).save(any(Evento.class));
        verify(inscripcionRepository, never()).save(any(Inscripcion.class));
        verify(logService).registrarFallo(ORGANIZADOR_ID, LogService.ACCION_INSCRIBIR, LogService.ENTIDAD_EVENTO,
                EVENTO_ID, "El organizador no puede inscribirse en su propio evento");
    }

    // HU-10 escenario 1.0: último cupo (control transaccional)

    @Test
    void inscribir_leeElEventoConBloqueoDeFilaParaQueElUltimoCupoLoGaneSoloUno() {
        Usuario usuario = usuario(USUARIO_ID, Rol.USUARIO);
        Evento evento = evento(10, 1);
        prepararInscripcion(usuario, evento);
        when(inscripcionRepository.existsByUsuarioIdAndEventoIdAndEstado(USUARIO_ID, EVENTO_ID, EstadoInscripcion.ACTIVA)).thenReturn(false);
        when(inscripcionRepository.save(any(Inscripcion.class))).thenAnswer(inv -> inv.getArgument(0));

        servicio.inscribirUsuario(USUARIO_ID, EVENTO_ID);

        assertEquals(0, evento.getCuposDisponibles());
        verify(eventoRepository).buscarPorIdConBloqueo(EVENTO_ID);
        verify(eventoRepository, never()).findById(any());
    }

    @Test
    void inscribir_cuandoElCupoSeAgotoPorLaOtraInscripcion_rechazaIndicandoQueNoHayCupos() {
        Usuario usuario = usuario(USUARIO_ID, Rol.USUARIO);
        prepararInscripcion(usuario, evento(10, 0));
        when(inscripcionRepository.existsByUsuarioIdAndEventoIdAndEstado(USUARIO_ID, EVENTO_ID, EstadoInscripcion.ACTIVA)).thenReturn(false);

        ConflictoException error = assertThrows(ConflictoException.class,
                () -> servicio.inscribirUsuario(USUARIO_ID, EVENTO_ID));

        assertTrue(error.getMessage().contains("cupos"));
        verify(inscripcionRepository, never()).save(any());
    }

    // HU-10 escenario 1.0: inscripción duplicada

    @Test
    void inscribir_conInscripcionActivaPrevia_rechazaYNoDescuentaCupo() {
        Usuario usuario = usuario(USUARIO_ID, Rol.USUARIO);
        Evento evento = evento(10, 3);
        prepararInscripcion(usuario, evento);
        when(inscripcionRepository.existsByUsuarioIdAndEventoIdAndEstado(USUARIO_ID, EVENTO_ID, EstadoInscripcion.ACTIVA)).thenReturn(true);

        assertThrows(ConflictoException.class, () -> servicio.inscribirUsuario(USUARIO_ID, EVENTO_ID));

        assertEquals(3, evento.getCuposDisponibles());
        verify(inscripcionRepository, never()).save(any());
        verify(logService).registrarFallo(eq(USUARIO_ID), eq(LogService.ACCION_INSCRIBIR),
                eq(LogService.ENTIDAD_EVENTO), eq(EVENTO_ID), anyString());
    }

    // HU-10 escenario 1.0: evento no disponible (cancelado)

    @Test
    void inscribir_enUnEventoCancelado_rechaza() {
        Evento evento = evento(10, 3);
        evento.setEstado(EstadoEvento.CANCELADO);
        prepararInscripcion(usuario(USUARIO_ID, Rol.USUARIO), evento);

        assertThrows(ConflictoException.class, () -> servicio.inscribirUsuario(USUARIO_ID, EVENTO_ID));

        verify(inscripcionRepository, never()).save(any());
        verify(logService).registrarFallo(eq(USUARIO_ID), eq(LogService.ACCION_INSCRIBIR),
                eq(LogService.ENTIDAD_EVENTO), eq(EVENTO_ID), anyString());
    }

    // HU-07 escenario 1.0 (regla de inscripción): evento que ya inició

    @Test
    void inscribir_enUnEventoQueYaInicio_rechazaIndicandoQueYaInicio() {
        Evento evento = evento(10, 3);
        evento.setFechaInicio(LocalDateTime.now().minusHours(1));
        prepararInscripcion(usuario(USUARIO_ID, Rol.USUARIO), evento);

        ConflictoException error = assertThrows(ConflictoException.class,
                () -> servicio.inscribirUsuario(USUARIO_ID, EVENTO_ID));

        assertTrue(error.getMessage().contains("ya inició"));
        assertEquals(3, evento.getCuposDisponibles());
        verify(inscripcionRepository, never()).save(any());
        verify(logService).registrarFallo(eq(USUARIO_ID), eq(LogService.ACCION_INSCRIBIR),
                eq(LogService.ENTIDAD_EVENTO), eq(EVENTO_ID), anyString());
    }

    // HU-10 escenario 1.0: usuario con rol ADMIN

    @Test
    void inscribir_unAdministrador_lanzaAccesoDenegadoSinTocarElEvento() {
        when(autorizacionService.obtenerUsuario(ADMIN_ID)).thenReturn(usuario(ADMIN_ID, Rol.ADMIN));

        assertThrows(AccesoDenegadoException.class, () -> servicio.inscribirUsuario(ADMIN_ID, EVENTO_ID));

        verify(eventoRepository, never()).buscarPorIdConBloqueo(any());
        verify(inscripcionRepository, never()).save(any());
        verify(logService).registrarFallo(eq(ADMIN_ID), eq(LogService.ACCION_INSCRIBIR),
                eq(LogService.ENTIDAD_EVENTO), eq(EVENTO_ID), anyString());
    }

    @Test
    void inscribir_enUnEventoInexistente_lanzaNoEncontrado() {
        when(autorizacionService.obtenerUsuario(USUARIO_ID)).thenReturn(usuario(USUARIO_ID, Rol.USUARIO));
        when(eventoRepository.buscarPorIdConBloqueo(EVENTO_ID)).thenReturn(Optional.empty());

        assertThrows(RecursoNoEncontradoException.class, () -> servicio.inscribirUsuario(USUARIO_ID, EVENTO_ID));
    }

    // HU-12 escenario 2.0: reinscripción tras cancelar (la restricción solo aplica a inscripciones activas)

    @Test
    void inscribir_trasHaberCanceladoAntes_lePermiteInscribirseDeNuevo() {
        Usuario usuario = usuario(USUARIO_ID, Rol.USUARIO);
        prepararInscripcion(usuario, evento(10, 3));
        // La inscripción anterior está CANCELADA: no hay ninguna ACTIVA
        when(inscripcionRepository.existsByUsuarioIdAndEventoIdAndEstado(USUARIO_ID, EVENTO_ID, EstadoInscripcion.ACTIVA)).thenReturn(false);
        when(inscripcionRepository.save(any(Inscripcion.class))).thenAnswer(inv -> inv.getArgument(0));

        Inscripcion nueva = servicio.inscribirUsuario(USUARIO_ID, EVENTO_ID);

        assertEquals(EstadoInscripcion.ACTIVA, nueva.getEstado());
    }

    // Utilidades

    private void prepararInscripcion(Usuario usuario, Evento evento) {
        when(autorizacionService.obtenerUsuario(usuario.getId())).thenReturn(usuario);
        when(eventoRepository.buscarPorIdConBloqueo(EVENTO_ID)).thenReturn(Optional.of(evento));
    }

    private static Usuario usuario(Long id, Rol rol) {
        Usuario usuario = new Usuario();
        usuario.setId(id);
        usuario.setRol(rol);
        return usuario;
    }

    /** Evento publicado, con inscripciones abiertas y que empieza dentro de diez días */
    private static Evento evento(int maximos, int disponibles) {
        Evento evento = new Evento();
        evento.setId(EVENTO_ID);
        evento.setTitulo("Evento");
        evento.setOrganizador(usuario(ORGANIZADOR_ID, Rol.ORGANIZADOR));
        evento.setEstado(EstadoEvento.PUBLICADO);
        evento.setFechaInicio(LocalDateTime.now().plusDays(10));
        evento.setCuposMaximos(maximos);
        evento.setCuposDisponibles(disponibles);
        return evento;
    }
}
