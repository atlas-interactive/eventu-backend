package com.eventu.repositories;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.eventu.models.Usuario;

@Repository
public interface UsuarioRepository extends JpaRepository<Usuario, Long> {

    Optional<Usuario> findByCorreoIgnoreCase(String correo);
    List<Usuario> findByCorreoContainingIgnoreCaseOrNombreContainingIgnoreCase(String correo, String nombre);

    boolean existsByCorreoIgnoreCase(String correo);
}
