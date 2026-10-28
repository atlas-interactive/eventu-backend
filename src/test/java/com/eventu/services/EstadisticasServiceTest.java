package com.eventu.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;

import com.eventu.dto.CategoriaConcurridaDTO;
import com.eventu.dto.EstadisticasResponseDTO;
import com.eventu.dto.EventoConMasInscritosDTO;
import com.eventu.exceptions.AccesoDenegadoException;
import com.eventu.models.EstadoInscripcion;
import com.eventu.models.Rol;
import com.eventu.models.Usuario;
import com.eventu.repositories.AsistenciaRepository;
import com.eventu.repositories.InscripcionRepository;
import com.eventu.util.Mensajes;

/** Pruebas de HU-16 (dashboard de estadísticas). Cada prueba indica el escenario que cubre */
@ExtendWith(MockitoExtension.class)
class EstadisticasServiceTest {

    private static final Long ORGANIZADOR_ID = 10L;
    private static final Long ADMIN_ID = 1L;
    private static final Long USUARIO_ID = 20L;

    @Mock private InscripcionRepository inscripcionRepository;
    @Mock private AsistenciaRepository asistenciaRepository;
    @Mock private AutorizacionService autorizacionService;

    private EstadisticasService servicio;

    @BeforeEach
    void preparar() {
        servicio = new EstadisticasService(inscripcionRepository, asistenciaRepository, autorizacionService);
    }

    // HU-16 escenario 1.0: organizador

    @Test
    void obtenerEstadisticas_comoOrganizador_calculaLosIndicadoresSoloDeSusEventos() {
        when(autorizacionService.obtenerGestorDeEventos(ORGANIZADOR_ID)).thenReturn(usuario(ORGANIZADOR_ID, Rol.ORGANIZADOR));
        when(inscripcionRepository.countByEventoOrganizadorIdAndEstado(ORGANIZADOR_ID, EstadoInscripcion.ACTIVA))
                .thenReturn(8L);
        when(inscripcionRepository.contarAsistenciasDeOrganizador(ORGANIZADOR_ID)).thenReturn(6L);

        EstadisticasResponseDTO dto = servicio.obtenerEstadisticas(ORGANIZADOR_ID);

        assertEquals(8, dto.inscripcionesActivas());
        assertEquals(6, dto.asistentes());
        assertEquals(75.0, dto.porcentajeAsistencia());
        assertTrue(dto.categoriasMasConcurridas().isEmpty());
        assertTrue(dto.eventosConMasInscritos().isEmpty());
    }

    @Test
    void obtenerEstadisticas_comoOrganizador_redondeaElPorcentajeADosDecimales() {
        when(autorizacionService.obtenerGestorDeEventos(ORGANIZADOR_ID)).thenReturn(usuario(ORGANIZADOR_ID, Rol.ORGANIZADOR));
        when(inscripcionRepository.countByEventoOrganizadorIdAndEstado(ORGANIZADOR_ID, EstadoInscripcion.ACTIVA))
                .thenReturn(3L);
        when(inscripcionRepository.contarAsistenciasDeOrganizador(ORGANIZADOR_ID)).thenReturn(1L);

        assertEquals(33.33, servicio.obtenerEstadisticas(ORGANIZADOR_ID).porcentajeAsistencia());
    }

    // HU-16 escenario 2.0: administrador

    @Test
    void obtenerEstadisticas_comoAdministrador_incluyeTodaLaPlataformaYLosRankings() {
        List<CategoriaConcurridaDTO> categorias = List.of(new CategoriaConcurridaDTO("Tecnología", 12L));
        List<EventoConMasInscritosDTO> eventos = List.of(new EventoConMasInscritosDTO(100L, "Congreso", 9L));
        when(autorizacionService.obtenerGestorDeEventos(ADMIN_ID)).thenReturn(usuario(ADMIN_ID, Rol.ADMIN));
        when(inscripcionRepository.countByEstado(EstadoInscripcion.ACTIVA)).thenReturn(40L);
        when(asistenciaRepository.count()).thenReturn(10L);
        when(inscripcionRepository.findCategoriasMasConcurridas(EstadoInscripcion.ACTIVA,
                PageRequest.of(0, EstadisticasService.TAMANO_RANKING))).thenReturn(categorias);
        when(inscripcionRepository.findEventosConMasInscritos(EstadoInscripcion.ACTIVA,
                PageRequest.of(0, EstadisticasService.TAMANO_RANKING))).thenReturn(eventos);

        EstadisticasResponseDTO dto = servicio.obtenerEstadisticas(ADMIN_ID);

        assertEquals(40, dto.inscripcionesActivas());
        assertEquals(10, dto.asistentes());
        assertEquals(25.0, dto.porcentajeAsistencia());
        assertEquals(categorias, dto.categoriasMasConcurridas());
        assertEquals(eventos, dto.eventosConMasInscritos());
    }

    // HU-16 escenario 3.0: organizador sin eventos propios

    @Test
    void obtenerEstadisticas_organizadorSinEventos_devuelveElDashboardEnCero() {
        when(autorizacionService.obtenerGestorDeEventos(ORGANIZADOR_ID)).thenReturn(usuario(ORGANIZADOR_ID, Rol.ORGANIZADOR));
        when(inscripcionRepository.countByEventoOrganizadorIdAndEstado(ORGANIZADOR_ID, EstadoInscripcion.ACTIVA))
                .thenReturn(0L);
        when(inscripcionRepository.contarAsistenciasDeOrganizador(ORGANIZADOR_ID)).thenReturn(0L);

        EstadisticasResponseDTO dto = servicio.obtenerEstadisticas(ORGANIZADOR_ID);

        assertEquals(0, dto.inscripcionesActivas());
        assertEquals(0, dto.asistentes());
        assertEquals(0.0, dto.porcentajeAsistencia());
    }

    @Test
    void obtenerEstadisticas_plataformaSinInscripciones_noDividePorCero() {
        when(autorizacionService.obtenerGestorDeEventos(ADMIN_ID)).thenReturn(usuario(ADMIN_ID, Rol.ADMIN));
        when(inscripcionRepository.countByEstado(EstadoInscripcion.ACTIVA)).thenReturn(0L);
        when(asistenciaRepository.count()).thenReturn(0L);
        when(inscripcionRepository.findCategoriasMasConcurridas(any(), any())).thenReturn(List.of());
        when(inscripcionRepository.findEventosConMasInscritos(any(), any())).thenReturn(List.of());

        EstadisticasResponseDTO dto = servicio.obtenerEstadisticas(ADMIN_ID);

        assertEquals(0.0, dto.porcentajeAsistencia());
        assertTrue(dto.categoriasMasConcurridas().isEmpty());
    }

    // Rol sin permiso

    @Test
    void obtenerEstadisticas_comoUsuarioComun_lanzaAccesoDenegadoSinConsultarDatos() {
        doThrow(new AccesoDenegadoException(Mensajes.SIN_PERMISO_GESTIONAR_EVENTOS))
                .when(autorizacionService).obtenerGestorDeEventos(USUARIO_ID);

        assertThrows(AccesoDenegadoException.class, () -> servicio.obtenerEstadisticas(USUARIO_ID));

        verifyNoInteractions(inscripcionRepository, asistenciaRepository);
    }

    private static Usuario usuario(Long id, Rol rol) {
        Usuario usuario = new Usuario();
        usuario.setId(id);
        usuario.setRol(rol);
        return usuario;
    }
}
