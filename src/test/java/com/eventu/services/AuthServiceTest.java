package com.eventu.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.eventu.dto.RegistroRequestDTO;
import com.eventu.exceptions.ConflictoException;
import com.eventu.exceptions.CredencialesInvalidasException;
import com.eventu.models.Rol;
import com.eventu.models.Usuario;
import com.eventu.repositories.UsuarioRepository;

/** Pruebas de registro (HU-01) e inicio de sesión (HU-02), incluida la reactivación de cuentas desactivadas */
@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    private static final Long USUARIO_ID = 7L;
    private static final String CORREO = "laura@unillanos.edu.co";
    private static final String PASSWORD = "password-segura";
    private static final String HASH = "hash-bcrypt";

    @Mock private UsuarioRepository usuarioRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private LogService logService;

    private AuthService servicio;

    @BeforeEach
    void preparar() {
        servicio = new AuthService(usuarioRepository, passwordEncoder, logService);
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

        Usuario resultado = servicio.autenticar(CORREO, PASSWORD);

        assertTrue(resultado.getActivo());
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

        servicio.autenticar(CORREO, PASSWORD);

        verify(usuarioRepository, never()).save(any());
        verify(logService, never()).registrarExito(any(), eq(LogService.ACCION_REACTIVAR_CUENTA), anyString(), any());
        verify(logService).registrarExito(USUARIO_ID, LogService.ACCION_LOGIN, LogService.ENTIDAD_USUARIO, USUARIO_ID);
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