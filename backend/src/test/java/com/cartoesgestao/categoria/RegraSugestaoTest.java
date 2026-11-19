package com.cartoesgestao.categoria;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import com.cartoesgestao.categoria.RegraSugestao.Anterior;
import com.cartoesgestao.categoria.RegraSugestao.CategoriaNome;
import com.cartoesgestao.categoria.RegraSugestao.Origem;
import com.cartoesgestao.categoria.RegraSugestao.Sugestao;
import java.util.List;
import org.junit.jupiter.api.Test;

class RegraSugestaoTest {

    // Ids das categorias padrão de uma conta nova
    static final List<CategoriaNome> CATS = List.of(
            new CategoriaNome(1, "Alimentação"), new CategoriaNome(2, "Mercado"), new CategoriaNome(3, "Transporte"),
            new CategoriaNome(4, "Saúde"), new CategoriaNome(5, "Casa"), new CategoriaNome(6, "Assinaturas"),
            new CategoriaNome(7, "Compras"), new CategoriaNome(8, "Lazer"), new CategoriaNome(9, "Educação"),
            new CategoriaNome(10, "Outros"));

    static Sugestao sem(String d) {
        return RegraSugestao.sugerir(d, List.of(), CATS);
    }

    @Test
    void normalizaDescricaoDeFatura() {
        assertEquals("uber trip", RegraSugestao.normalizar("UBER *TRIP 4417"));
        assertEquals("loja do ze", RegraSugestao.normalizar("MP *LOJA DO ZÉ"));
        assertEquals("99 pop", RegraSugestao.normalizar("99 Pop"));
        assertEquals("pao de acucar", RegraSugestao.normalizar("Pão de Açúcar"));
    }

    @Test
    void palavrasChave() {
        assertEquals(new Sugestao(3, Origem.PALAVRA_CHAVE), sem("UBER *TRIP 4417"));
        assertEquals(new Sugestao(1, Origem.PALAVRA_CHAVE), sem("IFD*Restaurante X"));
        assertEquals(new Sugestao(1, Origem.PALAVRA_CHAVE), sem("Uber Eats"));
        assertEquals(new Sugestao(6, Origem.PALAVRA_CHAVE), sem("NETFLIX.COM"));
        assertEquals(new Sugestao(6, Origem.PALAVRA_CHAVE), sem("Amazon Prime Video"));
        assertEquals(new Sugestao(7, Origem.PALAVRA_CHAVE), sem("AMAZON MARKETPLACE"));
        assertEquals(new Sugestao(7, Origem.PALAVRA_CHAVE), sem("Mercado Livre"));
        assertEquals(new Sugestao(2, Origem.PALAVRA_CHAVE), sem("Mercado Atacadão"));
        assertEquals(new Sugestao(2, Origem.PALAVRA_CHAVE), sem("GBARBOSA ARACAJU"));
        assertEquals(new Sugestao(4, Origem.PALAVRA_CHAVE), sem("Drogasil 123"));
    }

    @Test
    void semCasarVaiParaOutros() {
        assertEquals(new Sugestao(10, Origem.PADRAO), sem("Fulano de Tal ME"));
    }

    @Test
    void semOutrosFicaSemCategoria() {
        assertNull(RegraSugestao.sugerir("Fulano de Tal", List.of(), List.of(new CategoriaNome(1, "Mercado"))));
    }

    @Test
    void historicoVenceAsPalavrasChave() {
        // O usuário classifica Drogasil como Compras, não Saúde
        var hist = List.of(new Anterior("DROGASIL 0451", 7), new Anterior("Drogasil", 7), new Anterior("Mercado", 2));
        assertEquals(new Sugestao(7, Origem.HISTORICO), RegraSugestao.sugerir("drogasil", hist, CATS));
    }

    @Test
    void historicoPelaPrimeiraPalavraEEmpateFicaComAMaisRecente() {
        var hist = List.of(new Anterior("Posto Ipiranga Centro", 5), new Anterior("Posto Shell", 3));
        assertEquals(new Sugestao(5, Origem.HISTORICO), RegraSugestao.sugerir("Posto BR Mania", hist, CATS));
    }
}
