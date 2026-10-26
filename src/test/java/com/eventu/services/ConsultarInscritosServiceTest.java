package com.eventu.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.eventu.dto.InscritoResponseDTO;
import com.eventu.exceptions.AccesoDenegadoException;
import com.eventu.exceptions.RecursoNoEncontradoException;
import com.eventu.models.EstadoInscripcion;
import com.eventu.models.Evento;
import com.eventu.models.Inscripcion;
import com.eventu.models.Rol;
import com.eventu.models.Usuario;
import com.eventu.repositories.AsistenciaRepository;
import com.eventu.repositories.EventoRepository;
import com.eventu.repositories.InscripcionRepository;
import com.eventu.util.Mensajes;

/** Pruebas de HU-13 (consulta de participantes inscritos). Cada prueba indica el escenario que cubre */
@ExtendWith(MockitoExtension.class)
class ConsultarInscritosServiceTest {

    private static final Long ORGANIZADOR_ID = 10L;
    private static final Long OTRO_ID = 11L;
    private static final Long ADMIN_ID = 1L;
    private static final Long USUARIO_ID = 20L;
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

    // HU-13 escenario 1.0: evento propio

    @Test
    void listarInscritos_enUnEventoPropio_muestraLosInscritosYSuEstadoDeAsistencia() {
        Usuario organizador = usuario(ORGANIZADOR_ID, Rol.ORGANIZADOR, "Organizador");
        Evento evento = evento(organizador);
        when(autorizacionService.obtenerGestorDeEventos(ORGANIZADOR_ID)).thenReturn(organizador);
        when(eventoRepository.findById(EVENTO_ID)).thenReturn(Optional.of(evento));
        when(autorizacionService.puedeModificarEvento(organizador, evento)).thenReturn(true);
        when(asistenciaRepository.findInscripcionIdsConAsistenciaPorEvento(EVENTO_ID)).thenReturn(List.of(500L));
        when(inscripcionRepository.findByEventoIdAndEstado(EVENTO_ID, EstadoInscripcion.ACTIVA)).thenReturn(List.of(
                inscripcion(500L, usuario(USUARIO_ID, Rol.USUARIO, "Ana"), evento),
                inscripcion(501L, usuario(21L, Rol.USUARIO, "Luis"), evento)));

        List<InscritoResponseDTO> inscritos = servicio.listarInscritosDeEvento(EVENTO_ID, ORGANIZADOR_ID);

        assertEquals(2, inscritos.size());
        assertEquals("Ana", inscritos.get(0).nombre());
        assertTrue(inscritos.get(0).asistio());
        assertEquals("Luis", inscritos.get(1).nombre());
        assertFalse(inscritos.get(1).asistio());
    }

    // HU-13 escenario 2.0: evento ajeno

    @Test
    void listarInscritos_enUnEventoDeOtroOrganizador_lanzaAccesoDenegadoSinConsultarInscripciones() {
        Usuario otro = usuario(OTRO_ID, Rol.ORGANIZADOR, "Otro");
        Evento evento = evento(usuario(ORGANIZADOR_ID, Rol.ORGANIZADOR, "Organizador"));
        when(autorizacionService.obtenerGestorDeEventos(OTRO_ID)).thenReturn(otro);
        when(eventoRepository.findById(EVENTO_ID)).thenReturn(Optional.of(evento));
        when(autorizacionService.puedeModificarEvento(otro, evento)).thenReturn(false);

        AccesoDenegadoException error = assertThrows(AccesoDenegadoException.class,
                () -> servicio.listarInscritosDeEvento(EVENTO_ID, OTRO_ID));

        assertEquals(Mensajes.EVENTO_AJENO, error.getMessage());
        verifyNoInteractions(inscripcionRepository, asistenciaRepository);
        verify(logService).registrarFallo(OTRO_ID, LogService.ACCION_CONSULTAR_INSCRITOS, LogService.ENTIDAD_EVENTO,
                EVENTO_ID, "Intento de consultar los inscritos de un evento ajeno");
    }

    @Test
    void listarInscritos_comoAdministradorEnUnEventoAjeno_permiteLaConsulta() {
        Usuario admin = usuario(ADMIN_ID, Rol.ADMIN, "Admin");
        Evento evento = evento(usuario(ORGANIZADOR_ID, Rol.ORGANIZADOR, "Organizador"));
        when(autorizacionService.obtenerGestorDeEventos(ADMIN_ID)).thenReturn(admin);
        when(eventoRepository.findById(EVENTO_ID)).thenReturn(Optional.of(evento));
        when(autorizacionService.puedeModificarEvento(admin, evento)).thenReturn(true);
        when(asistenciaRepository.findInscripcionIdsConAsistenciaPorEvento(EVENTO_ID)).thenReturn(List.of());
        when(inscripcionRepository.findByEventoIdAndEstado(EVENTO_ID, EstadoInscripcion.ACTIVA)).thenReturn(List.of(
                inscripcion(500L, usuario(USUARIO_ID, Rol.USUARIO, "Ana"), evento)));

        List<InscritoResponseDTO> inscritos = servicio.listarInscritosDeEvento(EVENTO_ID, ADMIN_ID);

        assertEquals(1, inscritos.size());
        verify(logService, never()).registrarFallo(ADMIN_ID, LogService.ACCION_CONSULTAR_INSCRITOS,
                LogService.ENTIDAD_EVENTO, EVENTO_ID, "Intento de consultar los inscritos de un evento ajeno");
    }

    @Test
    void listarInscritos_siElUsuarioNoPuedeGestionarEventos_lanzaAccesoDenegadoSinBuscarElEvento() {
        doThrow(new AccesoDenegadoException(Mensajes.SIN_PERMISO_GESTIONAR_EVENTOS))
                .when(autorizacionService).obtenerGestorDeEventos(USUARIO_ID);

        assertThrows(AccesoDenegadoException.class, () -> servicio.listarInscritosDeEvento(EVENTO_ID, USUARIO_ID));

        verifyNoInteractions(eventoRepository, inscripcionRepository, asistenciaRepository);
    }

    // HU-13 escenario 3.0: evento sin inscritos

    @Test
    void listarInscritos_enUnEventoSinInscripciones_devuelveElListadoVacio() {
        Usuario organizador = usuario(ORGANIZADOR_ID, Rol.ORGANIZADOR, "Organizador");
        Evento evento = evento(organizador);
        when(autorizacionService.obtenerGestorDeEventos(ORGANIZADOR_ID)).thenReturn(organizador);
        when(eventoRepository.findById(EVENTO_ID)).thenReturn(Optional.of(evento));
        when(autorizacionService.puedeModificarEvento(organizador, evento)).thenReturn(true);
        when(asistenciaRepository.findInscripcionIdsConAsistenciaPorEvento(EVENTO_ID)).thenReturn(List.of());
        when(inscripcionRepository.findByEventoIdAndEstado(EVENTO_ID, EstadoInscripcion.ACTIVA)).thenReturn(List.of());

        assertTrue(servicio.listarInscritosDeEvento(EVENTO_ID, ORGANIZADOR_ID).isEmpty());
    }

    // Evento inexistente

    @Test
    void listarInscritos_enUnEventoInexistente_lanzaRecursoNoEncontrado() {
        Usuario organizador = usuario(ORGANIZADOR_ID, Rol.ORGANIZADOR, "Organizador");
        when(autorizacionService.obtenerGestorDeEventos(ORGANIZADOR_ID)).thenReturn(organizador);
        when(eventoRepository.findById(EVENTO_ID)).thenReturn(Optional.empty());

        assertThrows(RecursoNoEncontradoException.class,
                () -> servicio.listarInscritosDeEvento(EVENTO_ID, ORGANIZADOR_ID));

        verifyNoInteractions(inscripcionRepository, asistenciaRepository);
    }

    private static Usuario usuario(Long id, Rol rol, String nombre) {
        Usuario usuario = new Usuario();
        usuario.setId(id);
        usuario.setRol(rol);
        usuario.setNombre(nombre);
        usuario.setCorreo(nombre.toLowerCase() + "@unillanos.edu.co");
        return usuario;
    }

    private static Evento evento(Usuario organizador) {
        Evento evento = new Evento();
        evento.setId(EVENTO_ID);
        evento.setTitulo("Evento");
        evento.setOrganizador(organizador);
        return evento;
    }

    private static Inscripcion inscripcion(Long id, Usuario usuario, Evento evento) {
        Inscripcion inscripcion = new Inscripcion();
        inscripcion.setId(id);
        inscripcion.setUsuario(usuario);
        inscripcion.setEvento(evento);
        inscripcion.setEstado(EstadoInscripcion.ACTIVA);
        return inscripcion;
    }
}
