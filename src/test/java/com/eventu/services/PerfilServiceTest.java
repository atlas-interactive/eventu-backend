package com.eventu.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.eventu.dto.PerfilActualizacionDTO;
import com.eventu.dto.PerfilResponseDTO;
import com.eventu.exceptions.ConflictoException;
import com.eventu.exceptions.SolicitudInvalidaException;
import com.eventu.models.EstadoEvento;
import com.eventu.models.EstadoInscripcion;
import com.eventu.models.Evento;
import com.eventu.models.Inscripcion;
import com.eventu.models.Rol;
import com.eventu.models.Usuario;
import com.eventu.notificaciones.ContrasenaCambiada;
import com.eventu.repositories.EventoRepository;
import com.eventu.repositories.InscripcionRepository;
import com.eventu.repositories.UsuarioRepository;
import com.eventu.util.Mensajes;

/** Pruebas de HU-19: consultar y editar el perfil, cambiar la contraseña y desactivar la cuenta */
@ExtendWith(MockitoExtension.class)
class PerfilServiceTest {

    private static final Long USUARIO_ID = 7L;
    private static final String CORREO = "laura@unillanos.edu.co";
    private static final String HASH = "hash-actual";
    private static final String ACTUAL = "clave-actual-123";
    private static final String NUEVA = "clave-nueva-456";

    @Mock private UsuarioRepository usuarioRepository;
    @Mock private EventoRepository eventoRepository;
    @Mock private InscripcionRepository inscripcionRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private RecuperacionContrasenaService recuperacionService;
    @Mock private LogService logService;
    @Mock private ApplicationEventPublisher publicador;

    private PerfilService servicio;

    @BeforeEach
    void preparar() {
        servicio = new PerfilService(usuarioRepository, eventoRepository, inscripcionRepository, passwordEncoder, recuperacionService, logService, publicador);
    }

    // Escenario 1.0: consultar el perfil

    @Test
    void obtenerPerfil_usuarioAutenticado_devuelveNombreCorreoRolYCodigo() {
        when(usuarioRepository.findById(USUARIO_ID)).thenReturn(Optional.of(usuario(Rol.ORGANIZADOR)));

        PerfilResponseDTO perfil = servicio.obtenerPerfil(USUARIO_ID);

        assertEquals("Laura Gomez", perfil.nombre());
        assertEquals(CORREO, perfil.correo());
        assertEquals("ORGANIZADOR", perfil.rol());
        assertEquals("160001234", perfil.codigoInstitucional());
    }

    // Escenario 2.0: editar el perfil

    @Test
    void actualizarPerfil_datosValidos_actualizaNombreYCodigoYRegistraLog() {
        Usuario usuario = usuario(Rol.USUARIO);
        when(usuarioRepository.findById(USUARIO_ID)).thenReturn(Optional.of(usuario));
        when(usuarioRepository.save(usuario)).thenReturn(usuario);

        PerfilResponseDTO perfil = servicio.actualizarPerfil(USUARIO_ID, cambios("  Laura Gómez Ruiz ", " 160009999 ", null));

        assertEquals("Laura Gómez Ruiz", perfil.nombre());
        assertEquals("160009999", perfil.codigoInstitucional());
        verify(logService).registrarExito(USUARIO_ID, LogService.ACCION_EDITAR_PERFIL, LogService.ENTIDAD_USUARIO, USUARIO_ID);
    }

    @Test
    void actualizarPerfil_codigoEnBlanco_loGuardaVacio() {
        Usuario usuario = usuario(Rol.USUARIO);
        when(usuarioRepository.findById(USUARIO_ID)).thenReturn(Optional.of(usuario));
        when(usuarioRepository.save(usuario)).thenReturn(usuario);

        servicio.actualizarPerfil(USUARIO_ID, cambios("Laura Gomez", "   ", CORREO));

        assertNull(usuario.getCodigoInstitucional());
    }

    @Test
    void actualizarPerfil_correoDistinto_lanzaSolicitudInvalidaYNoGuarda() {
        when(usuarioRepository.findById(USUARIO_ID)).thenReturn(Optional.of(usuario(Rol.USUARIO)));

        SolicitudInvalidaException error = assertThrows(SolicitudInvalidaException.class,
                () -> servicio.actualizarPerfil(USUARIO_ID, cambios("Laura Gomez", null, "otra@unillanos.edu.co")));

        assertEquals(PerfilService.CORREO_NO_EDITABLE, error.getMessage());
        verify(usuarioRepository, never()).save(any());
    }

    // Escenario 3.0: cambiar la contraseña

    @Test
    void cambiarContrasena_actualCorrecta_guardaElNuevoHashInvalidaEnlacesYAvisaPorCorreo() {
        Usuario usuario = usuario(Rol.USUARIO);
        when(usuarioRepository.findById(USUARIO_ID)).thenReturn(Optional.of(usuario));
        when(passwordEncoder.matches(ACTUAL, HASH)).thenReturn(true);
        when(passwordEncoder.matches(NUEVA, HASH)).thenReturn(false);
        when(passwordEncoder.encode(NUEVA)).thenReturn("hash-nuevo");

        servicio.cambiarContrasena(USUARIO_ID, ACTUAL, NUEVA);

        assertEquals("hash-nuevo", usuario.getPasswordHash());
        assertNotNull(usuario.getPasswordActualizadaEn());
        verify(recuperacionService).invalidarTokensPendientes(eq(USUARIO_ID), any());
        verify(logService).registrarExito(USUARIO_ID, LogService.ACCION_CAMBIAR_CONTRASENA, LogService.ENTIDAD_USUARIO, USUARIO_ID);
        verify(publicador).publishEvent(any(ContrasenaCambiada.class));
    }

    @Test
    void cambiarContrasena_actualIncorrecta_lanzaSolicitudInvalidaYRegistraElIntento() {
        Usuario usuario = usuario(Rol.USUARIO);
        when(usuarioRepository.findById(USUARIO_ID)).thenReturn(Optional.of(usuario));
        when(passwordEncoder.matches("equivocada", HASH)).thenReturn(false);

        SolicitudInvalidaException error = assertThrows(SolicitudInvalidaException.class,
                () -> servicio.cambiarContrasena(USUARIO_ID, "equivocada", NUEVA));

        assertEquals(Mensajes.CONTRASENA_INCORRECTA, error.getMessage());
        assertEquals(HASH, usuario.getPasswordHash());
        verify(logService).registrarFallo(eq(USUARIO_ID), eq(LogService.ACCION_CAMBIAR_CONTRASENA),
                eq(LogService.ENTIDAD_USUARIO), eq(USUARIO_ID), anyString());
        verify(publicador, never()).publishEvent(any());
    }

    @Test
    void cambiarContrasena_nuevaIgualALaActual_lanzaSolicitudInvalida() {
        when(usuarioRepository.findById(USUARIO_ID)).thenReturn(Optional.of(usuario(Rol.USUARIO)));
        when(passwordEncoder.matches(ACTUAL, HASH)).thenReturn(true);

        SolicitudInvalidaException error = assertThrows(SolicitudInvalidaException.class,
                () -> servicio.cambiarContrasena(USUARIO_ID, ACTUAL, ACTUAL));

        assertEquals(PerfilService.MISMA_CONTRASENA, error.getMessage());
        verify(usuarioRepository, never()).save(any());
    }

    // Escenario 4.0: desactivar la cuenta

    @Test
    void desactivarCuenta_contrasenaCorrectaSinEventos_laDesactivaYRegistraLog() {
        Usuario usuario = usuario(Rol.USUARIO);
        when(usuarioRepository.findById(USUARIO_ID)).thenReturn(Optional.of(usuario));
        when(passwordEncoder.matches(ACTUAL, HASH)).thenReturn(true);

        servicio.desactivarCuenta(USUARIO_ID, ACTUAL);

        assertFalse(usuario.getActivo());
        verify(usuarioRepository).save(usuario);
        verify(logService).registrarExito(USUARIO_ID, LogService.ACCION_DESACTIVAR_CUENTA, LogService.ENTIDAD_USUARIO, USUARIO_ID);
    }

    @Test
    void desactivarCuenta_conInscripcionesEnEventosPorEmpezar_lasCancelaYLiberaLosCupos() {
        Usuario usuario = usuario(Rol.USUARIO);
        Evento evento = new Evento();
        evento.setId(30L);
        evento.setCuposDisponibles(4);
        Inscripcion inscripcion = new Inscripcion();
        inscripcion.setId(90L);
        inscripcion.setEvento(evento);
        inscripcion.setEstado(EstadoInscripcion.ACTIVA);
        when(usuarioRepository.findById(USUARIO_ID)).thenReturn(Optional.of(usuario));
        when(passwordEncoder.matches(ACTUAL, HASH)).thenReturn(true);
        when(inscripcionRepository.findByUsuarioIdAndEstadoAndEventoFechaInicioAfter(eq(USUARIO_ID), eq(EstadoInscripcion.ACTIVA), any(LocalDateTime.class)))
                .thenReturn(List.of(inscripcion));
        when(eventoRepository.buscarPorIdConBloqueo(30L)).thenReturn(Optional.of(evento));

        servicio.desactivarCuenta(USUARIO_ID, ACTUAL);

        assertEquals(EstadoInscripcion.CANCELADA, inscripcion.getEstado());
        assertEquals(5, evento.getCuposDisponibles());
        assertFalse(usuario.getActivo());
        verify(logService).registrarExito(USUARIO_ID, LogService.ACCION_CANCELAR_INSCRIPCION, LogService.ENTIDAD_INSCRIPCION, 90L);
    }

    @Test
    void desactivarCuenta_rechazada_noCancelaNingunaInscripcion() {
        when(usuarioRepository.findById(USUARIO_ID)).thenReturn(Optional.of(usuario(Rol.USUARIO)));
        when(passwordEncoder.matches("equivocada", HASH)).thenReturn(false);

        assertThrows(SolicitudInvalidaException.class, () -> servicio.desactivarCuenta(USUARIO_ID, "equivocada"));

        verify(inscripcionRepository, never()).save(any());
    }

    @Test
    void desactivarCuenta_contrasenaIncorrecta_lanzaSolicitudInvalidaYLaCuentaSigueActiva() {
        Usuario usuario = usuario(Rol.USUARIO);
        when(usuarioRepository.findById(USUARIO_ID)).thenReturn(Optional.of(usuario));
        when(passwordEncoder.matches("equivocada", HASH)).thenReturn(false);

        assertThrows(SolicitudInvalidaException.class, () -> servicio.desactivarCuenta(USUARIO_ID, "equivocada"));

        assertTrue(usuario.getActivo());
        verify(usuarioRepository, never()).save(any());
    }

    @Test
    void desactivarCuenta_unicoAdministradorActivo_lanzaConflicto() {
        Usuario admin = usuario(Rol.ADMIN);
        when(usuarioRepository.findById(USUARIO_ID)).thenReturn(Optional.of(admin));
        when(passwordEncoder.matches(ACTUAL, HASH)).thenReturn(true);
        when(usuarioRepository.countByRolAndActivoTrue(Rol.ADMIN)).thenReturn(1L);

        ConflictoException error = assertThrows(ConflictoException.class, () -> servicio.desactivarCuenta(USUARIO_ID, ACTUAL));

        assertEquals(PerfilService.ADMINISTRADOR_UNICO, error.getMessage());
        assertTrue(admin.getActivo());
        verify(usuarioRepository, never()).save(any());
    }

    @Test
    void desactivarCuenta_administradorConOtrosActivos_laDesactiva() {
        Usuario admin = usuario(Rol.ADMIN);
        when(usuarioRepository.findById(USUARIO_ID)).thenReturn(Optional.of(admin));
        when(passwordEncoder.matches(ACTUAL, HASH)).thenReturn(true);
        when(usuarioRepository.countByRolAndActivoTrue(Rol.ADMIN)).thenReturn(2L);

        servicio.desactivarCuenta(USUARIO_ID, ACTUAL);

        assertFalse(admin.getActivo());
    }

    @Test
    void desactivarCuenta_organizadorConEventosPublicadosPorEmpezar_lanzaConflicto() {
        Usuario organizador = usuario(Rol.ORGANIZADOR);
        when(usuarioRepository.findById(USUARIO_ID)).thenReturn(Optional.of(organizador));
        when(passwordEncoder.matches(ACTUAL, HASH)).thenReturn(true);
        when(eventoRepository.existsByOrganizadorIdAndEstadoAndFechaInicioAfter(eq(USUARIO_ID), eq(EstadoEvento.PUBLICADO), any()))
                .thenReturn(true);

        ConflictoException error = assertThrows(ConflictoException.class, () -> servicio.desactivarCuenta(USUARIO_ID, ACTUAL));

        assertEquals(PerfilService.EVENTOS_PUBLICADOS, error.getMessage());
        assertTrue(organizador.getActivo());
        verify(logService).registrarFallo(eq(USUARIO_ID), eq(LogService.ACCION_DESACTIVAR_CUENTA),
                eq(LogService.ENTIDAD_USUARIO), eq(USUARIO_ID), eq(PerfilService.EVENTOS_PUBLICADOS));
    }

    private Usuario usuario(Rol rol) {
        Usuario usuario = new Usuario();
        usuario.setId(USUARIO_ID);
        usuario.setNombre("Laura Gomez");
        usuario.setCorreo(CORREO);
        usuario.setPasswordHash(HASH);
        usuario.setRol(rol);
        usuario.setActivo(true);
        usuario.setCodigoInstitucional("160001234");
        return usuario;
    }

    private PerfilActualizacionDTO cambios(String nombre, String codigo, String correo) {
        PerfilActualizacionDTO datos = new PerfilActualizacionDTO();
        datos.setNombre(nombre);
        datos.setCodigoInstitucional(codigo);
        datos.setCorreo(correo);
        return datos;
    }
}
