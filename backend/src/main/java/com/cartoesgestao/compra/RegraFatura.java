package com.cartoesgestao.compra;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;

/**
 * Regras de fatura, sem dependência de Spring/JPA (dá para testar isolado).
 *
 * <p><b>Competência</b> = mês em que a fatura FECHA. Uma compra feita antes do dia de
 * fechamento cai na fatura que fecha no mesmo mês; feita no dia do fechamento ou depois,
 * vai para a do mês seguinte (o "melhor dia de compra" é o próprio dia do fechamento).
 *
 * <p>Dia de fechamento/vencimento maior que o mês (ex.: 29, 30, 31 em fevereiro) vira o
 * último dia do mês.
 *
 * <p>Fechamento antecipado por fim de semana ou feriado NÃO é calculado aqui (decisão do
 * Romulo: tratar na mão). Para isso a compra aceita {@code primeiraFatura} informada,
 * que sobrepõe o cálculo.
 */
public final class RegraFatura {

    private RegraFatura() {
    }

    /** Mês da fatura em que cai a 1ª parcela de uma compra feita em {@code dataCompra}. */
    public static YearMonth competenciaDaCompra(LocalDate dataCompra, int diaFechamento) {
        YearMonth mesDaCompra = YearMonth.from(dataCompra);
        LocalDate fechamentoNoMes = dataFechamento(mesDaCompra, diaFechamento);
        return dataCompra.isBefore(fechamentoNoMes) ? mesDaCompra : mesDaCompra.plusMonths(1);
    }

    /** Dia em que a fatura da competência fecha. */
    public static LocalDate dataFechamento(YearMonth competencia, int diaFechamento) {
        return competencia.atDay(Math.min(diaFechamento, competencia.lengthOfMonth()));
    }

    /** Vencimento da fatura da competência: no mesmo mês se o dia de vencimento vier depois
     * do fechamento; senão, no mês seguinte. */
    public static LocalDate dataVencimento(YearMonth competencia, int diaFechamento, int diaVencimento) {
        YearMonth mes = diaVencimento > diaFechamento ? competencia : competencia.plusMonths(1);
        return mes.atDay(Math.min(diaVencimento, mes.lengthOfMonth()));
    }

    /** Fatura que ainda está ABERTA hoje (recebe as compras de hoje). Para quem fecha dia 3,
     * em 18/11 é a de dezembro - a de novembro já fechou. */
    public static YearMonth faturaAberta(LocalDate hoje, int diaFechamento) {
        return competenciaDaCompra(hoje, diaFechamento);
    }

    /** Primeira fatura que ainda NÃO venceu: a anterior à aberta, se já fechou mas o
     * vencimento é hoje ou depois; senão, a própria aberta. O sistema não registra
     * pagamento, então fatura vencida conta como paga - é daqui em diante que o dinheiro
     * ainda vai sair (limite comprometido, "faltam N parcelas"). */
    public static YearMonth primeiraNaoVencida(LocalDate hoje, int diaFechamento, int diaVencimento) {
        YearMonth aberta = faturaAberta(hoje, diaFechamento);
        YearMonth anterior = aberta.minusMonths(1);
        return dataVencimento(anterior, diaFechamento, diaVencimento).isBefore(hoje) ? aberta : anterior;
    }

    /** Divide o total em parcelas de centavos inteiros; a sobra dos centavos vai para a 1ª
     * (é como os emissores costumam fazer, e a soma bate sempre com o total). */
    public static List<BigDecimal> dividir(BigDecimal total, int qtdParcelas) {
        if (qtdParcelas < 1) {
            throw new IllegalArgumentException("qtdParcelas deve ser >= 1");
        }
        BigDecimal centavos = total.setScale(2, RoundingMode.HALF_UP).movePointRight(2);
        BigDecimal[] divisao = centavos.divideAndRemainder(BigDecimal.valueOf(qtdParcelas));
        BigDecimal base = divisao[0].movePointLeft(2).setScale(2);
        BigDecimal sobra = divisao[1].movePointLeft(2).setScale(2);
        List<BigDecimal> valores = new ArrayList<>(qtdParcelas);
        for (int i = 0; i < qtdParcelas; i++) {
            valores.add(i == 0 ? base.add(sobra) : base);
        }
        return valores;
    }
}
