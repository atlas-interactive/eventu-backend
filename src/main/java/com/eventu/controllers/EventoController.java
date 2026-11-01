package com.eventu.controllers;

import com.eventu.dto.EventoActualizacionRequestDTO;
import com.eventu.dto.EventoRequestDTO;
import com.eventu.dto.EventoResponseDTO;
import com.eventu.dto.InscritoResponseDTO;
import com.eventu.models.Evento;
import com.eventu.security.UsuarioAutenticado;
import com.eventu.services.EventoService;
import com.eventu.services.InscripcionService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.eventu.dto.EventoFiltroRequestDTO;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.RequestParam;
import java.time.LocalDate;

import java.util.List;
import java.util.Map;

/** Creación, edición y consulta de eventos */
@RestController
@RequestMapping("/api/eventos")
public class EventoController {

    private final EventoService eventoService;
    private final InscripcionService inscripcionService;

    public EventoController(EventoService eventoService, InscripcionService inscripcionService) {
        this.eventoService = eventoService;
        this.inscripcionService = inscripcionService;
    }

    /*Lista los eventos publicados (un administrador ve todos, incluidos los cancelados), con filtros opcionales*/
    @GetMapping
    public ResponseEntity<List<EventoResponseDTO>> listarEventos(
            @RequestParam(required = false) Long categoriaId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaDesde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaHasta,
            @RequestParam(required = false) String texto,
            @AuthenticationPrincipal UsuarioAutenticado actual) {
        EventoFiltroRequestDTO filtro = new EventoFiltroRequestDTO(categoriaId, fechaDesde, fechaHasta, texto);
        return ResponseEntity.ok(eventoService.listarEventos(actual.id(), filtro));
    }

    /** Lista los eventos del organizador que consulta, en cualquier estado (incluidos los cancelados) */
    @GetMapping("/mios")
    @PreAuthorize("hasAnyRole('ORGANIZADOR','ADMIN')")
    public ResponseEntity<List<EventoResponseDTO>> listarMisEventos(@AuthenticationPrincipal UsuarioAutenticado actual) {
        return ResponseEntity.ok(eventoService.listarEventosDeOrganizador(actual.id()));
    }

    /** Crea un evento publicado. Solo organizadores y administradores (se identifican con el token) */
    @PostMapping
    @PreAuthorize("hasAnyRole('ORGANIZADOR','ADMIN')")
    public ResponseEntity<Map<String, Object>> crearEvento(@Valid @RequestBody EventoRequestDTO request, @AuthenticationPrincipal UsuarioAutenticado actual) {
        Evento evento = eventoService.crearEvento(request, actual.id());
        return ResponseEntity.status(HttpStatus.CREATED).body(Map.<String, Object>of(
                "mensaje", "Evento creado exitosamente.", "eventoId", evento.getId()));
    }

    /** Edita los campos enviados de un evento. Solo su organizador o un administrador */
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ORGANIZADOR','ADMIN')")
    public ResponseEntity<Map<String, Object>> editarEvento(@PathVariable Long id, @Valid @RequestBody EventoActualizacionRequestDTO request, @AuthenticationPrincipal UsuarioAutenticado actual) {
        Evento evento = eventoService.actualizarEvento(id, request, actual.id());
        return ResponseEntity.ok(Map.<String, Object>of(
                "mensaje", "Evento actualizado exitosamente.", "eventoId", evento.getId()));
    }

    /** Cancela el evento y sus inscripciones activas, y avisa por correo a los inscritos. Solo su organizador o un administrador */
    @PatchMapping("/{id}/cancelar")
    @PreAuthorize("hasAnyRole('ORGANIZADOR','ADMIN')")
    public ResponseEntity<Map<String, Object>> cancelarEvento(@PathVariable Long id, @AuthenticationPrincipal UsuarioAutenticado actual) {
        Evento evento = eventoService.cancelarEvento(id, actual.id());
        return ResponseEntity.ok(Map.<String, Object>of(
                "mensaje", "Evento cancelado exitosamente.", "eventoId", evento.getId()));
    }

    /** Lista los participantes inscritos en un evento y si ya asistieron. Solo su organizador o un administrador */
    @GetMapping("/{id}/inscritos")
    @PreAuthorize("hasAnyRole('ORGANIZADOR','ADMIN')")
    public ResponseEntity<List<InscritoResponseDTO>> listarInscritos(@PathVariable Long id, @AuthenticationPrincipal UsuarioAutenticado actual) {
        return ResponseEntity.ok(inscripcionService.listarInscritosDeEvento(id, actual.id()));
    }

    /**Consulta el detalle de un evento: descripción, fecha, lugar, categoría, cupos y organizador*/
    @GetMapping("/{id}")
    public ResponseEntity<EventoResponseDTO> obtenerEvento(@PathVariable Long id, @AuthenticationPrincipal UsuarioAutenticado actual) {
        return ResponseEntity.ok(eventoService.obtenerEvento(id, actual.id()));
    }
}
