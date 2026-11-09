package com.eventu.services;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.SimpleTransactionStatus;

import com.eventu.models.Log;
import com.eventu.repositories.LogRepository;

/** Registro de las operaciones críticas en la tabla de logs (RN13, HU-20) */
@ExtendWith(MockitoExtension.class)
class LogServiceTest {

    @Mock private LogRepository logRepository;
    @Mock private PlatformTransactionManager transactionManager;

    private LogService servicio;

    @BeforeEach
    void preparar() {
        servicio = new LogService(logRepository, transactionManager);
    }

    @Test
    void registrarExito_guardaElLogConResultadoExitoYSinMotivo() {
        servicio.registrarExito(1L, LogService.ACCION_CREAR_CATEGORIA, LogService.ENTIDAD_CATEGORIA, 5L);

        Log log = guardado();
        assertEquals(1L, log.getUsuarioId());
        assertEquals(LogService.ACCION_CREAR_CATEGORIA, log.getAccion());
        assertEquals(LogService.ENTIDAD_CATEGORIA, log.getEntidad());
        assertEquals(5L, log.getEntidadId());
        assertEquals(LogService.EXITO, log.getResultado());
        assertNull(log.getMotivo());
    }

    @Test
    void registrarFallo_guardaElLogConElMotivoEnUnaTransaccionIndependiente() {
        when(transactionManager.getTransaction(any())).thenReturn(new SimpleTransactionStatus());

        servicio.registrarFallo(null, LogService.ACCION_LOGIN, LogService.ENTIDAD_USUARIO, null, "Credenciales inválidas");

        Log log = guardado();
        assertEquals(LogService.FALLIDO, log.getResultado());
        assertEquals("Credenciales inválidas", log.getMotivo());
        ArgumentCaptor<TransactionDefinition> definicion = ArgumentCaptor.forClass(TransactionDefinition.class);
        verify(transactionManager).getTransaction(definicion.capture());
        assertEquals(TransactionDefinition.PROPAGATION_REQUIRES_NEW, definicion.getValue().getPropagationBehavior());
    }

    @Test
    void registrarFallo_siNoSePuedeGuardarElLog_noInterrumpeLaRespuesta() {
        when(transactionManager.getTransaction(any())).thenReturn(new SimpleTransactionStatus());
        when(logRepository.save(any())).thenThrow(new IllegalStateException("base de datos no disponible"));

        assertDoesNotThrow(() -> servicio.registrarFallo(2L, LogService.ACCION_EDITAR_EVENTO, LogService.ENTIDAD_EVENTO, 10L, "Evento cancelado"));
    }

    private Log guardado() {
        ArgumentCaptor<Log> capturado = ArgumentCaptor.forClass(Log.class);
        verify(logRepository).save(capturado.capture());
        return capturado.getValue();
    }
}
