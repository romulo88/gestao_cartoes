package com.cartoesgestao.fatura.dto;

import com.cartoesgestao.compra.dto.Ref;
import java.math.BigDecimal;
import java.time.LocalDate;

public record ItemFaturaResponse(
        Integer compraId,
        String descricao,
        LocalDate dataCompra,
        int numeroParcela,
        int qtdParcelas,
        BigDecimal valor,
        Ref categoria) {
}
