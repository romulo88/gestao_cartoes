package com.cartoesgestao.dashboard.dto;

import com.cartoesgestao.compra.dto.Ref;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

/**
 * Tudo que o painel mostra para um mês (fatura) escolhido.
 *
 * @param comprometidoFuturo parcelas das faturas depois do mês escolhido.
 * @param serie 6 meses para trás e 6 para frente do mês escolhido (tendência + projeção).
 */
public record DashboardResponse(
        YearMonth competencia,
        BigDecimal totalMes,
        BigDecimal totalMesAnterior,
        BigDecimal comprometidoFuturo,
        List<CartaoNoMes> cartoes,
        List<CategoriaNoMes> categorias,
        List<PontoSerie> serie) {

    /**
     * @param emAberto tudo o que ainda vai cair desse cartão, da fatura atual (hoje) em diante -
     *     é o que "come" o limite.
     * @param limiteDisponivel limite - emAberto (null se o cartão não tem limite cadastrado).
     */
    public record CartaoNoMes(
            Ref cartao,
            BigDecimal totalMes,
            BigDecimal limite,
            BigDecimal emAberto,
            BigDecimal limiteDisponivel,
            LocalDate dataFechamento,
            LocalDate dataVencimento) {
    }

    public record CategoriaNoMes(Ref categoria, BigDecimal total, BigDecimal limiteMensal) {
    }

    /**
     * @param previstoRecorrente cobranças recorrentes que ainda NÃO viraram compra e cairiam
     *     nessa fatura (só previsão: fora do total e do limite).
     */
    public record PontoSerie(
            YearMonth competencia, BigDecimal total, List<TotalPorChave> porCartao, BigDecimal previstoRecorrente) {
    }
}
