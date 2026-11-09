package com.eventu.repositories;

import com.eventu.dto.EventoFiltroRequestDTO;
import com.eventu.models.EstadoEvento;
import com.eventu.models.Evento;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/** Arma la consulta dinámica de eventos según los filtros recibidos (HU-08) */
public final class EventoEspecificaciones {

    private EventoEspecificaciones() {
    }

    /**
     * @param soloDisponibles true para dejar solo los eventos publicados que aún no han iniciado
     * @param organizadorId si no es null, solo los eventos de ese organizador
     * @param ahora momento contra el que se compara la fecha de inicio cuando soloDisponibles es true
     */
    public static Specification<Evento> filtrar(EventoFiltroRequestDTO filtro, boolean soloDisponibles, Long organizadorId,
                                                LocalDateTime ahora) {
        return (root, query, cb) -> {
            if (query != null && query.getResultType() != Long.class && query.getResultType() != long.class) {
                root.fetch("organizador");
                root.fetch("categoria");
            }

            List<Predicate> condiciones = new ArrayList<>();
            if (soloDisponibles) {
                condiciones.add(cb.equal(root.get("estado"), EstadoEvento.PUBLICADO));
                condiciones.add(cb.greaterThanOrEqualTo(root.get("fechaInicio"), ahora));
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