package com.cartoesgestao.compartilhamento;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.HexFormat;

/** Geração e hash dos tokens de link (sem Spring, testável isolado). */
public final class TokenCompartilhamento {

    private static final SecureRandom RANDOM = new SecureRandom();

    private TokenCompartilhamento() {
    }

    /** 32 bytes aleatórios (256 bits) em base64url sem padding: 43 caracteres, seguros na URL. */
    public static String gerar() {
        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    /** SHA-256 em hex (64 caracteres) - é o que vai para o banco. Token aleatório de 256 bits
     * não precisa de salt/bcrypt: não dá para adivinhar por força bruta. */
    public static String hash(String token) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(token.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 indisponível", e);
        }
    }

    /** Formato do token gerado: evita consulta ao banco com lixo. */
    public static boolean formatoValido(String token) {
        return token != null && token.matches("^[A-Za-z0-9_-]{43}$");
    }
}
