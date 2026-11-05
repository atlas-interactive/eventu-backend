package com.eventu.repositories;

import com.eventu.dto.EventoFiltroRequestDTO;
import com.eventu.models.EstadoEvento;
import com.eventu.models.Evento;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;

/** Arma la consulta dinámica de eventos según los filtros recibidos (HU-08) */
public final class EventoEspecificaciones {

    private EventoEspecificaciones() {
    }

    /**
     * @param soloPublicados true para dejar fuera los eventos cancelados
     * @param organizadorId si no es null, solo los eventos de ese organizador
     */
    public static Specification<Evento> filtrar(EventoFiltroRequestDTO filtro, boolean soloPublicados, Long organizadorId) {
        return (root, query, cb) -> {
            if (query != null && query.getResultType() != Long.class && query.getResultType() != long.class) {
                root.fetch("organizador");
                root.fetch("categoria");
            }

            List<Predicate> condiciones = new ArrayList<>();
            if (soloPublicados) {
                condiciones.add(cb.equal(root.get("estado"), EstadoEvento.PUBLICADO));
            }
            if (organizadorId != null) {
                condiciones.add(cb.equal(root.get("organizador").get("id"), organizadorId));
            }
            if (filtro.categoriaId() != null) {
                condiciones.add(cb.equal(root.get("categoria").get("id"), filtro.categoriaId()));
            }
            if (filtro.fechaDesde() != null) {
                condiciones.add(cb.greaterThanOrEqualTo(root.get("fechaInicio"), filtro.fechaDesde().atStartOfDay()));
            }
            if (filtro.fechaHasta() != null) {
                condiciones.add(cb.lessThan(root.get("fechaInicio"), filtro.fechaHasta().plusDays(1).atStartOfDay()));
            }
            if (filtro.texto() != null) {
                String patron = "%" + escaparLike(filtro.texto().toLowerCase()) + "%";
                condiciones.add(cb.or(
                        cb.like(cb.lower(root.<String>get("titulo")), patron, '\\'),
                        cb.like(cb.lower(root.<String>get("descripcion")), patron, '\\')));
            }
            return cb.and(condiciones.toArray(new Predicate[0]));
        };
    }

    /** Evita que el usuario use % o _ como comodines en la búsqueda */
    private static String escaparLike(String texto) {
        return texto.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
    }
}