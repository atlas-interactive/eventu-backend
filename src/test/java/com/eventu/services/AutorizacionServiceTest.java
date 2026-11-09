package com.eventu.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.eventu.exceptions.AccesoDenegadoException;
import com.eventu.exceptions.RecursoNoEncontradoException;
import com.eventu.models.Evento;
import com.eventu.models.Rol;
import com.eventu.models.Usuario;
import com.eventu.repositories.UsuarioRepository;
import com.eventu.util.Mensajes;

/** Verificaciones de rol y de propiedad del evento que usan los demás servicios (RN16 y RN17) */
@ExtendWith(MockitoExtension.class)
class AutorizacionServiceTest {

    @Mock private UsuarioRepository usuarioRepository;

    private AutorizacionService servicio;

    @BeforeEach
    void preparar() {
        servicio = new AutorizacionService(usuarioRepository);
    }

    @Test
    void obtenerUsuario_inexistente_lanzaNoEncontrado() {
        when(usuarioRepository.findById(99L)).thenReturn(Optional.empty());

        RecursoNoEncontradoException error = assertThrows(RecursoNoEncontradoException.class, () -> servicio.obtenerUsuario(99L));

        assertEquals(Mensajes.USUARIO_NO_ENCONTRADO, error.getMessage());
    }

    @Test
    void obtenerGestorDeEventos_organizadorOAdministrador_loDevuelve() {
        Usuario organizador = registrar(2L, Rol.ORGANIZADOR);
        Usuario administrador = registrar(1L, Rol.ADMIN);

        assertSame(organizador, servicio.obtenerGestorDeEventos(2L));
        assertSame(administrador, servicio.obtenerGestorDeEventos(1L));
    }

    @Test
    void obtenerGestorDeEventos_usuarioSinRolDeGestion_lanzaAccesoDenegado() {
        registrar(4L, Rol.USUARIO);

        AccesoDenegadoException error = assertThrows(AccesoDenegadoException.class, () -> servicio.obtenerGestorDeEventos(4L));

        assertEquals(Mensajes.SIN_PERMISO_GESTIONAR_EVENTOS, error.getMessage());
    }

    @Test
    void obtenerAdministrador_administrador_loDevuelve() {
        Usuario administrador = registrar(1L, Rol.ADMIN);

        assertSame(administrador, servicio.obtenerAdministrador(1L));
    }

    @Test
    void obtenerAdministrador_organizador_lanzaAccesoDenegado() {
        registrar(2L, Rol.ORGANIZADOR);

        AccesoDenegadoException error = assertThrows(AccesoDenegadoException.class, () -> servicio.obtenerAdministrador(2L));

        assertEquals(Mensajes.SOLO_ADMINISTRADOR, error.getMessage());
    }

    @Test
    void puedeModificarEvento_soloElOrganizadorDuenoOElAdministrador() {
        Usuario dueno = usuario(2L, Rol.ORGANIZADOR);
        Evento evento = new Evento();
        evento.setOrganizador(dueno);

        assertTrue(servicio.puedeModificarEvento(dueno, evento));
        assertTrue(servicio.puedeModificarEvento(usuario(1L, Rol.ADMIN), evento));
        assertFalse(servicio.puedeModificarEvento(usuario(3L, Rol.ORGANIZADOR), evento));
    }

    private Usuario registrar(Long id, Rol rol) {
        Usuario usuario = usuario(id, rol);
        when(usuarioRepository.findById(id)).thenReturn(Optional.of(usuario));
        return usuario;
    }

    private Usuario usuario(Long id, Rol rol) {
        Usuario usuario = new Usuario();
        usuario.setId(id);
        usuario.setRol(rol);
        return usuario;
    }
}
