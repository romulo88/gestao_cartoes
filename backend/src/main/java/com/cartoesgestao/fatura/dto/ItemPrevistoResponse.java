package com.cartoesgestao.fatura.dto;

import com.cartoesgestao.compra.dto.Ref;
import java.math.BigDecimal;
import java.time.LocalDate;

/** Parcela de uma cobrança recorrente que ainda não virou compra (só previsão). */
public record ItemPrevistoResponse(
        Integer recorrenciaId,
        String descricao,
        LocalDate dataCobranca,
        int numeroParcela,
        int qtdParcelas,
        BigDecimal valor,
        Ref categoria) {
}
