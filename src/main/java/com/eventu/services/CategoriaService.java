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
import com.eventu.util.Mensajes;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/** Gestión de categorías de eventos */
@Service
public class CategoriaService {

    private static final String NOMBRE_DUPLICADO = "Ya existe una categoría con ese nombre.";

    private final CategoriaRepository categoriaRepository;
    private final EventoRepository eventoRepository;
    private final AutorizacionService autorizacionService;

    public CategoriaService(CategoriaRepository categoriaRepository, EventoRepository eventoRepository,
                            AutorizacionService autorizacionService) {
        this.categoriaRepository = categoriaRepository;
        this.eventoRepository = eventoRepository;
        this.autorizacionService = autorizacionService;
    }

    /**
     * Lista las categorías
     * @param soloActivas {@code true} para el selector de "crear evento", {@code false} para el panel de administración
     */
    @Transactional(readOnly = true)
    public List<CategoriaResponseDTO> listarCategorias(boolean soloActivas) {
        List<Categoria> categorias = soloActivas ? categoriaRepository.findByActivoTrue() : categoriaRepository.findAll();
        return categorias.stream().map(this::aDto).toList();
    }

    /**
     * Crea una categoría activa. Solo un administrador puede hacerlo
     * @throws AccesoDenegadoException si el usuario no es administrador
     * @throws ConflictoException si ya existe una con el mismo nombre (sin distinguir mayúsculas)
     */
    @Transactional
    public CategoriaResponseDTO crearCategoria(CategoriaRequestDTO request, Long administradorId) {
        autorizacionService.obtenerAdministrador(administradorId);
        String nombre = request.getNombre().trim();
        if (categoriaRepository.existsByNombreIgnoreCase(nombre)) {
            throw new ConflictoException(NOMBRE_DUPLICADO);
        }
        Categoria categoria = new Categoria();
        categoria.setNombre(nombre);
        return aDto(categoriaRepository.save(categoria));
    }

    /**
     * Cambia el nombre de una categoría y, si viene en la petición, la activa o desactiva
     * Desactivar no la elimina: los eventos que ya la tienen conservan su categoría. Solo un administrador puede hacerlo
     *
     * @throws AccesoDenegadoException si el usuario no es administrador
     * @throws RecursoNoEncontradoException si la categoría no existe
     * @throws ConflictoException si el nuevo nombre ya lo usa otra categoría
     */
    @Transactional
    public CategoriaResponseDTO editarCategoria(Long id, CategoriaRequestDTO request, Long administradorId) {
        autorizacionService.obtenerAdministrador(administradorId);
        Categoria categoria = categoriaRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException(Mensajes.CATEGORIA_NO_ENCONTRADA));
        String nombre = request.getNombre().trim();
        if (categoriaRepository.existsByNombreIgnoreCaseAndIdNot(nombre, id)) {
            throw new ConflictoException(NOMBRE_DUPLICADO);
        }
        categoria.setNombre(nombre);
        if (request.getActivo() != null) {
            categoria.setActivo(request.getActivo());
        }
        return aDto(categoriaRepository.save(categoria));
    }

    private CategoriaResponseDTO aDto(Categoria categoria) {
        long eventosActivos = eventoRepository.countByCategoriaIdAndEstado(categoria.getId(), EstadoEvento.PUBLICADO);
        return CategoriaResponseDTO.desde(categoria, eventosActivos);
    }
}
