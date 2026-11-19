package com.cartoesgestao.security;

import com.cartoesgestao.common.UnauthorizedException;
import com.cartoesgestao.usuario.Usuario;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import javax.crypto.SecretKey;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/** Emite e valida o token de acesso. Diferente do Commander, não há etapa de "contexto":
 * cada usuário só enxerga os próprios cartões, então o token já é o de acesso completo. */
@Service
public class JwtService {

    private final SecretKey key;
    private final long expiracaoMs;

    public JwtService(
            @Value("${jwt.secret}") String secret,
            @Value("${jwt.expiracao-ms}") long expiracaoMs) {
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.expiracaoMs = expiracaoMs;
    }

    public String gerarToken(Usuario usuario) {
        Instant agora = Instant.now();
        return Jwts.builder()
                .subject(usuario.getId().toString())
                .claim("nome", usuario.getNome())
                .claim("email", usuario.getEmail())
                .issuedAt(Date.from(agora))
                .expiration(Date.from(agora.plusMillis(expiracaoMs)))
                .signWith(key)
                .compact();
    }

    public UsuarioAutenticado validar(String token) {
        try {
            Claims claims = Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload();
            return new UsuarioAutenticado(
                    Integer.valueOf(claims.getSubject()),
                    claims.get("nome", String.class),
                    claims.get("email", String.class));
        } catch (JwtException | IllegalArgumentException e) {
            throw new UnauthorizedException("Token inválido ou expirado");
        }
    }
}
