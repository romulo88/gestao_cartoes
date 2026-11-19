package com.cartoesgestao.dashboard.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Linha de agregação das consultas de {@code ParcelaRepository}. */
public record TotalAgrupado(Integer chave, LocalDate competencia, BigDecimal total) {
}
