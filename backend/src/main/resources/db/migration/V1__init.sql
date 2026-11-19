-- Schema inicial. O Flyway é o dono do schema; o Hibernate só valida (ddl-auto: validate).
-- Todo dado de domínio pertence a um usuário (SaaS multiusuário): cada tabela carrega
-- usuario_id e toda consulta da API filtra por ele.

CREATE TABLE usuarios (
    id_usuario   SERIAL PRIMARY KEY,
    nome         VARCHAR(120) NOT NULL,
    email        VARCHAR(160) NOT NULL,
    senha_hash   VARCHAR(100) NOT NULL,
    ultimo_login TIMESTAMP,
    created_at   TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at   TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
-- E-mail é o login: único sem diferenciar maiúsculas.
CREATE UNIQUE INDEX ux_usuarios_email ON usuarios (LOWER(email));

CREATE TABLE cartoes (
    id_cartao      SERIAL PRIMARY KEY,
    usuario_id     INTEGER NOT NULL REFERENCES usuarios (id_usuario) ON DELETE CASCADE,
    nome           VARCHAR(60) NOT NULL,
    bandeira       VARCHAR(30),
    final_cartao   VARCHAR(4),
    dia_fechamento SMALLINT NOT NULL CHECK (dia_fechamento BETWEEN 1 AND 31),
    dia_vencimento SMALLINT NOT NULL CHECK (dia_vencimento BETWEEN 1 AND 31),
    limite         NUMERIC(12, 2) CHECK (limite IS NULL OR limite >= 0),
    cor            VARCHAR(7) NOT NULL DEFAULT '#2a78d6',
    ativo          BOOLEAN NOT NULL DEFAULT TRUE,
    created_at     TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at     TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX ix_cartoes_usuario ON cartoes (usuario_id);
CREATE UNIQUE INDEX ux_cartoes_usuario_nome ON cartoes (usuario_id, LOWER(nome));

CREATE TABLE categorias (
    id_categoria  SERIAL PRIMARY KEY,
    usuario_id    INTEGER NOT NULL REFERENCES usuarios (id_usuario) ON DELETE CASCADE,
    nome          VARCHAR(60) NOT NULL,
    cor           VARCHAR(7) NOT NULL DEFAULT '#64748b',
    -- Teto de gasto mensal (soma das parcelas que caem na fatura do mês). Nulo = sem limite.
    limite_mensal NUMERIC(12, 2) CHECK (limite_mensal IS NULL OR limite_mensal >= 0),
    created_at    TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at    TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX ix_categorias_usuario ON categorias (usuario_id);
CREATE UNIQUE INDEX ux_categorias_usuario_nome ON categorias (usuario_id, LOWER(nome));

CREATE TABLE compras (
    id_compra     SERIAL PRIMARY KEY,
    usuario_id    INTEGER NOT NULL REFERENCES usuarios (id_usuario) ON DELETE CASCADE,
    cartao_id     INTEGER NOT NULL REFERENCES cartoes (id_cartao),
    categoria_id  INTEGER REFERENCES categorias (id_categoria) ON DELETE SET NULL,
    descricao     VARCHAR(160) NOT NULL,
    valor_total   NUMERIC(12, 2) NOT NULL CHECK (valor_total > 0),
    qtd_parcelas  SMALLINT NOT NULL CHECK (qtd_parcelas BETWEEN 1 AND 72),
    data_compra   DATE NOT NULL,
    -- Fatura (mês) em que cai a 1ª parcela. Calculada pelo dia de fechamento do cartão,
    -- mas gravada para que um ajuste manual (fechamento antecipado por fim de semana ou
    -- feriado) seja respeitado.
    primeira_fatura DATE NOT NULL CHECK (EXTRACT(DAY FROM primeira_fatura) = 1),
    observacao    VARCHAR(500),
    created_at    TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at    TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX ix_compras_usuario_data ON compras (usuario_id, data_compra DESC);
CREATE INDEX ix_compras_cartao ON compras (cartao_id);

CREATE TABLE parcelas (
    id_parcela  SERIAL PRIMARY KEY,
    compra_id   INTEGER NOT NULL REFERENCES compras (id_compra) ON DELETE CASCADE,
    numero      SMALLINT NOT NULL CHECK (numero >= 1),
    valor       NUMERIC(12, 2) NOT NULL CHECK (valor > 0),
    -- Mês da fatura (sempre dia 1): é a chave de todos os totais por fatura.
    competencia DATE NOT NULL CHECK (EXTRACT(DAY FROM competencia) = 1),
    UNIQUE (compra_id, numero)
);
CREATE INDEX ix_parcelas_competencia ON parcelas (competencia);
