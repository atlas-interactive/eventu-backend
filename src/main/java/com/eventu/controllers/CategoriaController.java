package com.eventu.controllers;

import com.eventu.dto.CategoriaRequestDTO;
import com.eventu.dto.CategoriaResponseDTO;
import com.eventu.services.CategoriaService;
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

/** Gestión de categorías de eventos. */
@RestController
@RequestMapping("/api/categorias")
public class CategoriaController {

    private final CategoriaService categoriaService;

    public CategoriaController(CategoriaService categoriaService) {
        this.categoriaService = categoriaService;
    }

    /** Lista las categorías. Con {@code soloActivas=true} devuelve solo las que se pueden elegir al crear un evento */
    @GetMapping
    public ResponseEntity<List<CategoriaResponseDTO>> obtenerCategorias(
            @RequestParam(defaultValue = "false") boolean soloActivas) {
        return ResponseEntity.ok(categoriaService.listarCategorias(soloActivas));
    }

    /** Crea una categoría. */
    @PostMapping
    public ResponseEntity<CategoriaResponseDTO> crearCategoria(@Valid @RequestBody CategoriaRequestDTO request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(categoriaService.crearCategoria(request));
    }

    /** Cambia el nombre de una categoría y, opcionalmente, la activa o desactiva */
    @PutMapping("/{id}")
    public ResponseEntity<CategoriaResponseDTO> editarCategoria(@PathVariable Long id, @Valid @RequestBody CategoriaRequestDTO request) {
        return ResponseEntity.ok(categoriaService.editarCategoria(id, request));
    }
}
