package com.cartoesgestao.recorrencia.dto;

import com.cartoesgestao.compra.RegraFatura;
import com.cartoesgestao.compra.dto.Ref;
import com.cartoesgestao.recorrencia.Periodicidade;
import com.cartoesgestao.recorrencia.Recorrencia;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;

/**
 * @param proximaFatura fatura em que cai a próxima cobrança (1ª parcela dela).
 * @param encerrada data de fim já passou: não gera mais nada.
 * @param valorMensal custo por mês (anual / 12) - para somar assinaturas e anuidades juntas.
 */
public record RecorrenciaResponse(
        Integer id,
        Ref cartao,
        Ref categoria,
        String descricao,
        BigDecimal valor,
        int qtdParcelas,
        Periodicidade periodicidade,
        int diaCobranca,
        LocalDate proximaCobranca,
        YearMonth proximaFatura,
        LocalDate dataFim,
        boolean ativa,
        boolean encerrada,
        BigDecimal valorMensal,
        String observacao) {

    public static RecorrenciaResponse de(Recorrencia r) {
        var cat = r.getCategoria();
        boolean encerrada = r.getDataFim() != null && r.getProximaCobranca().isAfter(r.getDataFim());
        BigDecimal mensal = r.getPeriodicidade() == Periodicidade.ANUAL
                ? r.getValor().divide(BigDecimal.valueOf(12), 2, RoundingMode.HALF_UP)
                : r.getValor();
        return new RecorrenciaResponse(
                r.getId(),
                new Ref(r.getCartao().getId(), r.getCartao().getNome(), r.getCartao().getCor()),
                cat == null ? null : new Ref(cat.getId(), cat.getNome(), cat.getCor()),
                r.getDescricao(),
                r.getValor(),
                r.getQtdParcelas(),
                r.getPeriodicidade(),
                r.getDiaCobranca(),
                r.getProximaCobranca(),
                RegraFatura.competenciaDaCompra(r.getProximaCobranca(), r.getCartao().getDiaFechamento()),
                r.getDataFim(),
                r.getAtiva(),
                encerrada,
                mensal,
                r.getObservacao());
    }
}
