package com.eventu.repositories;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.time.LocalDateTime;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.eventu.dto.EventoFiltroRequestDTO;
import com.eventu.models.EstadoEvento;
import com.eventu.models.Evento;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Root;

/** Pruebas de la consulta de eventos disponibles (HU-08) */
@ExtendWith(MockitoExtension.class)
class EventoEspecificacionesTest {

    private static final EventoFiltroRequestDTO SIN_FILTROS = new EventoFiltroRequestDTO(null, null, null, null);
    private static final LocalDateTime AHORA = LocalDateTime.of(2026, 11, 7, 20, 0);

    @Mock private Root<Evento> root;
    @Mock private CriteriaQuery<?> query;
    @Mock private CriteriaBuilder cb;
    @Mock private Path<Object> estado;
    @Mock private Path<LocalDateTime> fechaInicio;

    @BeforeEach
    void configurar() {
        doReturn(Long.class).when(query).getResultType();
        lenient().doReturn(estado).when(root).get("estado");
        lenient().doReturn(fechaInicio).when(root).get("fechaInicio");
    }

    @Test
    void soloDisponibles_dejaPublicadosQueNoHanIniciado() {
        EventoEspecificaciones.filtrar(SIN_FILTROS, true, null, AHORA).toPredicate(root, query, cb);

        verify(cb).equal(estado, EstadoEvento.PUBLICADO);
        verify(cb).greaterThanOrEqualTo(fechaInicio, AHORA);
    }

    @Test
    void sinRestriccion_noFiltraPorEstadoNiPorFechaDeInicio() {
        EventoEspecificaciones.filtrar(SIN_FILTROS, false, null, null).toPredicate(root, query, cb);

        verify(root, never()).get("estado");
        verify(root, never()).get("fechaInicio");
        verify(cb, never()).greaterThanOrEqualTo(any(Path.class), any(LocalDateTime.class));
    }

    @Test
    void soloDisponibles_conRangoDeFechas_mantieneAmbasCondiciones() {
        var filtro = new EventoFiltroRequestDTO(null, AHORA.toLocalDate().minusDays(3), null, null);

        EventoEspecificaciones.filtrar(filtro, true, null, AHORA).toPredicate(root, query, cb);

        verify(cb).greaterThanOrEqualTo(fechaInicio, AHORA);
        verify(cb).greaterThanOrEqualTo(fechaInicio, AHORA.toLocalDate().minusDays(3).atStartOfDay());
        verify(root, never()).fetch(anyString());
    }
}
