package com.cartoesgestao.categoria.dto;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;

public record CategoriaRequest(
        @NotBlank @Size(max = 60) String nome,
        @Pattern(regexp = "^#[0-9a-fA-F]{6}$", message = "deve ser uma cor no formato #RRGGBB") String cor,
        @DecimalMin("0.00") @Digits(integer = 10, fraction = 2) BigDecimal limiteMensal) {
}
