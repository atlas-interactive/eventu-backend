package com.eventu.controllers;

import com.eventu.dto.EventoActualizacionRequestDTO;
import com.eventu.dto.EventoRequestDTO;
import com.eventu.dto.EventoResponseDTO;
import com.eventu.models.Evento;
import com.eventu.services.EventoService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/** Creación, edición y consulta de eventos */
@RestController
@RequestMapping("/api/eventos")
public class EventoController {

    private final EventoService eventoService;

    public EventoController(EventoService eventoService) {
        this.eventoService = eventoService;
    }

    /** Crea un evento. El organizador se identifica con {@code organizadorId} hasta que exista el JWT */
    @PostMapping
    public ResponseEntity<Map<String, Object>> crearEvento(@Valid @RequestBody EventoRequestDTO request, @RequestParam Long organizadorId) {
        Evento evento = eventoService.crearEvento(request, organizadorId);
        return ResponseEntity.status(HttpStatus.CREATED).body(Map.<String, Object>of("mensaje", "Evento creado exitosamente.", "eventoId", evento.getId()));
    }

    /** Lista los eventos publicados */
    @GetMapping
    public ResponseEntity<List<EventoResponseDTO>> listarEventos() {
        return ResponseEntity.ok(eventoService.listarEventosActivos());
    }

    /** Edita los campos enviados de un evento. Solo su organizador (o un administrador) puede hacerlo */
    @PutMapping("/{id}")
    public ResponseEntity<Map<String, Object>> editarEvento(@PathVariable Long id,
                                                            @Valid @RequestBody EventoActualizacionRequestDTO request, @RequestParam Long organizadorId) {
        Evento evento = eventoService.actualizarEvento(id, request, organizadorId);
        return ResponseEntity.ok(Map.<String, Object>of("mensaje", "Evento actualizado exitosamente.", "eventoId", evento.getId()));
    }
}
