package com.cartoesgestao.recorrencia.dto;

import com.cartoesgestao.recorrencia.Periodicidade;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Usado no POST e no PUT (o PUT substitui todos os campos).
 *
 * @param proximaCobranca data da próxima cobrança. Pode ser no passado: as cobranças entre
 *     ela e hoje são geradas na hora (útil para cadastrar uma assinatura antiga).
 * @param qtdParcelas opcional, padrão 1 (anuidade "em 12x": valor total + 12).
 * @param ativa opcional, padrão true. Pausada não gera cobranças.
 */
public record RecorrenciaRequest(
        @NotNull Integer cartaoId,
        Integer categoriaId,
        @NotBlank @Size(max = 160) String descricao,
        @NotNull @DecimalMin(value = "0.01") @Digits(integer = 10, fraction = 2) BigDecimal valor,
        @Min(1) @Max(72) Short qtdParcelas,
        @NotNull Periodicidade periodicidade,
        @NotNull LocalDate proximaCobranca,
        LocalDate dataFim,
        Boolean ativa,
        @Size(max = 500) String observacao) {
}
