-- Lançamentos recorrentes (assinaturas, anuidades). Cada recorrência GERA uma compra comum
-- quando chega a data da cobrança (ver RecorrenciaService.gerarPendentes) - a partir daí a
-- compra é como qualquer outra: entra nas faturas, no painel e pode ser editada à parte.

CREATE TABLE recorrencias (
    id_recorrencia   SERIAL PRIMARY KEY,
    usuario_id       INTEGER NOT NULL REFERENCES usuarios (id_usuario) ON DELETE CASCADE,
    cartao_id        INTEGER NOT NULL REFERENCES cartoes (id_cartao),
    categoria_id     INTEGER REFERENCES categorias (id_categoria) ON DELETE SET NULL,
    descricao        VARCHAR(160) NOT NULL,
    -- Valor de CADA cobrança (anuidade de R$ 480 em 12x: valor 480, qtd_parcelas 12).
    valor            NUMERIC(12, 2) NOT NULL CHECK (valor > 0),
    qtd_parcelas     SMALLINT NOT NULL DEFAULT 1 CHECK (qtd_parcelas BETWEEN 1 AND 72),
    periodicidade    VARCHAR(10) NOT NULL CHECK (periodicidade IN ('MENSAL', 'ANUAL')),
    -- Dia "nominal" da cobrança. Guardado à parte porque proxima_cobranca é limitada ao fim
    -- do mês (dia 31 em fevereiro vira 28) e o mês seguinte precisa voltar ao 31.
    dia_cobranca     SMALLINT NOT NULL CHECK (dia_cobranca BETWEEN 1 AND 31),
    -- Data da próxima compra a gerar. Avança a cada geração.
    proxima_cobranca DATE NOT NULL,
    -- Última cobrança possível (inclusive). Nulo = sem fim.
    data_fim         DATE,
    ativa            BOOLEAN NOT NULL DEFAULT TRUE,
    observacao       VARCHAR(500),
    created_at       TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at       TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX ix_recorrencias_usuario ON recorrencias (usuario_id);
CREATE INDEX ix_recorrencias_pendentes ON recorrencias (proxima_cobranca) WHERE ativa;

-- Compra gerada por uma recorrência. Excluir a recorrência mantém as compras (histórico).
ALTER TABLE compras ADD COLUMN recorrencia_id INTEGER REFERENCES recorrencias (id_recorrencia) ON DELETE SET NULL;
-- Rede de segurança contra gerar a mesma cobrança duas vezes.
CREATE UNIQUE INDEX ux_compras_recorrencia_data ON compras (recorrencia_id, data_compra)
    WHERE recorrencia_id IS NOT NULL;
