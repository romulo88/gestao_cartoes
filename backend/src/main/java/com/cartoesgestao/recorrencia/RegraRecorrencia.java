package com.cartoesgestao.recorrencia;

import com.cartoesgestao.compra.RegraFatura;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;

/**
 * Datas das cobranças recorrentes, sem dependência de Spring/JPA (dá para testar isolado).
 *
 * <p>O dia da cobrança é "nominal": dia 31 num mês de 30 dias vira o último dia do mês, e o
 * mês seguinte volta para o 31 (mesma regra do fechamento em {@code RegraFatura}). Anual
 * repete o mesmo mês todo ano (29/02 vira 28/02 em ano não bissexto).
 */
public final class RegraRecorrencia {

    private RegraRecorrencia() {
    }

    /** Cobrança seguinte a {@code atual}. */
    public static LocalDate proxima(LocalDate atual, Periodicidade periodicidade, int diaCobranca) {
        YearMonth mes = YearMonth.from(atual);
        mes = periodicidade == Periodicidade.ANUAL ? mes.plusYears(1) : mes.plusMonths(1);
        return mes.atDay(Math.min(diaCobranca, mes.lengthOfMonth()));
    }

    /** Parcela de uma cobrança que ainda não virou compra, já na fatura em que vai cair. */
    public record ParcelaPrevista(YearMonth competencia, LocalDate dataCobranca, int numero, int qtdParcelas, BigDecimal valor) {
    }

    /**
     * Parcelas das cobranças futuras (a partir de {@code proxima}) que caem nas faturas de
     * {@code inicio} a {@code fim}, pela mesma regra de fatura das compras. É o que o painel e
     * a tela de faturas mostram como previsão.
     */
    public static List<ParcelaPrevista> previstas(
            LocalDate proxima, Periodicidade periodicidade, int diaCobranca, LocalDate dataFim,
            int diaFechamento, BigDecimal valor, int qtdParcelas, YearMonth inicio, YearMonth fim) {
        List<BigDecimal> valores = RegraFatura.dividir(valor, qtdParcelas);
        List<ParcelaPrevista> previstas = new ArrayList<>();
        // Cobrança depois do fim da janela cai em fatura depois dela: não interessa.
        for (LocalDate data : cobrancas(proxima, periodicidade, diaCobranca, dataFim, fim.atEndOfMonth(), 400)) {
            YearMonth primeira = RegraFatura.competenciaDaCompra(data, diaFechamento);
            for (int i = 0; i < valores.size(); i++) {
                YearMonth comp = primeira.plusMonths(i);
                if (!comp.isBefore(inicio) && !comp.isAfter(fim)) {
                    previstas.add(new ParcelaPrevista(comp, data, i + 1, qtdParcelas, valores.get(i)));
                }
            }
        }
        return previstas;
    }

    /** Cobranças de {@code desde} (inclusive) até {@code ate} (inclusive), respeitando a data
     * de fim. {@code limite} evita laço gigante se a data inicial estiver muito no passado. */
    public static List<LocalDate> cobrancas(
            LocalDate desde, Periodicidade periodicidade, int diaCobranca, LocalDate dataFim, LocalDate ate, int limite) {
        List<LocalDate> datas = new ArrayList<>();
        LocalDate d = desde;
        while (!d.isAfter(ate) && (dataFim == null || !d.isAfter(dataFim)) && datas.size() < limite) {
            datas.add(d);
            d = proxima(d, periodicidade, diaCobranca);
        }
        return datas;
    }
}
