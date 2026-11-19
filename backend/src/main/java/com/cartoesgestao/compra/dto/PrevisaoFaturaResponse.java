package com.cartoesgestao.compra.dto;

import java.time.LocalDate;
import java.time.YearMonth;

/** Resposta do "em qual fatura cai?" - a tela de lançamento mostra antes de salvar. */
public record PrevisaoFaturaResponse(YearMonth competencia, LocalDate dataFechamento, LocalDate dataVencimento) {
}
