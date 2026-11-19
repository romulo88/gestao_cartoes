package com.cartoesgestao.compra.dto;

import java.math.BigDecimal;
import java.time.YearMonth;

public record ParcelaResponse(int numero, BigDecimal valor, YearMonth competencia) {
}
