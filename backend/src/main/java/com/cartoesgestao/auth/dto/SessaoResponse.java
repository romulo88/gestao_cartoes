package com.cartoesgestao.auth.dto;

public record SessaoResponse(String token, Integer usuarioId, String nome, String email) {
}
