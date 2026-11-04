package com.eventu.repositories;

import com.eventu.dto.LogFiltroRequestDTO;
import com.eventu.models.Log;
import com.eventu.models.Usuario;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;

/** Arma la consulta dinámica de logs según los filtros recibidos (HU-20) */
public final class LogEspecificaciones {

    private LogEspecificaciones() {
    }

    /** Todos los filtros recibidos deben cumplirse a la vez; los que llegan vacíos no se aplican */
    public static Specification<Log> filtrar(LogFiltroRequestDTO filtro) {
        return (root, query, cb) -> {
            Join<Log, Usuario> usuario = unirUsuario(root, query);

            List<Predicate> condiciones = new ArrayList<>();
            if (filtro.accion() != null) {
                condiciones.add(cb.equal(root.get("accion"), filtro.accion()));
            }
            if (filtro.entidad() != null) {
                condiciones.add(cb.equal(root.get("entidad"), filtro.entidad()));
            }
            if (filtro.resultado() != null) {
                condiciones.add(cb.equal(root.get("resultado"), filtro.resultado()));
            }
            if (filtro.usuario() != null) {
                String patron = "%" + escaparLike(filtro.usuario().toLowerCase()) + "%";
                condiciones.add(cb.or(
                        cb.like(cb.lower(usuario.<String>get("nombre")), patron, '\\'),
                        cb.like(cb.lower(usuario.<String>get("correo")), patron, '\\')));
            }
            if (filtro.fechaDesde() != null) {
                condiciones.add(cb.greaterThanOrEqualTo(root.get("fecha"), filtro.fechaDesde().atStartOfDay()));
            }
            if (filtro.fechaHasta() != null) {
                condiciones.add(cb.lessThan(root.get("fecha"), filtro.fechaHasta().plusDays(1).atStartOfDay()));
            }
            return cb.and(condiciones.toArray(new Predicate[0]));
        };
    }

    /**
     * LEFT JOIN con el usuario para conservar los registros sin usuario asociado. En la consulta de la página
     * se trae el usuario en la misma consulta; en la de conteo basta el join, porque no se devuelven filas
     */
    @SuppressWarnings("unchecked")
    private static Join<Log, Usuario> unirUsuario(Root<Log> root, CriteriaQuery<?> query) {
        boolean esConteo = query != null && (query.getResultType() == Long.class || query.getResultType() == long.class);
        if (esConteo) {
            return root.join("usuario", JoinType.LEFT);
        }
        return (Join<Log, Usuario>) root.<Log, Usuario>fetch("usuario", JoinType.LEFT);
    }

    /** Evita que el administrador use % o _ como comodines en la búsqueda */
    private static String escaparLike(String texto) {
        return texto.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
    }
}
