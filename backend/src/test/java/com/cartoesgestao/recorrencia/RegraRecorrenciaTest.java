package com.cartoesgestao.recorrencia;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;

class RegraRecorrenciaTest {

    @Test
    void mensalAvancaUmMes() {
        assertEquals(LocalDate.of(2026, 12, 15), RegraRecorrencia.proxima(LocalDate.of(2026, 11, 15), Periodicidade.MENSAL, 15));
        assertEquals(LocalDate.of(2027, 1, 15), RegraRecorrencia.proxima(LocalDate.of(2026, 12, 15), Periodicidade.MENSAL, 15));
    }

    @Test
    void dia31ViraUltimoDiaEVoltaPara31() {
        LocalDate fev = RegraRecorrencia.proxima(LocalDate.of(2027, 1, 31), Periodicidade.MENSAL, 31);
        assertEquals(LocalDate.of(2027, 2, 28), fev);
        // O dia nominal (31) é que manda, não o dia da data anterior (28).
        assertEquals(LocalDate.of(2027, 3, 31), RegraRecorrencia.proxima(fev, Periodicidade.MENSAL, 31));
    }

    @Test
    void anualRepeteOMesTodoAno() {
        assertEquals(LocalDate.of(2027, 3, 10), RegraRecorrencia.proxima(LocalDate.of(2026, 3, 10), Periodicidade.ANUAL, 10));
        // 29/02 em ano não bissexto
        assertEquals(LocalDate.of(2029, 2, 28), RegraRecorrencia.proxima(LocalDate.of(2028, 2, 29), Periodicidade.ANUAL, 29));
    }

    @Test
    void cobrancasAteHojeInclusive() {
        List<LocalDate> datas = RegraRecorrencia.cobrancas(
                LocalDate.of(2026, 9, 19), Periodicidade.MENSAL, 19, null, LocalDate.of(2026, 11, 19), 100);
        assertEquals(List.of(LocalDate.of(2026, 9, 19), LocalDate.of(2026, 10, 19), LocalDate.of(2026, 11, 19)), datas);
    }

    @Test
    void cobrancaFuturaNaoGeraNada() {
        assertEquals(List.of(), RegraRecorrencia.cobrancas(
                LocalDate.of(2026, 11, 20), Periodicidade.MENSAL, 20, null, LocalDate.of(2026, 11, 19), 100));
    }

    @Test
    void previstasCaemNaFaturaCertaComParcelas() {
        // Cartão fecha dia 5. Anuidade de 480 em 12x cobrada em 10/02/2027 -> 1ª parcela na
        // fatura de mar/27; janela mar..abr pega as parcelas 1 e 2.
        List<RegraRecorrencia.ParcelaPrevista> p = RegraRecorrencia.previstas(
                LocalDate.of(2027, 2, 10), Periodicidade.ANUAL, 10, null, 5,
                new java.math.BigDecimal("480.00"), 12, java.time.YearMonth.of(2027, 3), java.time.YearMonth.of(2027, 4));
        assertEquals(2, p.size());
        assertEquals(java.time.YearMonth.of(2027, 3), p.get(0).competencia());
        assertEquals(1, p.get(0).numero());
        assertEquals(2, p.get(1).numero());
        assertEquals(new java.math.BigDecimal("40.00"), p.get(1).valor());
    }

    @Test
    void previstaMensalUmaPorFatura() {
        // Netflix dia 19, cartão fecha dia 5: cobrança de 19/12 cai na fatura de jan.
        List<RegraRecorrencia.ParcelaPrevista> p = RegraRecorrencia.previstas(
                LocalDate.of(2026, 12, 19), Periodicidade.MENSAL, 19, null, 5,
                new java.math.BigDecimal("44.90"), 1, java.time.YearMonth.of(2027, 1), java.time.YearMonth.of(2027, 1));
        assertEquals(1, p.size());
        assertEquals(LocalDate.of(2026, 12, 19), p.get(0).dataCobranca());
    }

    @Test
    void respeitaDataDeFimELimite() {
        assertEquals(List.of(LocalDate.of(2026, 9, 5), LocalDate.of(2026, 10, 5)), RegraRecorrencia.cobrancas(
                LocalDate.of(2026, 9, 5), Periodicidade.MENSAL, 5, LocalDate.of(2026, 10, 5), LocalDate.of(2027, 1, 1), 100));
        assertEquals(3, RegraRecorrencia.cobrancas(
                LocalDate.of(2000, 1, 1), Periodicidade.MENSAL, 1, null, LocalDate.of(2026, 1, 1), 3).size());
    }
}
