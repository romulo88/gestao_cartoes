package com.cartoesgestao.compartilhamento.dto;

import com.cartoesgestao.compartilhamento.Compartilhamento;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;
import java.time.YearMonth;

public final class CompartilhamentoDtos {

    private CompartilhamentoDtos() {
    }

    public record CriarRequest(@NotBlank @Size(max = 60) String nome) {
    }

    public record CompartilhamentoResponse(Integer id, String nome, LocalDateTime criadoEm, LocalDateTime ultimoAcesso) {

        public static CompartilhamentoResponse de(Compartilhamento c) {
            return new CompartilhamentoResponse(c.getId(), c.getNome(), c.getCreatedAt(), c.getUltimoAcesso());
        }
    }

    /** Só na criação: o token não é gravado, então é a única vez que ele existe fora do link. */
    public record CriadoResponse(CompartilhamentoResponse compartilhamento, String token) {
    }

    /** O que a página pública mostra no topo. {@code dono} = primeiro nome. */
    public record InfoPublicaResponse(String dono, YearMonth mesAtual) {
    }
}
