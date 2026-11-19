package com.cartoesgestao.compartilhamento;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class TokenCompartilhamentoTest {

    @Test
    void tokenTem43CaracteresUrlSafeEEhUnico() {
        String a = TokenCompartilhamento.gerar();
        String b = TokenCompartilhamento.gerar();
        assertEquals(43, a.length());
        assertTrue(TokenCompartilhamento.formatoValido(a));
        assertNotEquals(a, b);
    }

    @Test
    void hashEhDeterministicoEHex64() {
        String h = TokenCompartilhamento.hash("abc");
        assertEquals("ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad", h);
        assertEquals(h, TokenCompartilhamento.hash("abc"));
    }

    @Test
    void formatoInvalidoNaoVaiAoBanco() {
        assertFalse(TokenCompartilhamento.formatoValido(null));
        assertFalse(TokenCompartilhamento.formatoValido(""));
        assertFalse(TokenCompartilhamento.formatoValido("curto"));
        assertFalse(TokenCompartilhamento.formatoValido("a".repeat(42) + "/"));
        assertFalse(TokenCompartilhamento.formatoValido("' or 1=1 --" + "a".repeat(32)));
    }
}
