package com.eventu.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.eventu.exceptions.AccesoDenegadoException;
import com.eventu.exceptions.ConflictoException;
import com.eventu.exceptions.RecursoNoEncontradoException;
import com.eventu.models.Asistencia;
import com.eventu.models.EstadoEvento;
import com.eventu.models.EstadoInscripcion;
import com.eventu.models.Evento;
import com.eventu.models.Inscripcion;
import com.eventu.models.Rol;
import com.eventu.models.Usuario;
import com.eventu.repositories.AsistenciaRepository;
import com.eventu.repositories.InscripcionRepository;
import com.eventu.util.Mensajes;

/** Pruebas de la validación del código QR y el registro de asistencia (HU-15) */
@ExtendWith(MockitoExtension.class)
class AsistenciaServiceTest {

    private static final String CODIGO = "qr-de-prueba";
    private static final Long ORGANIZADOR_ID = 2L;
    private static final Long INSCRIPCION_ID = 40L;

    @Mock private AsistenciaRepository asistenciaRepository;
    @Mock private InscripcionRepository inscripcionRepository;
    @Mock private AutorizacionService autorizacionService;
    @Mock private LogService logService;

    private AsistenciaService servicio;
    private Usuario organizador;
    private Inscripcion inscripcion;

    @BeforeEach
    void preparar() {
        servicio = new AsistenciaService(asistenciaRepository, inscripcionRepository, autorizacionService, logService);
        organizador = usuario(ORGANIZADOR_ID, "Organizador A", Rol.ORGANIZADOR);

        Evento evento = new Evento();
        evento.setId(10L);
        evento.setTitulo("Foro de Ingeniería");
        evento.setEstado(EstadoEvento.PUBLICADO);
        evento.setOrganizador(organizador);

        inscripcion = new Inscripcion();
        inscripcion.setId(INSCRIPCION_ID);
        inscripcion.setCodigoQr(CODIGO);
        inscripcion.setEstado(EstadoInscripcion.ACTIVA);
        inscripcion.setEvento(evento);
        inscripcion.setUsuario(usuario(4L, "Estudiante Uno", Rol.USUARIO));
    }

    @Test
    void registrar_conCodigoValidoDelOrganizadorDelEvento_guardaLaAsistenciaYRegistraElLog() {
        prepararValidacion(true);
        when(asistenciaRepository.existsByInscripcionId(INSCRIPCION_ID)).thenReturn(false);
        when(asistenciaRepository.save(any())).thenAnswer(invocacion -> {
            Asistencia asistencia = invocacion.getArgument(0);
            asistencia.setId(90L);
            asistencia.setFechaRegistro(LocalDateTime.now());
            return asistencia;
        });

        Map<String, Object> respuesta = servicio.registrarAsistenciaPorQr(CODIGO, ORGANIZADOR_ID);

        assertEquals("Asistencia registrada exitosamente.", respuesta.get("mensaje"));
        assertEquals("Estudiante Uno", respuesta.get("usuario"));
        assertEquals("Foro de Ingeniería", respuesta.get("evento"));
        verify(logService).registrarExito(ORGANIZADOR_ID, LogService.ACCION_REGISTRAR_ASISTENCIA, LogService.ENTIDAD_ASISTENCIA, 90L);
    }

    @Test
    void registrar_conCodigoInexistente_lanzaNoEncontradoSinGuardar() {
        when(autorizacionService.obtenerGestorDeEventos(ORGANIZADOR_ID)).thenReturn(organizador);
        when(inscripcionRepository.findByCodigoQr("otro")).thenReturn(Optional.empty());

        RecursoNoEncontradoException error = assertThrows(RecursoNoEncontradoException.class,
                () -> servicio.registrarAsistenciaPorQr("otro", ORGANIZADOR_ID));

        assertEquals("Código QR inválido o inscripción no encontrada.", error.getMessage());
        verify(asistenciaRepository, never()).save(any());
    }

    @Test
    void registrar_enEventoAjeno_lanzaAccesoDenegadoYRegistraElIntento() {
        prepararValidacion(false);

        AccesoDenegadoException error = assertThrows(AccesoDenegadoException.class,
                () -> servicio.registrarAsistenciaPorQr(CODIGO, ORGANIZADOR_ID));

        assertEquals("Solo el organizador del evento puede registrar asistencia.", error.getMessage());
        verificarRechazo("Intento de validar un QR de un evento ajeno");
    }

    @Test
    void registrar_enEventoCancelado_lanzaConflictoYRegistraElFallo() {
        prepararValidacion(true);
        inscripcion.getEvento().setEstado(EstadoEvento.CANCELADO);

        ConflictoException error = assertThrows(ConflictoException.class,
                () -> servicio.registrarAsistenciaPorQr(CODIGO, ORGANIZADOR_ID));

        assertEquals("El evento está cancelado; no se puede registrar asistencia.", error.getMessage());
        verificarRechazo("Evento cancelado");
    }

    @Test
    void registrar_conInscripcionCancelada_lanzaConflictoYRegistraElFallo() {
        prepararValidacion(true);
        inscripcion.setEstado(EstadoInscripcion.CANCELADA);

        ConflictoException error = assertThrows(ConflictoException.class,
                () -> servicio.registrarAsistenciaPorQr(CODIGO, ORGANIZADOR_ID));

        assertEquals("La inscripción se encuentra cancelada; no se puede registrar asistencia.", error.getMessage());
        verificarRechazo("Inscripción cancelada");
    }

    @Test
    void registrar_asistenciaYaRegistrada_lanzaConflictoSinDuplicarla() {
        prepararValidacion(true);
        when(asistenciaRepository.existsByInscripcionId(INSCRIPCION_ID)).thenReturn(true);

        ConflictoException error = assertThrows(ConflictoException.class,
                () -> servicio.registrarAsistenciaPorQr(CODIGO, ORGANIZADOR_ID));

        assertEquals("La asistencia de esta inscripción ya fue registrada.", error.getMessage());
        verificarRechazo("Asistencia duplicada");
    }

    @Test
    void registrar_porUsuarioSinRolDeGestion_lanzaAccesoDenegadoSinBuscarElCodigo() {
        when(autorizacionService.obtenerGestorDeEventos(4L))
                .thenThrow(new AccesoDenegadoException(Mensajes.SIN_PERMISO_GESTIONAR_EVENTOS));

        assertThrows(AccesoDenegadoException.class, () -> servicio.registrarAsistenciaPorQr(CODIGO, 4L));

        verify(inscripcionRepository, never()).findByCodigoQr(any());
        verify(asistenciaRepository, never()).save(any());
    }

    private void prepararValidacion(boolean puedeModificar) {
        when(autorizacionService.obtenerGestorDeEventos(ORGANIZADOR_ID)).thenReturn(organizador);
        when(inscripcionRepository.findByCodigoQr(CODIGO)).thenReturn(Optional.of(inscripcion));
        when(autorizacionService.puedeModificarEvento(organizador, inscripcion.getEvento())).thenReturn(puedeModificar);
    }

    private void verificarRechazo(String motivo) {
        verify(asistenciaRepository, never()).save(any());
        verify(logService).registrarFallo(ORGANIZADOR_ID, LogService.ACCION_REGISTRAR_ASISTENCIA,
                LogService.ENTIDAD_INSCRIPCION, INSCRIPCION_ID, motivo);
    }

    private Usuario usuario(Long id, String nombre, Rol rol) {
        Usuario usuario = new Usuario();
        usuario.setId(id);
        usuario.setNombre(nombre);
        usuario.setRol(rol);
        return usuario;
    }
}
