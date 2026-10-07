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
            logService.registrarFallo(administradorId, LogService.ACCION_CREAR_CATEGORIA, LogService.ENTIDAD_CATEGORIA, null, NOMBRE_DUPLICADO);
            throw new ConflictoException(NOMBRE_DUPLICADO);
        }
        Categoria categoria = new Categoria();
        categoria.setNombre(nombre);
        Categoria guardada = categoriaRepository.save(categoria);
        logService.registrarExito(administradorId, LogService.ACCION_CREAR_CATEGORIA,
                LogService.ENTIDAD_CATEGORIA, guardada.getId());
        return aDto(guardada);
    }

    /**
     * Cambia el nombre de una categoría. Solo un administrador puede hacerlo
     * El estado (activa/inactiva) no se toca aquí: para eso están {@link #desactivarCategoria} y {@link #activarCategoria}
     *
     * @throws AccesoDenegadoException si el usuario no es administrador
     * @throws RecursoNoEncontradoException si la categoría no existe
     * @throws ConflictoException si el nuevo nombre ya lo usa otra categoría
     */
    @Transactional
    public CategoriaResponseDTO editarCategoria(Long id, CategoriaRequestDTO request, Long administradorId) {
        exigirAdministrador(administradorId, LogService.ACCION_EDITAR_CATEGORIA, id);
        Categoria categoria = buscarCategoria(id);
        String nombre = request.getNombre().trim();
        if (categoriaRepository.existsByNombreIgnoreCaseAndIdNot(nombre, id)) {
            logService.registrarFallo(administradorId, LogService.ACCION_EDITAR_CATEGORIA, LogService.ENTIDAD_CATEGORIA, id, NOMBRE_DUPLICADO);
            throw new ConflictoException(NOMBRE_DUPLICADO);
        }

        boolean cambiaNombre = !categoria.getNombre().equals(nombre);
        categoria.setNombre(nombre);
        Categoria guardada = categoriaRepository.save(categoria);
        if (cambiaNombre) {
            logService.registrarExito(administradorId, LogService.ACCION_EDITAR_CATEGORIA, LogService.ENTIDAD_CATEGORIA, id);
        }
        return aDto(guardada);
    }

    /**
     * Desactiva una categoría: deja de ofrecerse al crear eventos, pero no se elimina
     * Los eventos que ya la tienen conservan su categoría. Si ya estaba inactiva no hace nada
     */
    @Transactional
    public CategoriaResponseDTO desactivarCategoria(Long id, Long administradorId) {
        return cambiarEstado(id, false, administradorId, LogService.ACCION_DESACTIVAR_CATEGORIA);
    }

    /** Reactiva una categoría para que vuelva a ofrecerse al crear eventos. Si ya estaba activa no hace nada */
    @Transactional
    public CategoriaResponseDTO activarCategoria(Long id, Long administradorId) {
        return cambiarEstado(id, true, administradorId, LogService.ACCION_ACTIVAR_CATEGORIA);
    }

    private CategoriaResponseDTO cambiarEstado(Long id, boolean activo, Long administradorId, String accion) {
        exigirAdministrador(administradorId, accion, id);
        Categoria categoria = buscarCategoria(id);
        if (Boolean.valueOf(activo).equals(categoria.getActivo())) {
            return aDto(categoria);
        }
        categoria.setActivo(activo);
        Categoria guardada = categoriaRepository.save(categoria);
        logService.registrarExito(administradorId, accion, LogService.ENTIDAD_CATEGORIA, id);
        return aDto(guardada);
    }

    private Categoria buscarCategoria(Long id) {
        return categoriaRepository.findById(id).orElseThrow(() -> new RecursoNoEncontradoException(Mensajes.CATEGORIA_NO_ENCONTRADA));
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