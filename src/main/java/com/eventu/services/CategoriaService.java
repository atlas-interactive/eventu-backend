package com.eventu.services;

import com.eventu.dto.CategoriaRequestDTO;
import com.eventu.dto.CategoriaResponseDTO;
import com.eventu.exceptions.ConflictoException;
import com.eventu.exceptions.RecursoNoEncontradoException;
import com.eventu.models.Categoria;
import com.eventu.repositories.CategoriaRepository;
import com.eventu.util.Mensajes;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/** Gestión de categorías de eventos */
@Service
public class CategoriaService {

    private static final String NOMBRE_DUPLICADO = "Ya existe una categoría con ese nombre.";

    private final CategoriaRepository categoriaRepository;

    public CategoriaService(CategoriaRepository categoriaRepository) {
        this.categoriaRepository = categoriaRepository;
    }

    /**
     * Lista las categorías
     * @param soloActivas {@code true} para el selector de "crear evento", {@code false} para el panel de administración
     */
    @Transactional(readOnly = true)
    public List<CategoriaResponseDTO> listarCategorias(boolean soloActivas) {
        List<Categoria> categorias = soloActivas ? categoriaRepository.findByActivoTrue() : categoriaRepository.findAll();
        return categorias.stream().map(CategoriaResponseDTO::desde).toList();
    }

    /**
     * Crea una categoría activa
     * @throws ConflictoException si ya existe una con el mismo nombre (sin distinguir mayúsculas)
     */
    @Transactional
    public CategoriaResponseDTO crearCategoria(CategoriaRequestDTO request) {
        String nombre = request.getNombre().trim();
        if (categoriaRepository.existsByNombreIgnoreCase(nombre)) {
            throw new ConflictoException(NOMBRE_DUPLICADO);
        }
        Categoria categoria = new Categoria();
        categoria.setNombre(nombre);
        return CategoriaResponseDTO.desde(categoriaRepository.save(categoria));
    }

    /**
     * Cambia el nombre de una categoría y, si viene en la petición, la activa o desactiva
     * Desactivar no la elimina: los eventos que ya la tienen conservan su categoría
     *
     * @throws RecursoNoEncontradoException si la categoría no existe
     * @throws ConflictoException si el nuevo nombre ya lo usa otra categoría
     */
    @Transactional
    public CategoriaResponseDTO editarCategoria(Long id, CategoriaRequestDTO request) {
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
        return CategoriaResponseDTO.desde(categoriaRepository.save(categoria));
    }
}
