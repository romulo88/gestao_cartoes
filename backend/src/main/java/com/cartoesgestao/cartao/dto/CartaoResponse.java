package com.cartoesgestao.cartao.dto;

import com.cartoesgestao.cartao.Cartao;
import java.math.BigDecimal;

public record CartaoResponse(
        Integer id,
        String nome,
        String bandeira,
        String finalCartao,
        int diaFechamento,
        int diaVencimento,
        BigDecimal limite,
        String cor,
        boolean ativo) {

    public static CartaoResponse de(Cartao c) {
        return new CartaoResponse(c.getId(), c.getNome(), c.getBandeira(), c.getFinalCartao(),
                c.getDiaFechamento(), c.getDiaVencimento(), c.getLimite(), c.getCor(), c.getAtivo());
    }
}
