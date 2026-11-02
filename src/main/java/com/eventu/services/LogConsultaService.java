package com.eventu.services;

import com.eventu.dto.LogFiltroRequestDTO;
import com.eventu.dto.LogResponseDTO;
import com.eventu.dto.OpcionesFiltroLogDTO;
import com.eventu.dto.PaginaResponseDTO;
import com.eventu.exceptions.SolicitudInvalidaException;
import com.eventu.repositories.LogEspecificaciones;
import com.eventu.repositories.LogRepository;
import com.eventu.util.Mensajes;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Consulta del registro de actividad para el administrador (HU-20). Solo lectura: los logs los escribe
 * {@link LogService} y ningún endpoint permite editarlos ni eliminarlos
 */
@Service
public class LogConsultaService {

    static final int TAMANO_MAXIMO = 100;
    static final int MAX_TEXTO_USUARIO = 100;
    static final String PAGINA_INVALIDA = "El número de página no puede ser negativo.";
    static final String TAMANO_INVALIDO = "El tamaño de página debe estar entre 1 y " + TAMANO_MAXIMO + ".";
    static final String RESULTADO_INVALIDO = "El resultado debe ser " + LogService.EXITO + " o " + LogService.FALLIDO + ".";
    static final String USUARIO_DEMASIADO_LARGO = "El texto para buscar el usuario no puede superar los " + MAX_TEXTO_USUARIO + " caracteres.";

    /** Del más reciente al más antiguo; el id desempata los registros guardados en el mismo instante */
    private static final Sort MAS_RECIENTES_PRIMERO = Sort.by(Sort.Order.desc("fecha"), Sort.Order.desc("id"));
    private static final List<String> RESULTADOS = List.of(LogService.EXITO, LogService.FALLIDO);

    private final LogRepository logRepository;

    public LogConsultaService(LogRepository logRepository) {
        this.logRepository = logRepository;
    }

    /**
     * Devuelve una página de logs que cumplen todos los filtros recibidos, del más reciente al más antiguo.
     * Si no hay registros, la página llega con el contenido vacío
     *
     * @param pagina número de página, empezando en 0
     * @param tamano registros por página, entre 1 y {@value #TAMANO_MAXIMO}
     * @throws SolicitudInvalidaException si la paginación, el resultado, el texto del usuario o el rango de fechas no son válidos
     */
    @Transactional(readOnly = true)
    public PaginaResponseDTO<LogResponseDTO> consultarLogs(LogFiltroRequestDTO filtro, int pagina, int tamano) {
        validar(filtro, pagina, tamano);
        Pageable solicitud = PageRequest.of(pagina, tamano, MAS_RECIENTES_PRIMERO);
        return PaginaResponseDTO.desde(logRepository.findAll(LogEspecificaciones.filtrar(filtro), solicitud), LogResponseDTO::desde);
    }

    /** Acciones y entidades que ya aparecen en los logs, y los dos resultados posibles, para llenar los selectores de filtro */
    @Transactional(readOnly = true)
    public OpcionesFiltroLogDTO obtenerOpcionesDeFiltro() {
        return new OpcionesFiltroLogDTO(logRepository.listarAcciones(), logRepository.listarEntidades(), RESULTADOS);
    }

    private void validar(LogFiltroRequestDTO filtro, int pagina, int tamano) {
        if (pagina < 0) {
            throw new SolicitudInvalidaException(PAGINA_INVALIDA);
        }
        if (tamano < 1 || tamano > TAMANO_MAXIMO) {
            throw new SolicitudInvalidaException(TAMANO_INVALIDO);
        }
        if (filtro.resultado() != null && !RESULTADOS.contains(filtro.resultado())) {
            throw new SolicitudInvalidaException(RESULTADO_INVALIDO);
        }
        if (filtro.usuario() != null && filtro.usuario().length() > MAX_TEXTO_USUARIO) {
            throw new SolicitudInvalidaException(USUARIO_DEMASIADO_LARGO);
        }
        if (filtro.fechaDesde() != null && filtro.fechaHasta() != null && filtro.fechaDesde().isAfter(filtro.fechaHasta())) {
            throw new SolicitudInvalidaException(Mensajes.RANGO_FECHAS_INVALIDO);
        }
    }
}
