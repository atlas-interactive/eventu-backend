package com.eventu.services;

import com.eventu.dto.EventoRequestDTO;
import com.eventu.models.Evento;
import com.eventu.models.EstadoEvento;
import com.eventu.models.Rol;
import com.eventu.models.Usuario;
import com.eventu.repositories.EventoRepository;
import com.eventu.repositories.UsuarioRepository;
import org.springframework.stereotype.Service;
import com.eventu.models.Categoria;
import com.eventu.repositories.CategoriaRepository;

import java.util.List;

@Service
public class EventoService {

    private final EventoRepository eventoRepository;
    private final UsuarioRepository usuarioRepository;
    private final CategoriaRepository categoriaRepository;

    public EventoService(EventoRepository eventoRepository, UsuarioRepository usuarioRepository, CategoriaRepository categoriaRepository) {
        this.eventoRepository = eventoRepository;
        this.usuarioRepository = usuarioRepository;
        this.categoriaRepository = categoriaRepository;
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
        if (request.getCategoriaId() != null) {
            Categoria categoria = categoriaRepository.findById(request.getCategoriaId()).orElseThrow(() -> new RuntimeException("Categoria no encontrada."));
            nuevoEvento.setCategoria(categoria);
        }
        return eventoRepository.save(nuevoEvento);
    }

    public List<Evento> listarEventosActivos() {
        return eventoRepository.findByEstado(EstadoEvento.PUBLICADO);
    }

    public Evento actualizarEvento(Long eventoId, EventoRequestDTO request, Long organizadorId) {
        Usuario organizador = usuarioRepository.findById(organizadorId)
                .orElseThrow(() -> new RuntimeException("Organizador no encontrado."));

        if (organizador.getRol() != Rol.ORGANIZADOR) {
            throw new RuntimeException("El usuario no tiene permisos para editar eventos.");
        }

        Evento evento = eventoRepository.findById(eventoId)
                .orElseThrow(() -> new RuntimeException("Evento no encontrado."));

        if (evento.getEstado() == EstadoEvento.CANCELADO || evento.getEstado() == EstadoEvento.FINALIZADO) {
            throw new RuntimeException("No se puede editar un evento cancelado o finalizado.");
        }

        if (request.getTitulo() != null) evento.setTitulo(request.getTitulo());
        if (request.getDescripcion() != null) evento.setDescripcion(request.getDescripcion());
        if (request.getFechaInicio() != null) evento.setFechaInicio(request.getFechaInicio());
        if (request.getUbicacion() != null) evento.setUbicacion(request.getUbicacion());
        if (request.getCertificable() != null) evento.setCertificable(request.getCertificable());

        if (request.getCuposMaximos() != null) {
            int inscritos = evento.getCuposMaximos() - evento.getCuposDisponibles();
            if (request.getCuposMaximos() < inscritos) {
                throw new RuntimeException("No se puede reducir la capacidad por debajo de los inscritos actuales (" + inscritos + ").");
            }
            evento.setCuposDisponibles(request.getCuposMaximos() - inscritos);
            evento.setCuposMaximos(request.getCuposMaximos());
        }
        if (request.getCategoriaId() != null) {
            Categoria categoria = categoriaRepository.findById(request.getCategoriaId()).orElseThrow(() -> new RuntimeException("Categoria no encontrada."));
            evento.setCategoria(categoria);
        }

        return eventoRepository.save(evento);
    }
}