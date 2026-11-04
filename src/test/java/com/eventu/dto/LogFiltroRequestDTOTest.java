package com.eventu.dto;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;

/** Normalización de los filtros de la consulta de logs (HU-20) */
class LogFiltroRequestDTOTest {

    @Test
    void valoresEnBlanco_equivalenANoFiltrar() {
        LogFiltroRequestDTO filtro = new LogFiltroRequestDTO("  ", "", " ", "   ", null, null);

        assertNull(filtro.accion());
        assertNull(filtro.entidad());
        assertNull(filtro.resultado());
        assertNull(filtro.usuario());
    }

    @Test
    void accionEntidadYResultado_seComparanEnMayusculasYSinEspacios() {
        LogFiltroRequestDTO filtro = new LogFiltroRequestDTO(" crear_evento ", "evento", "exito", "  Laura  ", null, null);

        assertEquals("CREAR_EVENTO", filtro.accion());
        assertEquals("EVENTO", filtro.entidad());
        assertEquals("EXITO", filtro.resultado());
        assertEquals("Laura", filtro.usuario());
    }
}
