package com.eventu.services;

import com.eventu.models.*;
import com.eventu.repositories.EventoRepository;
import com.eventu.repositories.InscripcionRepository;
import com.eventu.repositories.UsuarioRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class InscripcionService {

    private final InscripcionRepository inscripcionRepository;
    private final EventoRepository eventoRepository;
    private final UsuarioRepository usuarioRepository;

    public InscripcionService(InscripcionRepository inscripcionRepository, 
                              EventoRepository eventoRepository, 
                              UsuarioRepository usuarioRepository) {
        this.inscripcionRepository = inscripcionRepository;
        this.eventoRepository = eventoRepository;
        this.usuarioRepository = usuarioRepository;
    }

    @Transactional
    public Inscripcion inscribirEstudiante(Long estudianteId, Long eventoId) {
        Usuario estudiante = usuarioRepository.findById(estudianteId)
                .orElseThrow(() -> new RuntimeException("Estudiante no encontrado."));

        if (estudiante.getRol() != Rol.ESTUDIANTE) {
            throw new RuntimeException("Solo los usuarios con rol ESTUDIANTE pueden inscribirse.");
        }

        Evento evento = eventoRepository.findById(eventoId)
                .orElseThrow(() -> new RuntimeException("Evento no encontrado."));

        if (evento.getEstado() != EstadoEvento.PUBLICADO) {
            throw new RuntimeException("No se permiten inscripciones en eventos que no esten publicados.");
        }

        boolean yaInscrito = inscripcionRepository.existsByEstudianteIdAndEventoId(estudianteId, eventoId);
        if (yaInscrito) {
            throw new RuntimeException("RN01: Ya tienes una inscripcion activa para este evento.");
        }

        if (evento.getCuposDisponibles() <= 0) {
            throw new RuntimeException("RN02: El evento ya no cuenta con cupos disponibles.");
        }

        evento.setCuposDisponibles(evento.getCuposDisponibles() - 1);
        eventoRepository.save(evento);

        Inscripcion inscripcion = new Inscripcion();
        inscripcion.setEstudiante(estudiante);
        inscripcion.setEvento(evento);
        inscripcion.setEstado(EstadoInscripcion.ACTIVA);
        inscripcion.setCodigoQr("EVENTU-QR-" + UUID.randomUUID().toString().toUpperCase());

        return inscripcionRepository.save(inscripcion);
    }

    public List<Inscripcion> listarInscripcionesPorEstudiante(Long estudianteId) {
        return inscripcionRepository.findByEstudianteId(estudianteId);
    }

    public Inscripcion obtenerInscripcionPorId(Long id) {
        return inscripcionRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Inscripcion no encontrada."));
    }
}