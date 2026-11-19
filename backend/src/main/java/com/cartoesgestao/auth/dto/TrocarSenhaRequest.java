package com.cartoesgestao.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record TrocarSenhaRequest(
        @NotBlank String senhaAtual,
        @NotBlank @Size(min = 8, max = 72, message = "deve ter entre 8 e 72 caracteres") String novaSenha) {
}
