package com.cartoesgestao.cartao.dto;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;

/** Usado no POST e no PUT (o PUT substitui todos os campos). */
public record CartaoRequest(
        @NotBlank @Size(max = 60) String nome,
        @Size(max = 30) String bandeira,
        @Pattern(regexp = "^\\d{4}$", message = "deve ter 4 dígitos") String finalCartao,
        @NotNull @Min(1) @Max(31) Short diaFechamento,
        @NotNull @Min(1) @Max(31) Short diaVencimento,
        @DecimalMin("0.00") @Digits(integer = 10, fraction = 2) BigDecimal limite,
        @Pattern(regexp = "^#[0-9a-fA-F]{6}$", message = "deve ser uma cor no formato #RRGGBB") String cor,
        Boolean ativo) {
}
