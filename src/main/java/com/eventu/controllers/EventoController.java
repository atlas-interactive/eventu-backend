package com.eventu.controllers;

import com.eventu.dto.EventoRequestDTO;
import com.eventu.models.Evento;
import com.eventu.services.EventoService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/eventos")
@CrossOrigin(origins = "*")
public class EventoController {

    private final EventoService eventoService;

    public EventoController(EventoService eventoService) {
        this.eventoService = eventoService;
    }

    @PostMapping
    public ResponseEntity<?> crearEvento(@RequestBody EventoRequestDTO request, @RequestParam Long organizadorId) {
        try {
            Evento eventoGuardado = eventoService.crearEvento(request, organizadorId);
            return ResponseEntity.status(HttpStatus.CREATED).body(Map.of(
                "mensaje", "Evento creado exitosamente",
                "eventoId", eventoGuardado.getId()
            ));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping
    public ResponseEntity<List<Evento>> listarEventos() {
        return ResponseEntity.ok(eventoService.listarEventosActivos());
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> editarEvento(@PathVariable Long id, @RequestBody EventoRequestDTO request, @RequestParam Long organizadorId) {
        try {
            Evento eventoActualizado = eventoService.actualizarEvento(id, request, organizadorId);
            return ResponseEntity.ok(Map.of(
                "mensaje", "Evento actualizado exitosamente",
                "eventoId", eventoActualizado.getId()
            ));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}