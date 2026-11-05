package com.eventu.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
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
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.eventu.dto.RegistroRequestDTO;
import com.eventu.exceptions.AccesoDenegadoException;
import com.eventu.exceptions.ConflictoException;
import com.eventu.exceptions.CredencialesInvalidasException;
import com.eventu.models.Rol;
import com.eventu.models.Usuario;
import com.eventu.notificaciones.ConfirmacionCorreoSolicitada;
import com.eventu.repositories.TokenConfirmacionCorreoRepository;
import com.eventu.repositories.UsuarioRepository;

/**
 * Pruebas de registro (HU-01) e inicio de sesión (HU-02), incluida la reactivación de cuentas desactivadas
 * y la confirmación de correo encendida y apagada
 */
@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    private static final Long USUARIO_ID = 7L;
    private static final String CORREO = "laura@unillanos.edu.co";
    private static final String PASSWORD = "password-segura";
    private static final String HASH = "hash-bcrypt";

    @Mock private UsuarioRepository usuarioRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private LogService logService;
    @Mock private TokenConfirmacionCorreoRepository tokenConfirmacionRepository;
    @Mock private ApplicationEventPublisher publicador;

    private AuthService servicio;

    @BeforeEach
    void preparar() {
        servicio = servicioConConfirmacion(false);
    }

    private AuthService servicioConConfirmacion(boolean exigida) {
        ConfirmacionCorreoService confirmacion = new ConfirmacionCorreoService(
                usuarioRepository, tokenConfirmacionRepository, logService, publicador, exigida);
        return new AuthService(usuarioRepository, passwordEncoder, logService, confirmacion);
    }

    // HU-01 escenario 3: correo de una cuenta desactivada

    @Test
    void registrarUsuario_correoDeCuentaDesactivada_lanzaConflictoConMensajeDeReactivacion() {
        when(usuarioRepository.findByCorreoIgnoreCase(CORREO)).thenReturn(Optional.of(usuario(false)));

        ConflictoException error = assertThrows(ConflictoException.class,
                () -> servicio.registrarUsuario(solicitudRegistro()));

        assertTrue(error.getMessage().contains("desactivada"));
        verify(usuarioRepository, never()).save(any());
        verify(logService, never()).registrarExito(any(), anyString(), anyString(), any());
    }

    @Test
    void registrarUsuario_correoDeCuentaActiva_lanzaConflictoYNoGuarda() {
        when(usuarioRepository.findByCorreoIgnoreCase(CORREO)).thenReturn(Optional.of(usuario(true)));

        ConflictoException error = assertThrows(ConflictoException.class,
                () -> servicio.registrarUsuario(solicitudRegistro()));

        assertEquals("El correo ya se encuentra registrado.", error.getMessage());
        verify(usuarioRepository, never()).save(any());
    }

    // HU-02 escenario 2: reactivación al iniciar sesión

    @Test
    void autenticar_cuentaDesactivadaConCredencialesCorrectas_laReactivaYRegistraLog() {
        Usuario desactivado = usuario(false);
        when(usuarioRepository.findByCorreoIgnoreCase(CORREO)).thenReturn(Optional.of(desactivado));
        when(passwordEncoder.matches(PASSWORD, HASH)).thenReturn(true);

        AuthService.ResultadoAutenticacion resultado = servicio.autenticar(CORREO, PASSWORD);

        assertTrue(resultado.usuario().getActivo());
        assertTrue(resultado.cuentaReactivada());
        verify(usuarioRepository).save(desactivado);
        verify(logService).registrarExito(USUARIO_ID, LogService.ACCION_REACTIVAR_CUENTA, LogService.ENTIDAD_USUARIO, USUARIO_ID);
        verify(logService).registrarExito(USUARIO_ID, LogService.ACCION_LOGIN, LogService.ENTIDAD_USUARIO, USUARIO_ID);
    }

    @Test
    void autenticar_cuentaDesactivadaConPasswordIncorrecta_noLaReactivaYLanzaCredencialesInvalidas() {
        Usuario desactivado = usuario(false);
        when(usuarioRepository.findByCorreoIgnoreCase(CORREO)).thenReturn(Optional.of(desactivado));
        when(passwordEncoder.matches("otra-clave-123", HASH)).thenReturn(false);

        assertThrows(CredencialesInvalidasException.class, () -> servicio.autenticar(CORREO, "otra-clave-123"));

        assertFalse(desactivado.getActivo());
        verify(usuarioRepository, never()).save(any());
        verify(logService).registrarFallo(eq(USUARIO_ID), eq(LogService.ACCION_LOGIN),
                eq(LogService.ENTIDAD_USUARIO), eq(USUARIO_ID), anyString());
        verify(logService, never()).registrarExito(any(), eq(LogService.ACCION_REACTIVAR_CUENTA), anyString(), any());
    }

    @Test
    void autenticar_cuentaActivaConCredencialesCorrectas_noRegistraReactivacion() {
        Usuario activo = usuario(true);
        when(usuarioRepository.findByCorreoIgnoreCase(CORREO)).thenReturn(Optional.of(activo));
        when(passwordEncoder.matches(PASSWORD, HASH)).thenReturn(true);

        AuthService.ResultadoAutenticacion resultado = servicio.autenticar(CORREO, PASSWORD);

        assertFalse(resultado.cuentaReactivada());
        verify(usuarioRepository, never()).save(any());
        verify(logService, never()).registrarExito(any(), eq(LogService.ACCION_REACTIVAR_CUENTA), anyString(), any());
        verify(logService).registrarExito(USUARIO_ID, LogService.ACCION_LOGIN, LogService.ENTIDAD_USUARIO, USUARIO_ID);
    }

    // Confirmación de correo

    @Test
    void registrarUsuario_conConfirmacionApagada_noEnviaCorreoNiDejaLaCuentaPendiente() {
        when(usuarioRepository.findByCorreoIgnoreCase(CORREO)).thenReturn(Optional.empty());
        when(usuarioRepository.save(any())).thenAnswer(invocacion -> conId(invocacion.getArgument(0)));

        servicio.registrarUsuario(solicitudRegistro());

        verify(tokenConfirmacionRepository, never()).save(any());
        verify(publicador, never()).publishEvent(any());
    }

    @Test
    void registrarUsuario_conConfirmacionEncendida_guardaElTokenYPublicaElCorreoDeConfirmacion() {
        servicio = servicioConConfirmacion(true);
        when(usuarioRepository.findByCorreoIgnoreCase(CORREO)).thenReturn(Optional.empty());
        when(usuarioRepository.save(any())).thenAnswer(invocacion -> conId(invocacion.getArgument(0)));

        Usuario guardado = servicio.registrarUsuario(solicitudRegistro());

        assertNull(guardado.getCorreoConfirmadoEn());
        verify(tokenConfirmacionRepository).save(any());
        verify(publicador).publishEvent(any(ConfirmacionCorreoSolicitada.class));
    }

    @Test
    void registrarUsuario_conConfirmacionEncendidaYCuentaSinConfirmar_lanzaConflictoSinReemplazarla() {
        servicio = servicioConConfirmacion(true);
        when(usuarioRepository.findByCorreoIgnoreCase(CORREO)).thenReturn(Optional.of(usuario(true)));

        ConflictoException error = assertThrows(ConflictoException.class,
                () -> servicio.registrarUsuario(solicitudRegistro()));

        assertEquals(AuthService.REGISTRO_SIN_CONFIRMAR, error.getMessage());
        assertTrue(error.getMessage().contains("spam"));
        verify(usuarioRepository, never()).save(any());
    }

    @Test
    void autenticar_conConfirmacionEncendidaYCorreoSinConfirmar_lanzaAccesoDenegadoYRegistraElFallo() {
        servicio = servicioConConfirmacion(true);
        when(usuarioRepository.findByCorreoIgnoreCase(CORREO)).thenReturn(Optional.of(usuario(true)));
        when(passwordEncoder.matches(PASSWORD, HASH)).thenReturn(true);

        AccesoDenegadoException error = assertThrows(AccesoDenegadoException.class,
                () -> servicio.autenticar(CORREO, PASSWORD));

        assertEquals(AuthService.CORREO_SIN_CONFIRMAR, error.getMessage());
        verify(logService).registrarFallo(USUARIO_ID, LogService.ACCION_LOGIN, LogService.ENTIDAD_USUARIO, USUARIO_ID, "Correo sin confirmar");
        verify(logService, never()).registrarExito(any(), eq(LogService.ACCION_LOGIN), anyString(), any());
    }

    @Test
    void autenticar_conConfirmacionEncendidaYPasswordIncorrecta_noRevelaQueFaltaConfirmar() {
        servicio = servicioConConfirmacion(true);
        when(usuarioRepository.findByCorreoIgnoreCase(CORREO)).thenReturn(Optional.of(usuario(true)));
        when(passwordEncoder.matches("otra-clave-123", HASH)).thenReturn(false);

        assertThrows(CredencialesInvalidasException.class, () -> servicio.autenticar(CORREO, "otra-clave-123"));
    }

    @Test
    void autenticar_conConfirmacionEncendidaYCorreoConfirmado_iniciaSesion() {
        servicio = servicioConConfirmacion(true);
        Usuario confirmado = usuario(true);
        confirmado.setCorreoConfirmadoEn(LocalDateTime.now().minusDays(1));
        when(usuarioRepository.findByCorreoIgnoreCase(CORREO)).thenReturn(Optional.of(confirmado));
        when(passwordEncoder.matches(PASSWORD, HASH)).thenReturn(true);

        AuthService.ResultadoAutenticacion resultado = servicio.autenticar(CORREO, PASSWORD);

        assertEquals(confirmado, resultado.usuario());
    }

    @Test
    void autenticar_conConfirmacionApagadaYCorreoSinConfirmar_iniciaSesion() {
        when(usuarioRepository.findByCorreoIgnoreCase(CORREO)).thenReturn(Optional.of(usuario(true)));
        when(passwordEncoder.matches(PASSWORD, HASH)).thenReturn(true);

        AuthService.ResultadoAutenticacion resultado = servicio.autenticar(CORREO, PASSWORD);

        assertFalse(resultado.cuentaReactivada());
    }

    private Usuario conId(Usuario usuario) {
        usuario.setId(USUARIO_ID);
        return usuario;
    }

    private Usuario usuario(boolean activo) {
        Usuario usuario = new Usuario();
        usuario.setId(USUARIO_ID);
        usuario.setNombre("Laura Gomez");
        usuario.setCorreo(CORREO);
        usuario.setPasswordHash(HASH);
        usuario.setRol(Rol.USUARIO);
        usuario.setActivo(activo);
        return usuario;
    }

    private RegistroRequestDTO solicitudRegistro() {
        RegistroRequestDTO solicitud = new RegistroRequestDTO();
        solicitud.setNombre("Laura Gomez");
        solicitud.setCorreo(CORREO);
        solicitud.setPassword(PASSWORD);
        return solicitud;
    }
}