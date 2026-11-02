package com.eventu.repositories;

import com.eventu.models.TokenRecuperacion;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/** Acceso a los datos de la tabla "tokens_recuperacion" */
@Repository
public interface TokenRecuperacionRepository extends JpaRepository<TokenRecuperacion, Long> {

    /**
     * Busca el token bloqueando su fila hasta que termine la transacción.
     * Así dos peticiones simultáneas con el mismo enlace no lo pueden usar dos veces
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT t FROM TokenRecuperacion t WHERE t.tokenHash = :tokenHash")
    Optional<TokenRecuperacion> buscarPorHashConBloqueo(@Param("tokenHash") String tokenHash);

    /** Tokens del usuario que todavía no se han usado ni invalidado */
    List<TokenRecuperacion> findByUsuarioIdAndUsadoEnIsNull(Long usuarioId);

    /** Indica si el usuario pidió un enlace después de esa fecha, para limitar las solicitudes seguidas */
    boolean existsByUsuarioIdAndCreadoEnAfter(Long usuarioId, LocalDateTime fecha);
}
