package com.eventu.security;

import com.eventu.models.Usuario;
import com.eventu.repositories.UsuarioRepository;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

/**
 * Lee el token JWT del encabezado Authorization y, si es válido y la cuenta está activa,
 * deja al usuario autenticado para el resto de la petición. 
 */
@Component
public class JwtAuthFilter extends OncePerRequestFilter {

    private static final Logger LOG = LoggerFactory.getLogger(JwtAuthFilter.class);
    private static final String PREFIJO = "Bearer ";

    private final JwtService jwtService;
    private final UsuarioRepository usuarioRepository;

    public JwtAuthFilter(JwtService jwtService, UsuarioRepository usuarioRepository) {
        this.jwtService = jwtService;
        this.usuarioRepository = usuarioRepository;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain) throws ServletException, IOException {
        String cabecera = request.getHeader("Authorization");

        if (cabecera != null && cabecera.startsWith(PREFIJO)) {
            try {
                Claims claims = jwtService.validarYObtenerClaims(cabecera.substring(PREFIJO.length()));
                Long id = Long.valueOf(claims.getSubject());

                usuarioRepository.findById(id)
                        .filter(usuario -> !Boolean.FALSE.equals(usuario.getActivo()))
                        .filter(usuario -> !emitidoAntesDelCambioDeContrasena(claims, usuario))
                        .ifPresent(usuario -> {
                            var principal = new UsuarioAutenticado(usuario.getId(), usuario.getCorreo(), usuario.getRol());
                            var autenticacion = new UsernamePasswordAuthenticationToken(principal, null, List.of(new SimpleGrantedAuthority("ROLE_" + usuario.getRol().name())));
                            SecurityContextHolder.getContext().setAuthentication(autenticacion);
                        });
            } catch (JwtException | IllegalArgumentException e) {
                LOG.warn("Token JWT rechazado en {} {}: {}", request.getMethod(), request.getRequestURI(), e.getClass().getSimpleName());
            }
        }
        chain.doFilter(request, response);
    }

    /**
     * Un token emitido antes del último cambio de contraseña ya no se acepta: así restablecer la contraseña
     * cierra las sesiones abiertas con la anterior. Se compara en segundos porque el JWT guarda la fecha sin milisegundos
     */
    static boolean emitidoAntesDelCambioDeContrasena(Claims claims, Usuario usuario) {
        LocalDateTime cambio = usuario.getPasswordActualizadaEn();
        if (cambio == null || claims.getIssuedAt() == null) {
            return false;
        }
        long emitidoSegundos = claims.getIssuedAt().toInstant().getEpochSecond();
        long cambioSegundos = cambio.atZone(ZoneId.systemDefault()).toEpochSecond();
        return emitidoSegundos < cambioSegundos;
    }
}
