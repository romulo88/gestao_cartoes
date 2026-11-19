package com.cartoesgestao.fatura.dto;

import com.cartoesgestao.compra.dto.Ref;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

/**
 * @param situacao "aberta" (ainda não fechou), "fechada" (fechou, não venceu) ou "vencida"
 *     (passou do vencimento). O sistema não registra pagamento - é só o calendário.
 * @param total só o que já foi lançado (compras).
 * @param previstos cobranças recorrentes que ainda vão ser lançadas e cairiam aqui - fora
 *     do {@code total}.
 */
public record FaturaResponse(
        Ref cartao,
        YearMonth competencia,
        LocalDate dataFechamento,
        LocalDate dataVencimento,
        String situacao,
        BigDecimal total,
        List<ItemFaturaResponse> itens,
        BigDecimal totalPrevisto,
        List<ItemPrevistoResponse> previstos) {
}
