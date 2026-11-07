package com.eventu.dto;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;

import com.eventu.models.EstadoInscripcion;
import com.eventu.models.Evento;
import com.eventu.models.Inscripcion;

/** Vista de una inscripción en "Mis inscripciones" (HU-11) y su código QR (HU-14) */
class InscripcionResponseDTOTest {

    @Test
    void desde_inscripcionActiva_incluyeElCodigoQrYLosDatosDelEvento() {
        InscripcionResponseDTO dto = InscripcionResponseDTO.desde(inscripcion(EstadoInscripcion.ACTIVA));

        assertEquals("ACTIVA", dto.estado());
        assertEquals("qr-de-prueba", dto.codigoQr());
        assertEquals(10L, dto.eventoId());
        assertEquals("Foro de Ingeniería", dto.eventoTitulo());
        assertEquals("Auditorio Central", dto.eventoUbicacion());
    }

    @Test
    void desde_inscripcionCancelada_noExponeElCodigoQr() {
        InscripcionResponseDTO dto = InscripcionResponseDTO.desde(inscripcion(EstadoInscripcion.CANCELADA));

        assertEquals("CANCELADA", dto.estado());
        assertNull(dto.codigoQr());
    }

    private Inscripcion inscripcion(EstadoInscripcion estado) {
        Evento evento = new Evento();
        evento.setId(10L);
        evento.setTitulo("Foro de Ingeniería");
        evento.setUbicacion("Auditorio Central");
        evento.setFechaInicio(LocalDateTime.now().plusDays(30));

        Inscripcion inscripcion = new Inscripcion();
        inscripcion.setId(40L);
        inscripcion.setEstado(estado);
        inscripcion.setCodigoQr("qr-de-prueba");
        inscripcion.setFechaInscripcion(LocalDateTime.now());
        inscripcion.setEvento(evento);
        return inscripcion;
    }
}
