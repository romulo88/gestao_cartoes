package com.cartoesgestao.categoria.dto;

/**
 * @param categoria nula quando não há sugestão (nada casou e o usuário não tem "Outros").
 * @param origem "historico", "palavra_chave" ou "padrao" - a tela mostra de onde veio.
 */
public record SugestaoCategoriaResponse(CategoriaResponse categoria, String origem) {
}
