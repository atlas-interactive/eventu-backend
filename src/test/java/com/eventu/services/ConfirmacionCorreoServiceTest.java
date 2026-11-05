package com.eventu.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
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

import com.eventu.exceptions.SolicitudInvalidaException;
import com.eventu.models.Rol;
import com.eventu.models.TokenConfirmacionCorreo;
import com.eventu.models.Usuario;
import com.eventu.notificaciones.ConfirmacionCorreoSolicitada;
import com.eventu.repositories.TokenConfirmacionCorreoRepository;
import com.eventu.repositories.UsuarioRepository;
import com.eventu.util.TokenSeguro;

/** Pruebas de la confirmación del correo: envío y reenvío del enlace y confirmación con el token */
@ExtendWith(MockitoExtension.class)
class ConfirmacionCorreoServiceTest {

    private static final Long USUARIO_ID = 7L;
    private static final String CORREO = "laura@unillanos.edu.co";
    private static final String TOKEN = "token-del-enlace";

    @Mock private UsuarioRepository usuarioRepository;
    @Mock private TokenConfirmacionCorreoRepository tokenRepository;
    @Mock private LogService logService;
    @Mock private ApplicationEventPublisher publicador;

    private ConfirmacionCorreoService servicio;

    @BeforeEach
    void preparar() {
        servicio = new ConfirmacionCorreoService(usuarioRepository, tokenRepository, logService, publicador, true);
    }

    // Interruptor

    @Test
    void puedeIniciarSesion_conConfirmacionApagada_dejaEntrarAunqueNoHayaConfirmado() {
        ConfirmacionCorreoService apagada = new ConfirmacionCorreoService(usuarioRepository, tokenRepository, logService, publicador, false);

        assertFalse(apagada.estaExigida());
        assertTrue(apagada.puedeIniciarSesion(usuario(null)));
    }

    @Test
    void puedeIniciarSesion_conConfirmacionEncendida_soloDejaEntrarCuentasConfirmadas() {
        assertFalse(servicio.puedeIniciarSesion(usuario(null)));
        assertTrue(servicio.puedeIniciarSesion(usuario(LocalDateTime.now())));
    }

    // Envío del enlace

    @Test
    void enviarEnlace_guardaSoloElHashConVigenciaDe24HorasYPublicaElCorreoConElToken() {
        servicio.enviarEnlace(usuario(null));

        ArgumentCaptor<ConfirmacionCorreoSolicitada> hecho = ArgumentCaptor.forClass(ConfirmacionCorreoSolicitada.class);
        verify(publicador).publishEvent(hecho.capture());
        ArgumentCaptor<TokenConfirmacionCorreo> guardado = ArgumentCaptor.forClass(TokenConfirmacionCorreo.class);
        verify(tokenRepository).save(guardado.capture());

        assertEquals(TokenSeguro.hash(hecho.getValue().token()), guardado.getValue().getTokenHash());
        assertEquals(CORREO, hecho.getValue().correo());
        assertEquals(guardado.getValue().getCreadoEn().plus(ConfirmacionCorreoService.VIGENCIA_ENLACE), guardado.getValue().getExpiraEn());
        assertEquals(24, ConfirmacionCorreoService.VIGENCIA_ENLACE.toHours());
    }

    // Reenvío

    @Test
    void reenviarEnlace_cuentaSinConfirmar_invalidaElAnteriorYEnviaUnoNuevo() {
        TokenConfirmacionCorreo anterior = token(LocalDateTime.now().plusHours(5), null);
        when(usuarioRepository.findByCorreoIgnoreCase(CORREO)).thenReturn(Optional.of(usuario(null)));
        when(tokenRepository.findByUsuarioIdAndUsadoEnIsNull(USUARIO_ID)).thenReturn(List.of(anterior));

        servicio.reenviarEnlace("  Laura@Unillanos.edu.co ");

        assertNotNull(anterior.getUsadoEn());
        verify(publicador).publishEvent(any(ConfirmacionCorreoSolicitada.class));
        verify(logService).registrarExito(USUARIO_ID, LogService.ACCION_SOLICITAR_CONFIRMACION, LogService.ENTIDAD_USUARIO, USUARIO_ID);
    }

    @Test
    void reenviarEnlace_correoNoRegistradoOYaConfirmado_noEnviaNada() {
        when(usuarioRepository.findByCorreoIgnoreCase(CORREO)).thenReturn(Optional.empty());
        servicio.reenviarEnlace(CORREO);

        when(usuarioRepository.findByCorreoIgnoreCase(CORREO)).thenReturn(Optional.of(usuario(LocalDateTime.now())));
        servicio.reenviarEnlace(CORREO);

        verify(tokenRepository, never()).save(any());
        verify(publicador, never()).publishEvent(any());
    }

    @Test
    void reenviarEnlace_pidioOtroHaceMenosDeUnMinuto_ignoraLaSolicitud() {
        when(usuarioRepository.findByCorreoIgnoreCase(CORREO)).thenReturn(Optional.of(usuario(null)));
        when(tokenRepository.existsByUsuarioIdAndCreadoEnAfter(eq(USUARIO_ID), any())).thenReturn(true);

        servicio.reenviarEnlace(CORREO);

        verify(tokenRepository, never()).save(any());
        verify(publicador, never()).publishEvent(any());
    }

    @Test
    void reenviarEnlace_conConfirmacionApagada_noEnviaNada() {
        ConfirmacionCorreoService apagada = new ConfirmacionCorreoService(usuarioRepository, tokenRepository, logService, publicador, false);

        apagada.reenviarEnlace(CORREO);

        verifyNoInteractions(usuarioRepository, tokenRepository, publicador);
    }

    // Confirmación con el token

    @Test
    void confirmar_tokenVigente_marcaElCorreoComoConfirmadoYElTokenComoUsado() {
        Usuario usuario = usuario(null);
        TokenConfirmacionCorreo vigente = token(LocalDateTime.now().plusHours(1), null);
        when(tokenRepository.buscarPorHashConBloqueo(TokenSeguro.hash(TOKEN))).thenReturn(Optional.of(vigente));
        when(usuarioRepository.findById(USUARIO_ID)).thenReturn(Optional.of(usuario));
        when(tokenRepository.findByUsuarioIdAndUsadoEnIsNull(USUARIO_ID)).thenReturn(List.of(vigente));

        servicio.confirmar(" " + TOKEN + " ");

        assertNotNull(usuario.getCorreoConfirmadoEn());
        assertNotNull(vigente.getUsadoEn());
        verify(usuarioRepository).save(usuario);
        verify(logService).registrarExito(USUARIO_ID, LogService.ACCION_CONFIRMAR_CORREO, LogService.ENTIDAD_USUARIO, USUARIO_ID);
    }

    @Test
    void confirmar_enlaceAbiertoDosVecesConLaCuentaYaConfirmada_respondeSinErrorYNoCambiaNada() {
        LocalDateTime confirmadoEn = LocalDateTime.now().minusMinutes(1);
        Usuario usuario = usuario(confirmadoEn);
        when(tokenRepository.buscarPorHashConBloqueo(TokenSeguro.hash(TOKEN)))
                .thenReturn(Optional.of(token(LocalDateTime.now().plusHours(1), confirmadoEn)));
        when(usuarioRepository.findById(USUARIO_ID)).thenReturn(Optional.of(usuario));

        servicio.confirmar(TOKEN);

        assertEquals(confirmadoEn, usuario.getCorreoConfirmadoEn());
        verify(usuarioRepository, never()).save(any());
    }

    @Test
    void confirmar_tokenVencidoSinConfirmar_lanzaSolicitudInvalidaYRegistraElFallo() {
        when(tokenRepository.buscarPorHashConBloqueo(TokenSeguro.hash(TOKEN)))
                .thenReturn(Optional.of(token(LocalDateTime.now().minusMinutes(1), null)));
        when(usuarioRepository.findById(USUARIO_ID)).thenReturn(Optional.of(usuario(null)));

        SolicitudInvalidaException error = assertThrows(SolicitudInvalidaException.class, () -> servicio.confirmar(TOKEN));

        assertEquals(ConfirmacionCorreoService.ENLACE_INVALIDO, error.getMessage());
        verify(usuarioRepository, never()).save(any());
        verify(logService).registrarFallo(eq(USUARIO_ID), eq(LogService.ACCION_CONFIRMAR_CORREO),
                eq(LogService.ENTIDAD_USUARIO), eq(USUARIO_ID), anyString());
    }

    @Test
    void confirmar_tokenInexistente_lanzaSolicitudInvalida() {
        when(tokenRepository.buscarPorHashConBloqueo(anyString())).thenReturn(Optional.empty());

        assertThrows(SolicitudInvalidaException.class, () -> servicio.confirmar("inventado"));

        verify(usuarioRepository, never()).save(any());
    }

    private Usuario usuario(LocalDateTime confirmadoEn) {
        Usuario usuario = new Usuario();
        usuario.setId(USUARIO_ID);
        usuario.setNombre("Laura Gomez");
        usuario.setCorreo(CORREO);
        usuario.setRol(Rol.USUARIO);
        usuario.setCorreoConfirmadoEn(confirmadoEn);
        return usuario;
    }

    private TokenConfirmacionCorreo token(LocalDateTime expiraEn, LocalDateTime usadoEn) {
        TokenConfirmacionCorreo token = new TokenConfirmacionCorreo();
        token.setUsuarioId(USUARIO_ID);
        token.setTokenHash(TokenSeguro.hash(TOKEN));
        token.setExpiraEn(expiraEn);
        token.setUsadoEn(usadoEn);
        return token;
    }
}
