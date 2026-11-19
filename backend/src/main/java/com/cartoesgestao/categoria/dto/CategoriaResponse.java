package com.cartoesgestao.categoria.dto;

import com.cartoesgestao.categoria.Categoria;
import java.math.BigDecimal;

public record CategoriaResponse(Integer id, String nome, String cor, BigDecimal limiteMensal) {

    public static CategoriaResponse de(Categoria c) {
        return new CategoriaResponse(c.getId(), c.getNome(), c.getCor(), c.getLimiteMensal());
    }
}
