package com.cartoesgestao.compra.dto;

/** Referência curta a cartão/categoria dentro de outras respostas (id, nome, cor). */
public record Ref(Integer id, String nome, String cor) {
}
