package com.eventu.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
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
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.eventu.exceptions.SolicitudInvalidaException;
import com.eventu.models.Rol;
import com.eventu.models.TokenRecuperacion;
import com.eventu.models.Usuario;
import com.eventu.notificaciones.ContrasenaCambiada;
import com.eventu.notificaciones.RecuperacionContrasenaSolicitada;
import com.eventu.repositories.TokenRecuperacionRepository;
import com.eventu.repositories.UsuarioRepository;

/** Pruebas de la recuperación de contraseña olvidada: solicitud del enlace y restablecimiento con el token */
@ExtendWith(MockitoExtension.class)
class RecuperacionContrasenaServiceTest {

    private static final Long USUARIO_ID = 7L;
    private static final String CORREO = "laura@unillanos.edu.co";
    private static final String TOKEN = "token-del-enlace";
    private static final String NUEVA = "nueva-clave-segura";

    @Mock private UsuarioRepository usuarioRepository;
    @Mock private TokenRecuperacionRepository tokenRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private LogService logService;
    @Mock private ApplicationEventPublisher publicador;

    private RecuperacionContrasenaService servicio;

    @BeforeEach
    void preparar() {
        servicio = new RecuperacionContrasenaService(usuarioRepository, tokenRepository, passwordEncoder, logService, publicador);
    }

    // Solicitud del enlace

    @Test
    void solicitarRecuperacion_correoRegistrado_guardaSoloElHashYPublicaElCorreoConElToken() {
        when(usuarioRepository.findByCorreoIgnoreCase(CORREO)).thenReturn(Optional.of(usuario()));

        servicio.solicitarRecuperacion("  Laura@Unillanos.edu.co ");

        ArgumentCaptor<RecuperacionContrasenaSolicitada> hecho = ArgumentCaptor.forClass(RecuperacionContrasenaSolicitada.class);
        verify(publicador).publishEvent(hecho.capture());
        ArgumentCaptor<TokenRecuperacion> guardado = ArgumentCaptor.forClass(TokenRecuperacion.class);
        verify(tokenRepository).save(guardado.capture());

        String token = hecho.getValue().token();
        assertTrue(token.length() >= 43, "32 bytes en Base64 sin relleno");
        assertEquals(RecuperacionContrasenaService.hash(token), guardado.getValue().getTokenHash());
        assertNotEquals(token, guardado.getValue().getTokenHash());
        assertEquals(CORREO, hecho.getValue().correo());
        assertEquals(guardado.getValue().getCreadoEn().plus(RecuperacionContrasenaService.VIGENCIA_ENLACE),
                guardado.getValue().getExpiraEn());
        verify(logService).registrarExito(USUARIO_ID, LogService.ACCION_SOLICITAR_RECUPERACION, LogService.ENTIDAD_USUARIO, USUARIO_ID);
    }

    @Test
    void solicitarRecuperacion_correoNoRegistrado_noGuardaNiEnviaNada() {
        when(usuarioRepository.findByCorreoIgnoreCase(CORREO)).thenReturn(Optional.empty());

        servicio.solicitarRecuperacion(CORREO);

        verify(tokenRepository, never()).save(any());
        verify(publicador, never()).publishEvent(any());
    }

    @Test
    void solicitarRecuperacion_pidioOtroEnlaceHaceMenosDeUnMinuto_ignoraLaSolicitud() {
        when(usuarioRepository.findByCorreoIgnoreCase(CORREO)).thenReturn(Optional.of(usuario()));
        when(tokenRepository.existsByUsuarioIdAndCreadoEnAfter(eq(USUARIO_ID), any())).thenReturn(true);

        servicio.solicitarRecuperacion(CORREO);

        verify(tokenRepository, never()).save(any());
        verify(publicador, never()).publishEvent(any());
    }

    @Test
    void solicitarRecuperacion_conEnlacesAnteriores_losInvalidaParaQueSoloSirvaElUltimo() {
        TokenRecuperacion anterior = token(LocalDateTime.now().plusMinutes(20), null);
        when(usuarioRepository.findByCorreoIgnoreCase(CORREO)).thenReturn(Optional.of(usuario()));
        when(tokenRepository.findByUsuarioIdAndUsadoEnIsNull(USUARIO_ID)).thenReturn(List.of(anterior));

        servicio.solicitarRecuperacion(CORREO);

        assertNotNull(anterior.getUsadoEn());
    }

    // Restablecimiento con el token

    @Test
    void restablecerContrasena_tokenVigente_cambiaLaContrasenaMarcaElTokenUsadoYAvisaPorCorreo() {
        Usuario usuario = usuario();
        TokenRecuperacion vigente = token(LocalDateTime.now().plusMinutes(10), null);
        when(tokenRepository.buscarPorHashConBloqueo(RecuperacionContrasenaService.hash(TOKEN))).thenReturn(Optional.of(vigente));
        when(usuarioRepository.findById(USUARIO_ID)).thenReturn(Optional.of(usuario));
        when(tokenRepository.findByUsuarioIdAndUsadoEnIsNull(USUARIO_ID)).thenReturn(List.of(vigente));
        when(passwordEncoder.encode(NUEVA)).thenReturn("hash-nuevo");

        servicio.restablecerContrasena(TOKEN, NUEVA);

        assertEquals("hash-nuevo", usuario.getPasswordHash());
        assertNotNull(usuario.getPasswordActualizadaEn());
        assertNotNull(vigente.getUsadoEn());
        verify(usuarioRepository).save(usuario);
        verify(logService).registrarExito(USUARIO_ID, LogService.ACCION_RESTABLECER_CONTRASENA, LogService.ENTIDAD_USUARIO, USUARIO_ID);
        verify(publicador).publishEvent(any(ContrasenaCambiada.class));
    }

    @Test
    void restablecerContrasena_tokenVencido_lanzaSolicitudInvalidaYNoCambiaNada() {
        TokenRecuperacion vencido = token(LocalDateTime.now().minusMinutes(1), null);
        when(tokenRepository.buscarPorHashConBloqueo(RecuperacionContrasenaService.hash(TOKEN))).thenReturn(Optional.of(vencido));

        SolicitudInvalidaException error = assertThrows(SolicitudInvalidaException.class,
                () -> servicio.restablecerContrasena(TOKEN, NUEVA));

        assertEquals(RecuperacionContrasenaService.ENLACE_INVALIDO, error.getMessage());
        verify(usuarioRepository, never()).save(any());
        verify(logService).registrarFallo(eq(USUARIO_ID), eq(LogService.ACCION_RESTABLECER_CONTRASENA),
                eq(LogService.ENTIDAD_USUARIO), eq(USUARIO_ID), anyString());
    }

    @Test
    void restablecerContrasena_tokenYaUsado_lanzaSolicitudInvalida() {
        TokenRecuperacion usado = token(LocalDateTime.now().plusMinutes(10), LocalDateTime.now().minusMinutes(2));
        when(tokenRepository.buscarPorHashConBloqueo(RecuperacionContrasenaService.hash(TOKEN))).thenReturn(Optional.of(usado));

        assertThrows(SolicitudInvalidaException.class, () -> servicio.restablecerContrasena(TOKEN, NUEVA));

        verify(usuarioRepository, never()).save(any());
        verify(publicador, never()).publishEvent(any());
    }

    @Test
    void restablecerContrasena_tokenInexistente_lanzaSolicitudInvalida() {
        when(tokenRepository.buscarPorHashConBloqueo(anyString())).thenReturn(Optional.empty());

        assertThrows(SolicitudInvalidaException.class, () -> servicio.restablecerContrasena("inventado", NUEVA));

        verify(usuarioRepository, never()).save(any());
    }

    @Test
    void hash_esSha256EnHexadecimalYSiempreIgualParaElMismoToken() {
        String hash = RecuperacionContrasenaService.hash(TOKEN);

        assertEquals(64, hash.length());
        assertEquals(hash, RecuperacionContrasenaService.hash(TOKEN));
        assertNotEquals(hash, RecuperacionContrasenaService.hash(TOKEN + "x"));
    }

    private Usuario usuario() {
        Usuario usuario = new Usuario();
        usuario.setId(USUARIO_ID);
        usuario.setNombre("Laura Gomez");
        usuario.setCorreo(CORREO);
        usuario.setPasswordHash("hash-anterior");
        usuario.setRol(Rol.USUARIO);
        return usuario;
    }

    private TokenRecuperacion token(LocalDateTime expiraEn, LocalDateTime usadoEn) {
        TokenRecuperacion token = new TokenRecuperacion();
        token.setUsuarioId(USUARIO_ID);
        token.setTokenHash(RecuperacionContrasenaService.hash(TOKEN));
        token.setExpiraEn(expiraEn);
        token.setUsadoEn(usadoEn);
        return token;
    }
}
