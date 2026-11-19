package com.cartoesgestao.security;

import com.cartoesgestao.common.UnauthorizedException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/** Lê {@code Authorization: Bearer <token>} e autentica a requisição com um
 * {@link UsuarioAutenticado}. Token ausente ou inválido = segue sem autenticação, e o
 * Spring Security devolve 401 nos endpoints protegidos. */
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;

    public JwtAuthenticationFilter(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getServletPath();
        return path.equals("/api/auth/login")
                || path.equals("/api/auth/cadastro")
                || path.startsWith("/swagger-ui")
                || path.startsWith("/v3/api-docs");
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String header = request.getHeader("Authorization");
        if (header != null && header.startsWith("Bearer ")) {
            try {
                UsuarioAutenticado usuario = jwtService.validar(header.substring(7));
                var authentication = new UsernamePasswordAuthenticationToken(usuario, null, List.of());
                SecurityContextHolder.getContext().setAuthentication(authentication);
            } catch (UnauthorizedException ex) {
                SecurityContextHolder.clearContext();
            }
        }
        filterChain.doFilter(request, response);
    }
}
