package com.eventu.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import com.eventu.dto.EventoActualizacionRequestDTO;
import com.eventu.dto.EventoRequestDTO;
import com.eventu.exceptions.AccesoDenegadoException;
import com.eventu.exceptions.ConflictoException;
import com.eventu.exceptions.RecursoNoEncontradoException;
import com.eventu.exceptions.SolicitudInvalidaException;
import com.eventu.models.Categoria;
import com.eventu.models.EstadoEvento;
import com.eventu.models.EstadoInscripcion;
import com.eventu.models.Evento;
import com.eventu.models.Inscripcion;
import com.eventu.models.Rol;
import com.eventu.models.Usuario;
import com.eventu.notificaciones.CancelacionEventoNotificada;
import com.eventu.repositories.CategoriaRepository;
import com.eventu.repositories.EventoRepository;
import com.eventu.repositories.InscripcionRepository;

/** Pruebas de HU-05 (crear evento), HU-06 (editar evento) y HU-07 (cancelar evento). Cada prueba indica el escenario que cubre */
@ExtendWith(MockitoExtension.class)
class EventoServiceTest {

    private static final Long ORGANIZADOR_ID = 10L;
    private static final Long OTRO_ID = 11L;
    private static final Long USUARIO_ID = 20L;
    private static final Long EVENTO_ID = 100L;
    private static final Long CATEGORIA_ID = 5L;

    @Mock private EventoRepository eventoRepository;
    @Mock private CategoriaRepository categoriaRepository;
    @Mock private InscripcionRepository inscripcionRepository;
    @Mock private AutorizacionService autorizacionService;
    @Mock private LogService logService;
    @Mock private ApplicationEventPublisher publicador;

    private EventoService servicio;

    @BeforeEach
    void preparar() {
        servicio = new EventoService(eventoRepository, categoriaRepository, inscripcionRepository,
                autorizacionService, logService, publicador);
    }

    // HU-05 escenario 1.0: crear con datos válidos

    @Test
    void crear_conDatosValidos_creaElEventoPublicadoConTodosLosCuposYRegistraElLog() {
        Usuario organizador = usuario(ORGANIZADOR_ID, Rol.ORGANIZADOR);
        when(autorizacionService.obtenerGestorDeEventos(ORGANIZADOR_ID)).thenReturn(organizador);
        when(categoriaRepository.findById(CATEGORIA_ID)).thenReturn(Optional.of(categoria(true)));
        when(eventoRepository.save(any(Evento.class))).thenAnswer(inv -> {
            Evento e = inv.getArgument(0);
            e.setId(EVENTO_ID);
            return e;
        });

        Evento creado = servicio.crearEvento(solicitudCrear(), ORGANIZADOR_ID);

        assertEquals(EstadoEvento.PUBLICADO, creado.getEstado());
        assertEquals(50, creado.getCuposMaximos());
        assertEquals(50, creado.getCuposDisponibles());
        assertEquals("Feria de ciencias", creado.getTitulo());
        assertEquals(organizador, creado.getOrganizador());
        verify(logService).registrarExito(ORGANIZADOR_ID, LogService.ACCION_CREAR_EVENTO, LogService.ENTIDAD_EVENTO, EVENTO_ID);
    }

    // HU-05 escenario 2.0: solo categorías activas

    @Test
    void crear_conCategoriaInactiva_lanzaSolicitudInvalidaYNoGuarda() {
        when(autorizacionService.obtenerGestorDeEventos(ORGANIZADOR_ID)).thenReturn(usuario(ORGANIZADOR_ID, Rol.ORGANIZADOR));
        when(categoriaRepository.findById(CATEGORIA_ID)).thenReturn(Optional.of(categoria(false)));

        assertThrows(SolicitudInvalidaException.class, () -> servicio.crearEvento(solicitudCrear(), ORGANIZADOR_ID));

        verify(eventoRepository, never()).save(any());
    }

    @Test
    void crear_conCategoriaInexistente_lanzaNoEncontradaYNoGuarda() {
        when(autorizacionService.obtenerGestorDeEventos(ORGANIZADOR_ID)).thenReturn(usuario(ORGANIZADOR_ID, Rol.ORGANIZADOR));
        when(categoriaRepository.findById(CATEGORIA_ID)).thenReturn(Optional.empty());

        assertThrows(RecursoNoEncontradoException.class, () -> servicio.crearEvento(solicitudCrear(), ORGANIZADOR_ID));

        verify(eventoRepository, never()).save(any());
    }

    // HU-05 escenario 1.0: rol no autorizado

    @Test
    void crear_siElUsuarioNoPuedeGestionarEventos_lanzaAccesoDenegadoYNoToca() {
        doThrow(new AccesoDenegadoException("Sin permiso"))
                .when(autorizacionService).obtenerGestorDeEventos(USUARIO_ID);

        assertThrows(AccesoDenegadoException.class, () -> servicio.crearEvento(solicitudCrear(), USUARIO_ID));

        verifyNoInteractions(eventoRepository, categoriaRepository);
    }

    // HU-06 escenario 1.0: edición por el organizador dueño

    @Test
    void editar_porElOrganizadorDueno_guardaLosCambiosYRecalculaLosCuposConservandoLosInscritos() {
        Usuario dueno = usuario(ORGANIZADOR_ID, Rol.ORGANIZADOR);
        Evento evento = evento(dueno, EstadoEvento.PUBLICADO, 10, 7); // 3 inscritos
        prepararEdicion(dueno, evento, true);
        when(eventoRepository.save(evento)).thenReturn(evento);

        EventoActualizacionRequestDTO cambios = new EventoActualizacionRequestDTO();
        cambios.setTitulo("  Nuevo título  ");
        cambios.setCuposMaximos(20);

        Evento guardado = servicio.actualizarEvento(EVENTO_ID, cambios, ORGANIZADOR_ID);

        assertEquals("Nuevo título", guardado.getTitulo());
        assertEquals(20, guardado.getCuposMaximos());
        assertEquals(17, guardado.getCuposDisponibles());
        verify(logService).registrarExito(ORGANIZADOR_ID, LogService.ACCION_EDITAR_EVENTO, LogService.ENTIDAD_EVENTO, EVENTO_ID);
    }

    // HU-06 escenario 1.0: organizador distinto

    @Test
    void editar_porOtroOrganizador_lanzaAccesoDenegadoYRegistraElIntento() {
        Evento evento = evento(usuario(ORGANIZADOR_ID, Rol.ORGANIZADOR), EstadoEvento.PUBLICADO, 10, 10);
        prepararEdicion(usuario(OTRO_ID, Rol.ORGANIZADOR), evento, false);

        assertThrows(AccesoDenegadoException.class,
                () -> servicio.actualizarEvento(EVENTO_ID, new EventoActualizacionRequestDTO(), OTRO_ID));

        verify(eventoRepository, never()).save(any());
        verify(logService).registrarFallo(eq(OTRO_ID), eq(LogService.ACCION_EDITAR_EVENTO),
                eq(LogService.ENTIDAD_EVENTO), eq(EVENTO_ID), anyString());
    }

    // HU-06 escenario 1.0: evento cancelado

    @Test
    void editar_unEventoCancelado_lanzaConflictoYNoGuarda() {
        Usuario dueno = usuario(ORGANIZADOR_ID, Rol.ORGANIZADOR);
        Evento evento = evento(dueno, EstadoEvento.CANCELADO, 10, 10);
        prepararEdicion(dueno, evento, true);

        assertThrows(ConflictoException.class,
                () -> servicio.actualizarEvento(EVENTO_ID, new EventoActualizacionRequestDTO(), ORGANIZADOR_ID));

        verify(eventoRepository, never()).save(any());
        verify(logService).registrarFallo(eq(ORGANIZADOR_ID), eq(LogService.ACCION_EDITAR_EVENTO),
                eq(LogService.ENTIDAD_EVENTO), eq(EVENTO_ID), anyString());
    }

    // HU-06 escenario 1.0: cupo menor que los inscritos

    @Test
    void editar_conCupoMenorQueLosInscritos_lanzaConflictoIndicandoCuantosInscritosHay() {
        Usuario dueno = usuario(ORGANIZADOR_ID, Rol.ORGANIZADOR);
        Evento evento = evento(dueno, EstadoEvento.PUBLICADO, 10, 4); // 6 inscritos
        prepararEdicion(dueno, evento, true);

        EventoActualizacionRequestDTO cambios = new EventoActualizacionRequestDTO();
        cambios.setCuposMaximos(5);

        ConflictoException error = assertThrows(ConflictoException.class,
                () -> servicio.actualizarEvento(EVENTO_ID, cambios, ORGANIZADOR_ID));

        assertTrue(error.getMessage().contains("6"));
        verify(eventoRepository, never()).save(any());
    }

    // HU-07 escenario 1.0: cancelación por el organizador dueño

    @Test
    void cancelar_porElOrganizadorDueno_cancelaElEventoYSusInscripcionesActivasSinBorrarlas() {
        Usuario dueno = usuario(ORGANIZADOR_ID, Rol.ORGANIZADOR);
        Evento evento = evento(dueno, EstadoEvento.PUBLICADO, 10, 8);
        prepararEdicion(dueno, evento, true);
        Inscripcion primera = inscripcionActiva(evento, "ana@unillanos.edu.co");
        Inscripcion segunda = inscripcionActiva(evento, "luis@unillanos.edu.co");
        when(inscripcionRepository.findByEventoIdAndEstado(EVENTO_ID, EstadoInscripcion.ACTIVA))
                .thenReturn(List.of(primera, segunda));
        when(eventoRepository.save(evento)).thenReturn(evento);

        Evento cancelado = servicio.cancelarEvento(EVENTO_ID, ORGANIZADOR_ID);

        assertEquals(EstadoEvento.CANCELADO, cancelado.getEstado());
        assertEquals(EstadoInscripcion.CANCELADA, primera.getEstado());
        assertEquals(EstadoInscripcion.CANCELADA, segunda.getEstado());
        verify(inscripcionRepository).saveAll(List.of(primera, segunda));
        verify(inscripcionRepository, never()).delete(any());
        verify(inscripcionRepository, never()).deleteAll(any());
        verify(logService).registrarExito(ORGANIZADOR_ID, LogService.ACCION_CANCELAR_EVENTO, LogService.ENTIDAD_EVENTO, EVENTO_ID);
    }

    // HU-07 escenario 4.0: correo a los usuarios con inscripción activa

    @Test
    void cancelar_conUsuariosInscritos_publicaElAvisoConElCorreoDeCadaUno() {
        Usuario dueno = usuario(ORGANIZADOR_ID, Rol.ORGANIZADOR);
        Evento evento = evento(dueno, EstadoEvento.PUBLICADO, 10, 8);
        prepararEdicion(dueno, evento, true);
        when(inscripcionRepository.findByEventoIdAndEstado(EVENTO_ID, EstadoInscripcion.ACTIVA)).thenReturn(List.of(
                inscripcionActiva(evento, "ana@unillanos.edu.co"), inscripcionActiva(evento, "luis@unillanos.edu.co")));
        when(eventoRepository.save(evento)).thenReturn(evento);

        servicio.cancelarEvento(EVENTO_ID, ORGANIZADOR_ID);

        ArgumentCaptor<CancelacionEventoNotificada> aviso = ArgumentCaptor.forClass(CancelacionEventoNotificada.class);
        verify(publicador).publishEvent(aviso.capture());
        assertEquals(List.of("ana@unillanos.edu.co", "luis@unillanos.edu.co"), aviso.getValue().correos());
        assertEquals("Evento", aviso.getValue().titulo());
    }

    @Test
    void cancelar_sinInscritos_cancelaPeroNoPublicaAvisoDeCorreo() {
        Usuario dueno = usuario(ORGANIZADOR_ID, Rol.ORGANIZADOR);
        Evento evento = evento(dueno, EstadoEvento.PUBLICADO, 10, 10);
        prepararEdicion(dueno, evento, true);
        when(inscripcionRepository.findByEventoIdAndEstado(EVENTO_ID, EstadoInscripcion.ACTIVA)).thenReturn(List.of());
        when(eventoRepository.save(evento)).thenReturn(evento);

        servicio.cancelarEvento(EVENTO_ID, ORGANIZADOR_ID);

        assertEquals(EstadoEvento.CANCELADO, evento.getEstado());
        verify(publicador, never()).publishEvent(any(Object.class));
    }

    // HU-07 escenario 1.0: cancelación por un organizador distinto

    @Test
    void cancelar_porOtroOrganizador_lanzaAccesoDenegadoYNoCambiaNada() {
        Evento evento = evento(usuario(ORGANIZADOR_ID, Rol.ORGANIZADOR), EstadoEvento.PUBLICADO, 10, 10);
        prepararEdicion(usuario(OTRO_ID, Rol.ORGANIZADOR), evento, false);

        assertThrows(AccesoDenegadoException.class, () -> servicio.cancelarEvento(EVENTO_ID, OTRO_ID));

        assertEquals(EstadoEvento.PUBLICADO, evento.getEstado());
        verify(eventoRepository, never()).save(any());
        verifyNoInteractions(inscripcionRepository, publicador);
        verify(logService).registrarFallo(eq(OTRO_ID), eq(LogService.ACCION_CANCELAR_EVENTO),
                eq(LogService.ENTIDAD_EVENTO), eq(EVENTO_ID), anyString());
    }

    // HU-07 escenario 1.0: evento ya cancelado

    @Test
    void cancelar_unEventoYaCancelado_lanzaConflictoYNoVuelveAAvisar() {
        Usuario dueno = usuario(ORGANIZADOR_ID, Rol.ORGANIZADOR);
        prepararEdicion(dueno, evento(dueno, EstadoEvento.CANCELADO, 10, 10), true);

        assertThrows(ConflictoException.class, () -> servicio.cancelarEvento(EVENTO_ID, ORGANIZADOR_ID));

        verify(eventoRepository, never()).save(any());
        verifyNoInteractions(inscripcionRepository, publicador);
        verify(logService).registrarFallo(eq(ORGANIZADOR_ID), eq(LogService.ACCION_CANCELAR_EVENTO),
                eq(LogService.ENTIDAD_EVENTO), eq(EVENTO_ID), anyString());
    }

    @Test
    void cancelar_unEventoInexistente_lanzaNoEncontrado() {
        Usuario dueno = usuario(ORGANIZADOR_ID, Rol.ORGANIZADOR);
        when(autorizacionService.obtenerGestorDeEventos(ORGANIZADOR_ID)).thenReturn(dueno);
        when(eventoRepository.buscarPorIdConBloqueo(EVENTO_ID)).thenReturn(Optional.empty());

        assertThrows(RecursoNoEncontradoException.class, () -> servicio.cancelarEvento(EVENTO_ID, ORGANIZADOR_ID));
    }

    @Test
    void cancelar_siElUsuarioNoPuedeGestionarEventos_lanzaAccesoDenegado() {
        doThrow(new AccesoDenegadoException("Sin permiso"))
                .when(autorizacionService).obtenerGestorDeEventos(USUARIO_ID);

        assertThrows(AccesoDenegadoException.class, () -> servicio.cancelarEvento(EVENTO_ID, USUARIO_ID));

        verifyNoInteractions(eventoRepository, inscripcionRepository, publicador);
    }

    // Utilidades

    private static Inscripcion inscripcionActiva(Evento evento, String correo) {
        Usuario inscrito = new Usuario();
        inscrito.setCorreo(correo);
        Inscripcion inscripcion = new Inscripcion();
        inscripcion.setUsuario(inscrito);
        inscripcion.setEvento(evento);
        inscripcion.setEstado(EstadoInscripcion.ACTIVA);
        return inscripcion;
    }

    private void prepararEdicion(Usuario gestor, Evento evento, boolean puedeModificar) {
        when(autorizacionService.obtenerGestorDeEventos(gestor.getId())).thenReturn(gestor);
        when(eventoRepository.buscarPorIdConBloqueo(EVENTO_ID)).thenReturn(Optional.of(evento));
        when(autorizacionService.puedeModificarEvento(gestor, evento)).thenReturn(puedeModificar);
    }

    private static Usuario usuario(Long id, Rol rol) {
        Usuario usuario = new Usuario();
        usuario.setId(id);
        usuario.setRol(rol);
        return usuario;
    }

    private static Categoria categoria(boolean activa) {
        Categoria categoria = new Categoria();
        categoria.setId(CATEGORIA_ID);
        categoria.setNombre("Académico");
        categoria.setActivo(activa);
        return categoria;
    }

    private static Evento evento(Usuario organizador, EstadoEvento estado, int maximos, int disponibles) {
        Evento evento = new Evento();
        evento.setId(EVENTO_ID);
        evento.setTitulo("Evento");
        evento.setOrganizador(organizador);
        evento.setEstado(estado);
        evento.setCuposMaximos(maximos);
        evento.setCuposDisponibles(disponibles);
        return evento;
    }

    private static EventoRequestDTO solicitudCrear() {
        EventoRequestDTO request = new EventoRequestDTO();
        request.setTitulo("  Feria de ciencias  ");
        request.setDescripcion("  Muestra de proyectos  ");
        request.setFechaInicio(LocalDateTime.now().plusDays(10));
        request.setUbicacion("  Auditorio principal  ");
        request.setCuposMaximos(50);
        request.setCategoriaId(CATEGORIA_ID);
        return request;
    }
}