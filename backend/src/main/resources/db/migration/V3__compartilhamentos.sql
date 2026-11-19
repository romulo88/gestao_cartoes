-- Links de visualização do painel (somente leitura, sem login) - ex.: para a esposa.
-- Guarda só o SHA-256 do token: quem lê o banco não consegue montar o link. O link
-- completo é mostrado uma única vez, na criação. Revogar = apagar a linha.

CREATE TABLE compartilhamentos (
    id_compartilhamento SERIAL PRIMARY KEY,
    usuario_id          INTEGER NOT NULL REFERENCES usuarios (id_usuario) ON DELETE CASCADE,
    -- Para o dono saber de quem é o link ("Esposa").
    nome                VARCHAR(60) NOT NULL,
    token_hash          VARCHAR(64) NOT NULL,
    ultimo_acesso       TIMESTAMP,
    created_at          TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at          TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE UNIQUE INDEX ux_compartilhamentos_token ON compartilhamentos (token_hash);
CREATE INDEX ix_compartilhamentos_usuario ON compartilhamentos (usuario_id);
