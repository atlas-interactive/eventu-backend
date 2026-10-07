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
    private static final String MOTIVO_NO_ADMINISTRADOR = "El usuario no es administrador";

    private final CategoriaRepository categoriaRepository;
    private final EventoRepository eventoRepository;
    private final AutorizacionService autorizacionService;
    private final LogService logService;

    public CategoriaService(CategoriaRepository categoriaRepository, EventoRepository eventoRepository, AutorizacionService autorizacionService, LogService logService) {
        this.categoriaRepository = categoriaRepository;
        this.eventoRepository = eventoRepository;
        this.autorizacionService = autorizacionService;
        this.logService = logService;
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
        exigirAdministrador(administradorId, LogService.ACCION_CREAR_CATEGORIA, null);
        String nombre = request.getNombre().trim();
        if (categoriaRepository.existsByNombreIgnoreCase(nombre)) {
            logService.registrarFallo(administradorId, LogService.ACCION_CREAR_CATEGORIA,
                    LogService.ENTIDAD_CATEGORIA, null, NOMBRE_DUPLICADO);
            throw new ConflictoException(NOMBRE_DUPLICADO);
        }
        Categoria categoria = new Categoria();
        categoria.setNombre(nombre);
        Categoria guardada = categoriaRepository.save(categoria);
        logService.registrarExito(administradorId, LogService.ACCION_CREAR_CATEGORIA, LogService.ENTIDAD_CATEGORIA, guardada.getId());
        return aDto(guardada);
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
        exigirAdministrador(administradorId, LogService.ACCION_EDITAR_CATEGORIA, id);
        Categoria categoria = categoriaRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException(Mensajes.CATEGORIA_NO_ENCONTRADA));
        String nombre = request.getNombre().trim();
        if (categoriaRepository.existsByNombreIgnoreCaseAndIdNot(nombre, id)) {
            logService.registrarFallo(administradorId, LogService.ACCION_EDITAR_CATEGORIA, LogService.ENTIDAD_CATEGORIA, id, NOMBRE_DUPLICADO);
            throw new ConflictoException(NOMBRE_DUPLICADO);
        }

        Boolean nuevoActivo = request.getActivo();
        boolean cambiaNombre = !categoria.getNombre().equals(nombre);
        boolean cambiaEstado = nuevoActivo != null && !nuevoActivo.equals(categoria.getActivo());

        categoria.setNombre(nombre);
        if (nuevoActivo != null) {
            categoria.setActivo(nuevoActivo);
        }
        Categoria guardada = categoriaRepository.save(categoria);

        if (cambiaNombre) {
            logService.registrarExito(administradorId, LogService.ACCION_EDITAR_CATEGORIA, LogService.ENTIDAD_CATEGORIA, id);
        }
        if (cambiaEstado) {
            String accion = nuevoActivo ? LogService.ACCION_ACTIVAR_CATEGORIA : LogService.ACCION_DESACTIVAR_CATEGORIA;
            logService.registrarExito(administradorId, accion, LogService.ENTIDAD_CATEGORIA, id);
        }
        return aDto(guardada);
    }

    /** Exige rol ADMIN; si no lo tiene, deja el intento rechazado en el log antes de propagar la excepción */
    private void exigirAdministrador(Long administradorId, String accion, Long categoriaId) {
        try {
            autorizacionService.obtenerAdministrador(administradorId);
        } catch (AccesoDenegadoException e) {
            logService.registrarFallo(administradorId, accion, LogService.ENTIDAD_CATEGORIA, categoriaId, MOTIVO_NO_ADMINISTRADOR);
            throw e;
        }
    }

    private CategoriaResponseDTO aDto(Categoria categoria) {
        long eventosActivos = eventoRepository.countByCategoriaIdAndEstado(categoria.getId(), EstadoEvento.PUBLICADO);
        return CategoriaResponseDTO.desde(categoria, eventosActivos);
    }
}