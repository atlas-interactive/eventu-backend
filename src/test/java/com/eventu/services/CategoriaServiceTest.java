package com.eventu.services;

import com.eventu.dto.CategoriaRequestDTO;
import com.eventu.dto.CategoriaResponseDTO;
import com.eventu.exceptions.AccesoDenegadoException;
import com.eventu.exceptions.ConflictoException;
import com.eventu.exceptions.RecursoNoEncontradoException;
import com.eventu.models.Categoria;
import com.eventu.models.EstadoEvento;
import com.eventu.repositories.CategoriaRepository;
import com.eventu.repositories.EventoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/** Pruebas unitarias de HU-04 (gestión de categorías). Cada prueba indica el escenario de los criterios que cubre */
@ExtendWith(MockitoExtension.class)
class CategoriaServiceTest {

    private static final Long ADMIN_ID = 1L;
    private static final Long USUARIO_ID = 2L;

    @Mock private CategoriaRepository categoriaRepository;
    @Mock private EventoRepository eventoRepository;
    @Mock private AutorizacionService autorizacionService;
    @Mock private LogService logService;

    private CategoriaService servicio;

    @BeforeEach
    void preparar() {
        servicio = new CategoriaService(categoriaRepository, eventoRepository, autorizacionService, logService);
    }

    // Escenario 1: crear

    @Test
    void crear_conNombreDisponible_creaLaCategoriaActivaYRegistraElLog() {
        when(categoriaRepository.existsByNombreIgnoreCase("Cultural")).thenReturn(false);
        when(categoriaRepository.save(any(Categoria.class))).thenAnswer(inv -> {
            Categoria c = inv.getArgument(0);
            c.setId(10L);
            return c;
        });

        CategoriaResponseDTO respuesta = servicio.crearCategoria(solicitud("  Cultural  "), ADMIN_ID);

        assertEquals("Cultural", respuesta.nombre());
        assertTrue(respuesta.activo());
        verify(logService).registrarExito(ADMIN_ID, LogService.ACCION_CREAR_CATEGORIA, LogService.ENTIDAD_CATEGORIA, 10L);
    }

    @Test
    void crear_conNombreDuplicadoSinImportarMayusculas_lanzaConflictoYNoGuarda() {
        when(categoriaRepository.existsByNombreIgnoreCase("cultural")).thenReturn(true);

        assertThrows(ConflictoException.class, () -> servicio.crearCategoria(solicitud("cultural"), ADMIN_ID));

        verify(categoriaRepository, never()).save(any());
        verify(logService).registrarFallo(eq(ADMIN_ID), eq(LogService.ACCION_CREAR_CATEGORIA),
                eq(LogService.ENTIDAD_CATEGORIA), isNull(), anyString());
    }

    // Escenario 2: editar el nombre

    @Test
    void editar_conNuevoNombreDisponible_actualizaElNombre() {
        Categoria existente = categoria(5L, "Cultural", true);
        when(categoriaRepository.findById(5L)).thenReturn(Optional.of(existente));
        when(categoriaRepository.existsByNombreIgnoreCaseAndIdNot("Arte", 5L)).thenReturn(false);
        when(categoriaRepository.save(existente)).thenReturn(existente);

        CategoriaResponseDTO respuesta = servicio.editarCategoria(5L, solicitud("Arte"), ADMIN_ID);

        assertEquals("Arte", respuesta.nombre());
        verify(logService).registrarExito(ADMIN_ID, LogService.ACCION_EDITAR_CATEGORIA, LogService.ENTIDAD_CATEGORIA, 5L);
    }

    @Test
    void editar_conNombreDeOtraCategoria_lanzaConflictoYNoGuarda() {
        when(categoriaRepository.findById(5L)).thenReturn(Optional.of(categoria(5L, "Cultural", true)));
        when(categoriaRepository.existsByNombreIgnoreCaseAndIdNot("deportivo", 5L)).thenReturn(true);

        assertThrows(ConflictoException.class, () -> servicio.editarCategoria(5L, solicitud("deportivo"), ADMIN_ID));

        verify(categoriaRepository, never()).save(any());
        verify(logService).registrarFallo(eq(ADMIN_ID), eq(LogService.ACCION_EDITAR_CATEGORIA),
                eq(LogService.ENTIDAD_CATEGORIA), eq(5L), anyString());
    }

    @Test
    void editar_categoriaInexistente_lanzaNoEncontrada() {
        when(categoriaRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(RecursoNoEncontradoException.class,
                () -> servicio.editarCategoria(99L, solicitud("Arte"), ADMIN_ID));

        verify(categoriaRepository, never()).save(any());
    }

    // Escenario 3: desactivar y reactivar (solo con el id)

    @Test
    void desactivar_categoriaConEventosActivos_laMarcaInactivaSinEliminarlaNiTocarLosEventos() {
        Categoria existente = categoria(5L, "Cultural", true);
        when(categoriaRepository.findById(5L)).thenReturn(Optional.of(existente));
        when(categoriaRepository.save(existente)).thenReturn(existente);
        when(eventoRepository.countByCategoriaIdAndEstado(5L, EstadoEvento.PUBLICADO)).thenReturn(3L);

        CategoriaResponseDTO respuesta = servicio.desactivarCategoria(5L, ADMIN_ID);

        assertFalse(respuesta.activo());
        assertEquals("Cultural", respuesta.nombre());
        assertEquals(3L, respuesta.eventosActivos());
        verify(categoriaRepository, never()).delete(any(Categoria.class));
        verify(categoriaRepository, never()).deleteById(any());
        verify(logService).registrarExito(ADMIN_ID, LogService.ACCION_DESACTIVAR_CATEGORIA, LogService.ENTIDAD_CATEGORIA, 5L);
    }

    @Test
    void desactivar_categoriaYaInactiva_noHaceNadaNiRegistraLog() {
        Categoria existente = categoria(5L, "Cultural", false);
        when(categoriaRepository.findById(5L)).thenReturn(Optional.of(existente));

        CategoriaResponseDTO respuesta = servicio.desactivarCategoria(5L, ADMIN_ID);

        assertFalse(respuesta.activo());
        verify(categoriaRepository, never()).save(any());
        verify(logService, never()).registrarExito(any(), anyString(), anyString(), any());
    }

    @Test
    void activar_categoriaInactiva_laVuelveAMarcarActiva() {
        Categoria existente = categoria(5L, "Cultural", false);
        when(categoriaRepository.findById(5L)).thenReturn(Optional.of(existente));
        when(categoriaRepository.save(existente)).thenReturn(existente);

        CategoriaResponseDTO respuesta = servicio.activarCategoria(5L, ADMIN_ID);

        assertTrue(respuesta.activo());
        verify(logService).registrarExito(ADMIN_ID, LogService.ACCION_ACTIVAR_CATEGORIA, LogService.ENTIDAD_CATEGORIA, 5L);
    }

    @Test
    void desactivar_categoriaInexistente_lanzaNoEncontrada() {
        when(categoriaRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(RecursoNoEncontradoException.class, () -> servicio.desactivarCategoria(99L, ADMIN_ID));

        verify(categoriaRepository, never()).save(any());
    }

    @Test
    void editar_elNombre_noCambiaElEstado() {
        Categoria existente = categoria(5L, "Cultural", false);
        when(categoriaRepository.findById(5L)).thenReturn(Optional.of(existente));
        when(categoriaRepository.existsByNombreIgnoreCaseAndIdNot("Arte", 5L)).thenReturn(false);
        when(categoriaRepository.save(existente)).thenReturn(existente);

        CategoriaResponseDTO respuesta = servicio.editarCategoria(5L, solicitud("Arte"), ADMIN_ID);

        assertEquals("Arte", respuesta.nombre());
        assertFalse(respuesta.activo());
    }

    // Escenario 4: solo administradores

    @Test
    void crear_siElUsuarioNoEsAdministrador_lanzaAccesoDenegadoYRegistraElIntento() {
        doThrow(new AccesoDenegadoException("Solo un administrador puede realizar esta acción."))
                .when(autorizacionService).obtenerAdministrador(USUARIO_ID);

        assertThrows(AccesoDenegadoException.class,
                () -> servicio.crearCategoria(solicitud("Cultural"), USUARIO_ID));

        verifyNoInteractions(categoriaRepository);
        verify(logService).registrarFallo(eq(USUARIO_ID), eq(LogService.ACCION_CREAR_CATEGORIA),
                eq(LogService.ENTIDAD_CATEGORIA), isNull(), anyString());
    }

    @Test
    void desactivar_siElUsuarioNoEsAdministrador_lanzaAccesoDenegadoYRegistraElIntento() {
        doThrow(new AccesoDenegadoException("Solo un administrador puede realizar esta acción."))
                .when(autorizacionService).obtenerAdministrador(USUARIO_ID);

        assertThrows(AccesoDenegadoException.class, () -> servicio.desactivarCategoria(5L, USUARIO_ID));

        verifyNoInteractions(categoriaRepository);
        verify(logService).registrarFallo(eq(USUARIO_ID), eq(LogService.ACCION_DESACTIVAR_CATEGORIA),
                eq(LogService.ENTIDAD_CATEGORIA), eq(5L), anyString());
    }

    @Test
    void activar_siElUsuarioNoEsAdministrador_lanzaAccesoDenegadoYRegistraElIntento() {
        doThrow(new AccesoDenegadoException("Solo un administrador puede realizar esta acción."))
                .when(autorizacionService).obtenerAdministrador(USUARIO_ID);

        assertThrows(AccesoDenegadoException.class, () -> servicio.activarCategoria(5L, USUARIO_ID));

        verifyNoInteractions(categoriaRepository);
        verify(logService).registrarFallo(eq(USUARIO_ID), eq(LogService.ACCION_ACTIVAR_CATEGORIA),
                eq(LogService.ENTIDAD_CATEGORIA), eq(5L), anyString());
    }

    @Test
    void editar_siElUsuarioNoEsAdministrador_lanzaAccesoDenegadoYNoModificaNada() {
        doThrow(new AccesoDenegadoException("Solo un administrador puede realizar esta acción."))
                .when(autorizacionService).obtenerAdministrador(USUARIO_ID);

        assertThrows(AccesoDenegadoException.class,
                () -> servicio.editarCategoria(5L, solicitud("Arte"), USUARIO_ID));

        verifyNoInteractions(categoriaRepository);
        verify(logService).registrarFallo(eq(USUARIO_ID), eq(LogService.ACCION_EDITAR_CATEGORIA),
                eq(LogService.ENTIDAD_CATEGORIA), eq(5L), anyString());
    }

    // Selector de HU-05: solo categorías activas

    @Test
    void listar_soloActivas_noIncluyeLasInactivas() {
        when(categoriaRepository.findByActivoTrue()).thenReturn(List.of(categoria(1L, "Cultural", true)));

        List<CategoriaResponseDTO> categorias = servicio.listarCategorias(true);

        assertEquals(1, categorias.size());
        assertEquals("Cultural", categorias.get(0).nombre());
        verify(categoriaRepository, never()).findAll();
    }

    @Test
    void listar_todas_incluyeActivasEInactivas() {
        when(categoriaRepository.findAll()).thenReturn(List.of(
                categoria(1L, "Cultural", true), categoria(2L, "Deportivo", false)));

        List<CategoriaResponseDTO> categorias = servicio.listarCategorias(false);

        assertEquals(2, categorias.size());
        verify(categoriaRepository, never()).findByActivoTrue();
    }

    // Utilidades

    private static CategoriaRequestDTO solicitud(String nombre) {
        CategoriaRequestDTO request = new CategoriaRequestDTO();
        request.setNombre(nombre);
        return request;
    }

    private static Categoria categoria(Long id, String nombre, boolean activo) {
        Categoria categoria = new Categoria();
        categoria.setId(id);
        categoria.setNombre(nombre);
        categoria.setActivo(activo);
        return categoria;
    }
}