package com.cartoesgestao.security;

/** Principal das requisições autenticadas - controllers pegam com
 * {@code @AuthenticationPrincipal UsuarioAutenticado usuario}. */
public record UsuarioAutenticado(Integer id, String nome, String email) {
}
