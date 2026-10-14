package com.eventu.repositories;

import com.eventu.models.Rol;
import com.eventu.models.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/** Acceso a los datos de la tabla "usuarios" */
@Repository
public interface UsuarioRepository extends JpaRepository<Usuario, Long> {

    Optional<Usuario> findByCorreoIgnoreCase(String correo);
    List<Usuario> findByCorreoContainingIgnoreCaseOrNombreContainingIgnoreCase(String correo, String nombre);
    List<Usuario> findByRolOrderByNombreAsc(Rol rol);
}
