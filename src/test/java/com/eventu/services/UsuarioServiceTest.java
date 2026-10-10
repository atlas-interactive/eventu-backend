package com.eventu.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.eventu.dto.UsuarioResponseDTO;
import com.eventu.models.Rol;
import com.eventu.models.Usuario;
import com.eventu.repositories.UsuarioRepository;

/** Pruebas de la lista de organizadores actuales (HU-03) */
@ExtendWith(MockitoExtension.class)
class UsuarioServiceTest {

    @Mock private UsuarioRepository usuarioRepository;
    @Mock private AutorizacionService autorizacionService;
    @Mock private LogService logService;

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
}
