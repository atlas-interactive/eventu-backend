package com.eventu.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;

import com.eventu.dto.LogFiltroRequestDTO;
import com.eventu.dto.LogResponseDTO;
import com.eventu.dto.OpcionesFiltroLogDTO;
import com.eventu.dto.PaginaResponseDTO;
import com.eventu.exceptions.SolicitudInvalidaException;
import com.eventu.models.Log;
import com.eventu.models.Rol;
import com.eventu.models.Usuario;
import com.eventu.repositories.LogRepository;
import com.eventu.util.Mensajes;

/** Pruebas de HU-20: consulta paginada y filtrada del registro de actividad */
@ExtendWith(MockitoExtension.class)
class LogConsultaServiceTest {

    private static final LogFiltroRequestDTO SIN_FILTROS = new LogFiltroRequestDTO(null, null, null, null, null, null);

    @Mock private LogRepository logRepository;

    private LogConsultaService servicio;

    @BeforeEach
    void preparar() {
        servicio = new LogConsultaService(logRepository);
    }

    // 1.0 Consulta del listado

    @Test
    void consultarLogs_sinFiltros_pideLaPaginaDelMasRecienteAlMasAntiguoYDevuelveLosDatosDelUsuario() {
        Log log = log(1L, usuario(7L, "Laura Gomez", "laura@unillanos.edu.co"), LogService.ACCION_LOGIN, LogService.EXITO, null);
        when(logRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(log), PageRequest.of(0, 20), 1));

        PaginaResponseDTO<LogResponseDTO> pagina = servicio.consultarLogs(SIN_FILTROS, 0, 20);

        ArgumentCaptor<Pageable> solicitud = ArgumentCaptor.forClass(Pageable.class);
        verify(logRepository).findAll(any(Specification.class), solicitud.capture());
        assertEquals(0, solicitud.getValue().getPageNumber());
        assertEquals(20, solicitud.getValue().getPageSize());
        assertEquals(Sort.by(Sort.Order.desc("fecha"), Sort.Order.desc("id")), solicitud.getValue().getSort());

        assertEquals(1, pagina.totalElementos());
        assertEquals(1, pagina.totalPaginas());
        LogResponseDTO registro = pagina.contenido().get(0);
        assertEquals(7L, registro.usuarioId());
        assertEquals("Laura Gomez", registro.usuarioNombre());
        assertEquals("laura@unillanos.edu.co", registro.usuarioCorreo());
        assertEquals(LogService.ACCION_LOGIN, registro.accion());
        assertEquals(LogService.EXITO, registro.resultado());
    }

    @Test
    void consultarLogs_registroSinUsuario_devuelveLosDatosDelUsuarioEnNull() {
        Log log = log(2L, null, "SOLICITAR_RECUPERACION", LogService.FALLIDO, "Correo no registrado");
        when(logRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(log), PageRequest.of(0, 20), 1));

        LogResponseDTO registro = servicio.consultarLogs(SIN_FILTROS, 0, 20).contenido().get(0);

        assertNull(registro.usuarioId());
        assertNull(registro.usuarioNombre());
        assertNull(registro.usuarioCorreo());
        assertEquals("Correo no registrado", registro.motivo());
    }

    @Test
    void consultarLogs_sinRegistros_devuelveLaPaginaVacia() {
        when(logRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(Page.empty(PageRequest.of(0, 20)));

        PaginaResponseDTO<LogResponseDTO> pagina = servicio.consultarLogs(SIN_FILTROS, 0, 20);

        assertTrue(pagina.contenido().isEmpty());
        assertEquals(0, pagina.totalElementos());
    }

    @Test
    void consultarLogs_segundaPagina_pideEsaPaginaConElTamanoRecibido() {
        when(logRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(Page.empty(PageRequest.of(2, 5)));

        servicio.consultarLogs(SIN_FILTROS, 2, 5);

        verify(logRepository).findAll(any(Specification.class), eq(PageRequest.of(2, 5, Sort.by(Sort.Order.desc("fecha"), Sort.Order.desc("id")))));
    }

    // 2.0 Filtros

    @Test
    void consultarLogs_fechaInicialPosteriorALaFinal_rechazaSinConsultar() {
        LogFiltroRequestDTO filtro = new LogFiltroRequestDTO(null, null, null, null, LocalDate.of(2026, 11, 10), LocalDate.of(2026, 11, 1));

        SolicitudInvalidaException error = assertThrows(SolicitudInvalidaException.class, () -> servicio.consultarLogs(filtro, 0, 20));

        assertEquals(Mensajes.RANGO_FECHAS_INVALIDO, error.getMessage());
        verifyNoInteractions(logRepository);
    }

    @Test
    void consultarLogs_mismoDiaComoInicioYFin_esUnRangoValido() {
        LocalDate dia = LocalDate.of(2026, 11, 2);
        when(logRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(Page.empty(PageRequest.of(0, 20)));

        servicio.consultarLogs(new LogFiltroRequestDTO(null, null, null, null, dia, dia), 0, 20);

        verify(logRepository).findAll(any(Specification.class), any(Pageable.class));
    }

    @Test
    void consultarLogs_resultadoDesconocido_rechazaSinConsultar() {
        LogFiltroRequestDTO filtro = new LogFiltroRequestDTO(null, null, "PENDIENTE", null, null, null);

        SolicitudInvalidaException error = assertThrows(SolicitudInvalidaException.class, () -> servicio.consultarLogs(filtro, 0, 20));

        assertEquals(LogConsultaService.RESULTADO_INVALIDO, error.getMessage());
        verifyNoInteractions(logRepository);
    }

    @Test
    void consultarLogs_resultadoEnMinusculas_seAceptaPorqueSeNormaliza() {
        when(logRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(Page.empty(PageRequest.of(0, 20)));

        servicio.consultarLogs(new LogFiltroRequestDTO(null, null, " fallido ", null, null, null), 0, 20);

        verify(logRepository).findAll(any(Specification.class), any(Pageable.class));
    }

    @Test
    void consultarLogs_textoDeUsuarioDemasiadoLargo_rechazaSinConsultar() {
        LogFiltroRequestDTO filtro = new LogFiltroRequestDTO(null, null, null, "a".repeat(101), null, null);

        assertThrows(SolicitudInvalidaException.class, () -> servicio.consultarLogs(filtro, 0, 20));

        verifyNoInteractions(logRepository);
    }

    // Paginación

    @Test
    void consultarLogs_paginaNegativa_rechazaSinConsultar() {
        SolicitudInvalidaException error = assertThrows(SolicitudInvalidaException.class, () -> servicio.consultarLogs(SIN_FILTROS, -1, 20));

        assertEquals(LogConsultaService.PAGINA_INVALIDA, error.getMessage());
        verifyNoInteractions(logRepository);
    }

    @Test
    void consultarLogs_tamanoFueraDeRango_rechazaSinConsultar() {
        assertThrows(SolicitudInvalidaException.class, () -> servicio.consultarLogs(SIN_FILTROS, 0, 0));
        assertThrows(SolicitudInvalidaException.class, () -> servicio.consultarLogs(SIN_FILTROS, 0, LogConsultaService.TAMANO_MAXIMO + 1));

        verifyNoInteractions(logRepository);
    }

    // Opciones de filtro

    @Test
    void obtenerOpcionesDeFiltro_devuelveLasAccionesYEntidadesRegistradasYLosDosResultados() {
        when(logRepository.listarAcciones()).thenReturn(List.of("CREAR_EVENTO", "LOGIN"));
        when(logRepository.listarEntidades()).thenReturn(List.of("EVENTO", "USUARIO"));

        OpcionesFiltroLogDTO opciones = servicio.obtenerOpcionesDeFiltro();

        assertEquals(List.of("CREAR_EVENTO", "LOGIN"), opciones.acciones());
        assertEquals(List.of("EVENTO", "USUARIO"), opciones.entidades());
        assertEquals(List.of(LogService.EXITO, LogService.FALLIDO), opciones.resultados());
    }

    private Log log(Long id, Usuario usuario, String accion, String resultado, String motivo) {
        Log log = new Log();
        log.setId(id);
        log.setFecha(LocalDateTime.of(2026, 11, 2, 10, 30));
        log.setUsuario(usuario);
        log.setUsuarioId(usuario == null ? null : usuario.getId());
        log.setAccion(accion);
        log.setEntidad(LogService.ENTIDAD_USUARIO);
        log.setResultado(resultado);
        log.setMotivo(motivo);
        return log;
    }

    private Usuario usuario(Long id, String nombre, String correo) {
        Usuario usuario = new Usuario();
        usuario.setId(id);
        usuario.setNombre(nombre);
        usuario.setCorreo(correo);
        usuario.setRol(Rol.USUARIO);
        return usuario;
    }
}
