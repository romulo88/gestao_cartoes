package com.cartoesgestao.compra.dto;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;

/**
 * @param primeiraFatura opcional ("2026-12"). Se vier nulo, é calculado pelo dia de
 *     fechamento do cartão; informe só para corrigir fechamento antecipado (fim de semana,
 *     feriado) ou compra lançada em fatura diferente pelo banco.
 */
public record CompraRequest(
        @NotNull Integer cartaoId,
        Integer categoriaId,
        @NotBlank @Size(max = 160) String descricao,
        @NotNull @DecimalMin(value = "0.01") @Digits(integer = 10, fraction = 2) BigDecimal valorTotal,
        @NotNull @Min(1) @Max(72) Short qtdParcelas,
        @NotNull LocalDate dataCompra,
        YearMonth primeiraFatura,
        @Size(max = 500) String observacao) {
}
