package com.eventu.repositories;

import com.eventu.models.TokenConfirmacionCorreo;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/** Acceso a los datos de la tabla "tokens_confirmacion_correo" */
@Repository
public interface TokenConfirmacionCorreoRepository extends JpaRepository<TokenConfirmacionCorreo, Long> {

    /** Busca el token bloqueando su fila hasta que termine la transacción, para que no se use dos veces a la vez */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT t FROM TokenConfirmacionCorreo t WHERE t.tokenHash = :tokenHash")
    Optional<TokenConfirmacionCorreo> buscarPorHashConBloqueo(@Param("tokenHash") String tokenHash);

    /** Tokens del usuario que todavía no se han usado ni invalidado */
    List<TokenConfirmacionCorreo> findByUsuarioIdAndUsadoEnIsNull(Long usuarioId);

    /** Indica si el usuario pidió un enlace después de esa fecha, para limitar las solicitudes seguidas */
    boolean existsByUsuarioIdAndCreadoEnAfter(Long usuarioId, LocalDateTime fecha);
}
