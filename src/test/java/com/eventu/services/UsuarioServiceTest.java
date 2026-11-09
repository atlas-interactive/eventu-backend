package com.eventu.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.eventu.dto.UsuarioResponseDTO;
import com.eventu.exceptions.AccesoDenegadoException;
import com.eventu.exceptions.ConflictoException;
import com.eventu.exceptions.RecursoNoEncontradoException;
import com.eventu.exceptions.SolicitudInvalidaException;
import com.eventu.models.Rol;
import com.eventu.models.Usuario;
import com.eventu.repositories.UsuarioRepository;
import com.eventu.util.Mensajes;

/** Pruebas de la búsqueda de usuarios y de la asignación y revocación del rol de organizador (HU-03) */
@ExtendWith(MockitoExtension.class)
class UsuarioServiceTest {

    @Mock private UsuarioRepository usuarioRepository;
    @Mock private AutorizacionService autorizacionService;
    @Mock private LogService logService;

    private static final Long ADMIN_ID = 1L;
    private static final Long USUARIO_ID = 5L;

    private UsuarioService servicio;

    @BeforeEach
    void preparar() {
        servicio = new UsuarioService(usuarioRepository, autorizacionService, logService);
    }

    @Test
    void listarOrganizadores_siHayOrganizadores_devuelveSoloLosDeEsteRol() {
        Usuario martin = new Usuario();
        martin.setId(5L);
        martin.setNombre("Martin Pineda");
        martin.setCorreo("martin@unillanos.edu.co");
        martin.setRol(Rol.ORGANIZADOR);
        when(usuarioRepository.findByRolOrderByNombreAsc(Rol.ORGANIZADOR)).thenReturn(List.of(martin));

        List<UsuarioResponseDTO> resultado = servicio.listarOrganizadores();

        assertEquals(1, resultado.size());
        assertEquals("ORGANIZADOR", resultado.get(0).rol());
        assertEquals("martin@unillanos.edu.co", resultado.get(0).correo());
    }

    @Test
    void listarOrganizadores_siNoHayNinguno_devuelveListaVacia() {
        when(usuarioRepository.findByRolOrderByNombreAsc(Rol.ORGANIZADOR)).thenReturn(List.of());

        assertTrue(servicio.listarOrganizadores().isEmpty());
    }

    // Escenario 1: búsqueda

    @Test
    void buscarUsuarios_criterioExistente_devuelveElUsuarioConSuRolActual() {
        when(usuarioRepository.findByCorreoContainingIgnoreCaseOrNombreContainingIgnoreCase(
                "estudiante1@unillanos.edu.co", "estudiante1@unillanos.edu.co"))
                .thenReturn(List.of(usuario(USUARIO_ID, Rol.USUARIO)));

        List<UsuarioResponseDTO> resultado = servicio.buscarUsuarios("  estudiante1@unillanos.edu.co ");

        assertEquals(1, resultado.size());
        assertEquals("USUARIO", resultado.get(0).rol());
    }

    @Test
    void buscarUsuarios_sinCoincidencias_lanzaNoEncontrado() {
        when(usuarioRepository.findByCorreoContainingIgnoreCaseOrNombreContainingIgnoreCase("zzzzzz", "zzzzzz"))
                .thenReturn(List.of());

        RecursoNoEncontradoException error = assertThrows(RecursoNoEncontradoException.class,
                () -> servicio.buscarUsuarios("zzzzzz"));

        assertEquals("No se encontró ningún usuario con ese criterio.", error.getMessage());
    }

    @Test
    void buscarUsuarios_criterioEnBlanco_lanzaSolicitudInvalidaSinConsultar() {
        SolicitudInvalidaException error = assertThrows(SolicitudInvalidaException.class,
                () -> servicio.buscarUsuarios("   "));

        assertEquals("Ingrese un correo o un nombre para buscar.", error.getMessage());
        verifyNoInteractions(usuarioRepository);
    }

    // Escenario 2: asignar el rol

    @Test
    void asignarOrganizador_usuarioConRolUsuario_loPasaAOrganizadorYRegistraElLog() {
        Usuario usuario = usuario(USUARIO_ID, Rol.USUARIO);
        when(usuarioRepository.findById(USUARIO_ID)).thenReturn(Optional.of(usuario));
        when(usuarioRepository.save(usuario)).thenReturn(usuario);

        UsuarioResponseDTO resultado = servicio.asignarOrganizador(USUARIO_ID, ADMIN_ID);

        assertEquals("ORGANIZADOR", resultado.rol());
        verify(autorizacionService).obtenerAdministrador(ADMIN_ID);
        verify(logService).registrarExito(ADMIN_ID, LogService.ACCION_ASIGNAR_ORGANIZADOR, LogService.ENTIDAD_USUARIO, USUARIO_ID);
    }

    @Test
    void asignarOrganizador_usuarioQueYaEsOrganizador_lanzaConflictoYNoGuarda() {
        when(usuarioRepository.findById(3L)).thenReturn(Optional.of(usuario(3L, Rol.ORGANIZADOR)));

        ConflictoException error = assertThrows(ConflictoException.class,
                () -> servicio.asignarOrganizador(3L, ADMIN_ID));

        assertEquals("El usuario ya es organizador.", error.getMessage());
        verify(usuarioRepository, never()).save(any());
        verify(logService).registrarFallo(ADMIN_ID, LogService.ACCION_ASIGNAR_ORGANIZADOR, LogService.ENTIDAD_USUARIO, 3L,
                "El usuario ya es organizador.");
    }

    @Test
    void asignarOrganizador_destinoAdministrador_lanzaConflictoYNoGuarda() {
        when(usuarioRepository.findById(ADMIN_ID)).thenReturn(Optional.of(usuario(ADMIN_ID, Rol.ADMIN)));

        ConflictoException error = assertThrows(ConflictoException.class,
                () -> servicio.asignarOrganizador(ADMIN_ID, ADMIN_ID));

        assertEquals("Un administrador no puede pasar a organizador.", error.getMessage());
        verify(usuarioRepository, never()).save(any());
    }

    @Test
    void asignarOrganizador_usuarioInexistente_lanzaNoEncontradoYRegistraElFallo() {
        when(usuarioRepository.findById(999999L)).thenReturn(Optional.empty());

        RecursoNoEncontradoException error = assertThrows(RecursoNoEncontradoException.class,
                () -> servicio.asignarOrganizador(999999L, ADMIN_ID));

        assertEquals(Mensajes.USUARIO_NO_ENCONTRADO, error.getMessage());
        verify(logService).registrarFallo(ADMIN_ID, LogService.ACCION_ASIGNAR_ORGANIZADOR, LogService.ENTIDAD_USUARIO, 999999L,
                Mensajes.USUARIO_NO_ENCONTRADO);
    }

    // Escenario 3: quien hace el cambio no es administrador

    @Test
    void asignarOrganizador_quienInvocaNoEsAdministrador_lanzaAccesoDenegadoSinCambiarRoles() {
        when(autorizacionService.obtenerAdministrador(4L)).thenThrow(new AccesoDenegadoException(Mensajes.SOLO_ADMINISTRADOR));

        AccesoDenegadoException error = assertThrows(AccesoDenegadoException.class,
                () -> servicio.asignarOrganizador(USUARIO_ID, 4L));

        assertEquals(Mensajes.SOLO_ADMINISTRADOR, error.getMessage());
        verifyNoInteractions(usuarioRepository);
        verify(logService, never()).registrarExito(any(), anyString(), anyString(), any());
    }

    // Escenario 4: revocar el rol

    @Test
    void revocarOrganizador_organizador_vuelveARolUsuarioYConservaLaCuenta() {
        Usuario organizador = usuario(8L, Rol.ORGANIZADOR);
        when(usuarioRepository.findById(8L)).thenReturn(Optional.of(organizador));
        when(usuarioRepository.save(organizador)).thenReturn(organizador);

        UsuarioResponseDTO resultado = servicio.revocarOrganizador(8L, ADMIN_ID);

        assertEquals("USUARIO", resultado.rol());
        assertTrue(organizador.getActivo());
        verify(usuarioRepository, never()).delete(any());
        verify(logService).registrarExito(ADMIN_ID, LogService.ACCION_REVOCAR_ORGANIZADOR, LogService.ENTIDAD_USUARIO, 8L);
    }

    @Test
    void revocarOrganizador_usuarioQueNoEsOrganizador_lanzaConflictoYRegistraElFallo() {
        when(usuarioRepository.findById(USUARIO_ID)).thenReturn(Optional.of(usuario(USUARIO_ID, Rol.USUARIO)));

        ConflictoException error = assertThrows(ConflictoException.class,
                () -> servicio.revocarOrganizador(USUARIO_ID, ADMIN_ID));

        assertEquals("El usuario no es organizador.", error.getMessage());
        verify(usuarioRepository, never()).save(any());
        verify(logService).registrarFallo(ADMIN_ID, LogService.ACCION_REVOCAR_ORGANIZADOR, LogService.ENTIDAD_USUARIO, USUARIO_ID,
                "El usuario no es organizador.");
    }

    private Usuario usuario(Long id, Rol rol) {
        Usuario usuario = new Usuario();
        usuario.setId(id);
        usuario.setNombre("Usuario " + id);
        usuario.setCorreo("usuario" + id + "@unillanos.edu.co");
        usuario.setRol(rol);
        usuario.setActivo(true);
        return usuario;
    }
}
