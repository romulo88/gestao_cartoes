package com.cartoesgestao.fatura.dto;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.List;

/** @param totalPrevisto soma dos recorrentes previstos (fora do {@code total}). */
public record FaturasDoMesResponse(
        YearMonth competencia, BigDecimal total, BigDecimal totalPrevisto, List<FaturaResponse> faturas) {
}
