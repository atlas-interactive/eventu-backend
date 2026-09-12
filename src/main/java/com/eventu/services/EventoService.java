package com.eventu.services;

import com.eventu.dto.EventoRequestDTO;
import com.eventu.models.Evento;
import com.eventu.models.EstadoEvento;
import com.eventu.models.Rol;
import com.eventu.models.Usuario;
import com.eventu.repositories.EventoRepository;
import com.eventu.repositories.UsuarioRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class EventoService {

    private final EventoRepository eventoRepository;
    private final UsuarioRepository usuarioRepository;

    public EventoService(EventoRepository eventoRepository, UsuarioRepository usuarioRepository) {
        this.eventoRepository = eventoRepository;
        this.usuarioRepository = usuarioRepository;
    }

    public Evento crearEvento(EventoRequestDTO request, Long organizadorId) {
        Usuario organizador = usuarioRepository.findById(organizadorId)
                .orElseThrow(() -> new RuntimeException("Organizador no encontrado."));

        if (organizador.getRol() != Rol.ORGANIZADOR) {
            throw new RuntimeException("El usuario no tiene permisos para crear eventos.");
        }

        Evento nuevoEvento = new Evento();
        nuevoEvento.setTitulo(request.getTitulo());
        nuevoEvento.setDescripcion(request.getDescripcion());
        nuevoEvento.setFechaInicio(request.getFechaInicio());
        nuevoEvento.setUbicacion(request.getUbicacion());
        nuevoEvento.setCuposMaximos(request.getCuposMaximos());

        nuevoEvento.setCuposDisponibles(request.getCuposMaximos());
        nuevoEvento.setCertificable(request.getCertificable());
        nuevoEvento.setEstado(EstadoEvento.PUBLICADO);
        nuevoEvento.setOrganizador(organizador);

        return eventoRepository.save(nuevoEvento);
    }

    public List<Evento> listarEventosActivos() {
        return eventoRepository.findByEstado(EstadoEvento.PUBLICADO);
    }
}