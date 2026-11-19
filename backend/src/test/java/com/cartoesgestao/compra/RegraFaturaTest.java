package com.cartoesgestao.compra;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import org.junit.jupiter.api.Test;

class RegraFaturaTest {

    @Test
    void compraAntesDoFechamentoCaiNaFaturaDoMes() {
        // Inter fecha dia 6
        assertEquals(YearMonth.of(2026, 11), RegraFatura.competenciaDaCompra(LocalDate.of(2026, 11, 5), 6));
    }

    @Test
    void compraNoDiaDoFechamentoVaiParaOMesSeguinte() {
        assertEquals(YearMonth.of(2026, 12), RegraFatura.competenciaDaCompra(LocalDate.of(2026, 11, 6), 6));
        assertEquals(YearMonth.of(2027, 1), RegraFatura.competenciaDaCompra(LocalDate.of(2026, 12, 20), 6));
    }

    @Test
    void fechamentoDia29EmFevereiroViraUltimoDia() {
        // XP fecha dia 29; fevereiro/2027 tem 28 dias
        assertEquals(LocalDate.of(2027, 2, 28), RegraFatura.dataFechamento(YearMonth.of(2027, 2), 29));
        assertEquals(YearMonth.of(2027, 2), RegraFatura.competenciaDaCompra(LocalDate.of(2027, 2, 27), 29));
        assertEquals(YearMonth.of(2027, 3), RegraFatura.competenciaDaCompra(LocalDate.of(2027, 2, 28), 29));
    }

    @Test
    void vencimentoNoMesSeguinteQuandoDiaMenorQueFechamento() {
        assertEquals(LocalDate.of(2026, 12, 7), RegraFatura.dataVencimento(YearMonth.of(2026, 11), 29, 7));
        assertEquals(LocalDate.of(2026, 11, 15), RegraFatura.dataVencimento(YearMonth.of(2026, 11), 6, 15));
    }

    @Test
    void divisaoMandaSobraDosCentavosParaPrimeira() {
        assertEquals(List.of(new BigDecimal("33.34"), new BigDecimal("33.33"), new BigDecimal("33.33")),
                RegraFatura.dividir(new BigDecimal("100.00"), 3));
        assertEquals(List.of(new BigDecimal("59.90")), RegraFatura.dividir(new BigDecimal("59.9"), 1));
        BigDecimal soma = RegraFatura.dividir(new BigDecimal("1234.57"), 7).stream().reduce(BigDecimal.ZERO, BigDecimal::add);
        assertEquals(new BigDecimal("1234.57"), soma);
    }

    @Test
    void faturaAbertaEPrimeiraNaoVencida() {
        LocalDate hoje = LocalDate.of(2026, 11, 18);
        // Azul fecha 3, vence 10: a de nov fechou e venceu -> tudo daqui em diante é dez
        assertEquals(YearMonth.of(2026, 12), RegraFatura.faturaAberta(hoje, 3));
        assertEquals(YearMonth.of(2026, 12), RegraFatura.primeiraNaoVencida(hoje, 3, 10));
        // Fecha 6, vence 15, em 10/11: nov fechou mas ainda não venceu -> conta desde nov
        assertEquals(YearMonth.of(2026, 11), RegraFatura.primeiraNaoVencida(LocalDate.of(2026, 11, 10), 6, 15));
        // Fecha 29, vence 7: em 30/11 a aberta é dez, mas a de nov só vence 07/12
        assertEquals(YearMonth.of(2026, 12), RegraFatura.faturaAberta(LocalDate.of(2026, 11, 30), 29));
        assertEquals(YearMonth.of(2026, 11), RegraFatura.primeiraNaoVencida(LocalDate.of(2026, 11, 30), 29, 7));
        // Dia do vencimento ainda conta como a vencer
        assertEquals(YearMonth.of(2026, 11), RegraFatura.primeiraNaoVencida(LocalDate.of(2026, 11, 10), 3, 10));
    }
}
