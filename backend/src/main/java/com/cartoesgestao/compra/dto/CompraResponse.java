package com.cartoesgestao.compra.dto;

import com.cartoesgestao.compra.Compra;
import com.cartoesgestao.compra.RegraFatura;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

/**
 * @param parcelasRestantes parcelas em faturas que ainda não venceram (o que ainda vai pesar).
 */
public record CompraResponse(
        Integer id,
        Ref cartao,
        Ref categoria,
        String descricao,
        BigDecimal valorTotal,
        int qtdParcelas,
        BigDecimal valorParcela,
        LocalDate dataCompra,
        YearMonth primeiraFatura,
        YearMonth ultimaFatura,
        int parcelasRestantes,
        String observacao,
        Integer recorrenciaId,
        List<ParcelaResponse> parcelas) {

    public static CompraResponse de(Compra c, LocalDate hoje) {
        YearMonth desde = RegraFatura.primeiraNaoVencida(
                hoje, c.getCartao().getDiaFechamento(), c.getCartao().getDiaVencimento());
        List<ParcelaResponse> parcelas = c.getParcelas().stream()
                .map(p -> new ParcelaResponse(p.getNumero(), p.getValor(), YearMonth.from(p.getCompetencia())))
                .toList();
        int restantes = (int) parcelas.stream().filter(p -> !p.competencia().isBefore(desde)).count();
        var cat = c.getCategoria();
        return new CompraResponse(
                c.getId(),
                new Ref(c.getCartao().getId(), c.getCartao().getNome(), c.getCartao().getCor()),
                cat == null ? null : new Ref(cat.getId(), cat.getNome(), cat.getCor()),
                c.getDescricao(),
                c.getValorTotal(),
                c.getQtdParcelas(),
                parcelas.isEmpty() ? c.getValorTotal() : parcelas.get(parcelas.size() - 1).valor(),
                c.getDataCompra(),
                YearMonth.from(c.getPrimeiraFatura()),
                parcelas.isEmpty() ? YearMonth.from(c.getPrimeiraFatura()) : parcelas.get(parcelas.size() - 1).competencia(),
                restantes,
                c.getObservacao(),
                c.getRecorrenciaId(),
                parcelas);
    }
}
