package com.eventu.services;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.mockito.ArgumentMatchers.eq;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;

import com.eventu.dto.EventoFiltroRequestDTO;
import com.eventu.dto.EventoResponseDTO;
import com.eventu.exceptions.SolicitudInvalidaException;
import com.eventu.models.Categoria;
import com.eventu.models.EstadoEvento;
import com.eventu.models.Evento;
import com.eventu.models.Rol;
import com.eventu.models.Usuario;
import com.eventu.repositories.CategoriaRepository;
import com.eventu.repositories.EventoRepository;
import com.eventu.repositories.InscripcionRepository;
import com.eventu.exceptions.RecursoNoEncontradoException;

/** Pruebas de HU-08 (consulta y filtro de eventos) y HU-09 (detalle de un evento) */
@ExtendWith(MockitoExtension.class)
class EventoConsultaServiceTest {

    private static final Long USUARIO_ID = 20L;
    private static final Long ADMIN_ID = 1L;
    private static final Long ORGANIZADOR_ID = 10L;
    private static final Long EVENTO_ID = 100L;

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

    // HU-08: filtros

    @Test
    void listarConFiltro_sinNingunFiltro_tambienSaleOrdenadoPorFecha() {
        when(autorizacionService.obtenerUsuario(USUARIO_ID)).thenReturn(usuario(USUARIO_ID, Rol.USUARIO));
        when(eventoRepository.findAll(any(Specification.class), any(Sort.class))).thenReturn(List.of());

        servicio.listarEventos(USUARIO_ID, new EventoFiltroRequestDTO(null, null, null, "   "));

        verify(eventoRepository).findAll(any(Specification.class), eq(EventoService.ORDEN_LISTADO));
    }

    @Test
    void listarConFiltro_porCategoria_consultaConEspecificacionOrdenadaPorFecha() {
        Evento evento = evento(EstadoEvento.PUBLICADO);
        when(autorizacionService.obtenerUsuario(USUARIO_ID)).thenReturn(usuario(USUARIO_ID, Rol.USUARIO));
        when(eventoRepository.findAll(any(Specification.class), any(Sort.class))).thenReturn(List.of(evento));

        List<EventoResponseDTO> resultado = servicio.listarEventos(USUARIO_ID, new EventoFiltroRequestDTO(5L, null, null, null));

        assertEquals(1, resultado.size());
        assertEquals("Académico", resultado.get(0).categoriaNombre());
        verify(eventoRepository).findAll(any(Specification.class), eq(EventoService.ORDEN_LISTADO));
    }

    @Test
    void listarConFiltro_comoAdministrador_tambienConsultaConEspecificacion() {
        when(autorizacionService.obtenerUsuario(ADMIN_ID)).thenReturn(usuario(ADMIN_ID, Rol.ADMIN));
        when(eventoRepository.findAll(any(Specification.class), any(Sort.class))).thenReturn(List.of());

        servicio.listarEventos(ADMIN_ID, new EventoFiltroRequestDTO(null, null, null, "feria"));

        verify(eventoRepository).findAll(any(Specification.class), any(Sort.class));
    }

    @Test
    void listarConFiltro_conFechaInicialPosteriorALaFinal_lanzaSolicitudInvalida() {
        EventoFiltroRequestDTO filtro = new EventoFiltroRequestDTO(null, LocalDate.of(2026, 12, 10), LocalDate.of(2026, 12, 1), null);

        assertThrows(SolicitudInvalidaException.class, () -> servicio.listarEventos(USUARIO_ID, filtro));

        verifyNoInteractions(eventoRepository);
    }

    @Test
    void listarConFiltro_conTextoDemasiadoLargo_lanzaSolicitudInvalida() {
        EventoFiltroRequestDTO filtro = new EventoFiltroRequestDTO(null, null, null, "a".repeat(101));

        assertThrows(SolicitudInvalidaException.class, () -> servicio.listarEventos(USUARIO_ID, filtro));

        verifyNoInteractions(eventoRepository);
    }

    // HU-09: detalle

    @Test
    void obtenerEvento_publicado_devuelveElDetalleCompleto() {
        when(autorizacionService.obtenerUsuario(USUARIO_ID)).thenReturn(usuario(USUARIO_ID, Rol.USUARIO));
        when(eventoRepository.findById(EVENTO_ID)).thenReturn(Optional.of(evento(EstadoEvento.PUBLICADO)));

        EventoResponseDTO detalle = servicio.obtenerEvento(EVENTO_ID, USUARIO_ID);

        assertEquals("Feria de ciencias", detalle.titulo());
        assertEquals("Auditorio", detalle.ubicacion());
        assertEquals("Académico", detalle.categoriaNombre());
        assertEquals(40, detalle.cuposDisponibles());
        assertEquals("PUBLICADO", detalle.estado());
    }

    @Test
    void obtenerEvento_inexistente_lanzaNoEncontrado() {
        when(autorizacionService.obtenerUsuario(USUARIO_ID)).thenReturn(usuario(USUARIO_ID, Rol.USUARIO));
        when(eventoRepository.findById(EVENTO_ID)).thenReturn(Optional.empty());

        assertThrows(RecursoNoEncontradoException.class, () -> servicio.obtenerEvento(EVENTO_ID, USUARIO_ID));
    }

    @Test
    void obtenerEvento_canceladoParaUsuarioComun_lanzaNoEncontrado() {
        Usuario usuario = usuario(USUARIO_ID, Rol.USUARIO);
        Evento cancelado = evento(EstadoEvento.CANCELADO);
        when(autorizacionService.obtenerUsuario(USUARIO_ID)).thenReturn(usuario);
        when(eventoRepository.findById(EVENTO_ID)).thenReturn(Optional.of(cancelado));
        when(autorizacionService.puedeModificarEvento(usuario, cancelado)).thenReturn(false);

        assertThrows(RecursoNoEncontradoException.class, () -> servicio.obtenerEvento(EVENTO_ID, USUARIO_ID));
    }

    @Test
    void obtenerEvento_canceladoParaAdminOOrganizadorDueno_devuelveElDetalle() {
        Usuario admin = usuario(ADMIN_ID, Rol.ADMIN);
        Evento cancelado = evento(EstadoEvento.CANCELADO);
        when(autorizacionService.obtenerUsuario(ADMIN_ID)).thenReturn(admin);
        when(eventoRepository.findById(EVENTO_ID)).thenReturn(Optional.of(cancelado));
        when(autorizacionService.puedeModificarEvento(admin, cancelado)).thenReturn(true);

        EventoResponseDTO detalle = servicio.obtenerEvento(EVENTO_ID, ADMIN_ID);

        assertEquals("CANCELADO", detalle.estado());
        assertTrue(detalle.cuposMaximos() > 0);
    }

    // Utilidades

    private static Usuario usuario(Long id, Rol rol) {
        Usuario usuario = new Usuario();
        usuario.setId(id);
        usuario.setRol(rol);
        usuario.setNombre("Usuario " + id);
        return usuario;
    }

    private static Evento evento(EstadoEvento estado) {
        Categoria categoria = new Categoria();
        categoria.setId(5L);
        categoria.setNombre("Académico");
        Evento evento = new Evento();
        evento.setId(EVENTO_ID);
        evento.setTitulo("Feria de ciencias");
        evento.setDescripcion("Muestra de proyectos");
        evento.setFechaInicio(LocalDateTime.now().plusDays(10));
        evento.setUbicacion("Auditorio");
        evento.setCuposMaximos(50);
        evento.setCuposDisponibles(40);
        evento.setEstado(estado);
        evento.setCertificable(false);
        evento.setOrganizador(usuario(ORGANIZADOR_ID, Rol.ORGANIZADOR));
        evento.setCategoria(categoria);
        return evento;
    }
}