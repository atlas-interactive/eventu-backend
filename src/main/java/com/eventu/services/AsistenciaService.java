package com.eventu.services;

import com.eventu.models.Asistencia;
import com.eventu.models.EstadoInscripcion;
import com.eventu.models.Inscripcion;
import com.eventu.repositories.AsistenciaRepository;
import com.eventu.repositories.InscripcionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AsistenciaService {

    private final AsistenciaRepository asistenciaRepository;
    private final InscripcionRepository inscripcionRepository;

    public AsistenciaService(AsistenciaRepository asistenciaRepository, InscripcionRepository inscripcionRepository) {
        this.asistenciaRepository = asistenciaRepository;
        this.inscripcionRepository = inscripcionRepository;
    }

    @Transactional
    public Asistencia registrarAsistenciaPorQr(String codigoQr) {
        Inscripcion inscripcion = inscripcionRepository.findByCodigoQr(codigoQr)
                .orElseThrow(() -> new RuntimeException("Codigo QR invalido o inscripcion no encontrada."));

        if (inscripcion.getEstado() == EstadoInscripcion.CANCELADA) {
            throw new RuntimeException("RN08: La inscripción se encuentra cancelada; no se puede registrar asistencia.");
        }

        boolean asistenciaDuplicada = asistenciaRepository.existsByInscripcionId(inscripcion.getId());
        if (asistenciaDuplicada) {
            throw new RuntimeException("RN07: Esta asistencia ya fue registrada previamente para esta inscripción.");
        }

        Asistencia asistencia = new Asistencia();
        asistencia.setInscripcion(inscripcion);

        return asistenciaRepository.save(asistencia);
    }
}