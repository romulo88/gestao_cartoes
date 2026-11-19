# Handoff - Sistema de controle de cartões

> Aponte para este arquivo no início de uma conversa futura para retomar o projeto.

## O que é

SaaS da Romtech para controlar gastos de cartão de crédito: cada usuário cadastra os
próprios cartões (dia de fechamento/vencimento/limite), lança compras (à vista ou
parceladas) e vê em qual fatura cai cada parcela, o limite comprometido e o gasto por
categoria. Substitui a planilha `parcelas_por_mes` + dashboard Apps Script (decisão do
Romulo: recadastrar tudo no sistema novo, sem importar a planilha).

## Decisões

- **Mesma stack e padrões do Commander** (Next 16 / Spring Boot 3.3 / Postgres / Flyway
  `validate` / JWT / proxy `/api` no Next / deploy por GHCR + SSH).
- **Multiusuário desde o início**, autocadastro aberto, login por e-mail. Sem papéis.
- **Competência = mês em que a fatura fecha**; compra no dia do fechamento já vai para a
  próxima. Fechamento antecipado por fim de semana/feriado é ajuste manual por compra
  (campo `primeiraFatura`), como o Romulo já fazia na planilha.
- Cada compra **grava** suas parcelas (tabela `parcelas`), não recalcula na leitura.
- O sistema **não registra pagamento** de fatura: "Aberta / Fechada / Venceu" é só calendário.
- Sessão em `localStorage` (uso pessoal), token de 7 dias renovado a cada dia de uso.
- Produção em `cartoes.romtechsolucoes.com.br`, projeto Compose separado (`/opt/cartoes`).
- Portas locais 5433/8081/3001 para coexistir com o Commander.

## Estado (v1)

- Backend: auth, cartões, categorias (10 padrão na conta nova, com limite mensal opcional),
  compras com parcelas, previsão de fatura, faturas do mês, painel.
- Frontend: login, criar conta, painel (totais, limite por cartão, barras mês a mês com
  projeção, categorias com limite), lançamentos (filtros + paginação + editar/excluir),
  faturas por cartão, cartões, categorias, minha conta (trocar senha). PWA básico.
- Testes: `RegraFaturaTest` (regra de fatura e divisão de parcelas).

## Verificado (18/11)

- Backend compilou e subiu de primeira na máquina do Romulo; Flyway aplicou a V1 e o
  Hibernate validou as entidades.
- Rodando local: Postgres 5433, API 8081, frontend 3001 (3000/8080/5432 são do Commander).
- Banco acessível no DBeaver (localhost:5433, cartoes_gestao, cartoes/cartoes).

## v2 - sugestão de categoria (18/11, pedido do Romulo)

- Ao digitar a descrição, a categoria é sugerida: histórico do usuário → palavras-chave
  padrão → "Outros" (ver README, "Sugestão de categoria"). Sem tela de palavras-chave
  (decisão do Romulo). Escolha manual nunca é sobrescrita.
- `GET /api/categorias/sugestao?descricao=`; lógica em `RegraSugestao` + `RegraSugestaoTest`.
- ✅ Validado em 19/11: `mvn test` 12/12; endpoint no backend real conferido nos 3 caminhos
  (palavra-chave, "Outros", histórico - escolha manual passa a valer, sem diferenciar
  maiúsculas), descrição vazia → `categoria: null`, sem token → 401.
- Também: removido o "Using generated security password" do log (exclude de
  `UserDetailsServiceAutoConfiguration`) e `npm run dev` já sobe na 3001.

## Próximos passos sugeridos

- Esqueci minha senha (precisa de e-mail; dá para reaproveitar o SMTP do Commander).
- Marcar fatura como paga e conciliar com o valor do banco.
- Resumo por e-mail (o que a rotina noturna da planilha fazia).
- Importar CSV/OFX da fatura do banco.

## v3 - fatura atual (18/11, achado do Romulo)

- Painel e Faturas abriam no mês do calendário (nov) mesmo com as faturas de nov já
  fechadas (cartões que fecham dia 3 e 5). Agora abrem na **fatura em aberto** -
  `GET /api/faturas/atual` = a mais antiga ainda aberta entre os cartões ativos. Botão "Atual"
  no seletor de mês volta para ela.
- Limite comprometido, "faltam N parcelas" e o filtro "só com parcelas a vencer" contam a
  partir da **primeira fatura não vencida de cada cartão** (`RegraFatura.primeiraNaoVencida`;
  sem registro de pagamento, vencida = paga).
- Decidido (19/11, Romulo): a fatura continua nomeada pelo mês do **FECHAMENTO**, não do
  vencimento como nos apps dos bancos. Não reabrir.

## v4 - lançamentos recorrentes (19/11, pedido do Romulo)

- Assinaturas e anuidades: tela **Recorrentes**, `/api/recorrencias`, migration `V2__recorrencias.sql`
  (tabela `recorrencias` + `compras.recorrencia_id`). Regras no README, "Lançamentos recorrentes".
- Decisões (tomadas por mim, sem pergunta - revisar se o Romulo discordar): a recorrência gera
  compra de verdade **no dia da cobrança** (não antecipa); cobranças futuras só como previsão
  listrada no gráfico do painel, **fora** do total e do limite; editar não mexe nas já geradas;
  excluir mantém as compras; mensal ou anual, com parcelas por cobrança.
- Geração ao subir + cron 00:05 (`@EnableScheduling`) + na hora ao salvar.
- Verificado: `mvn test` 18/18; API real (retroativo, dia 31, anual 12x, previsão do painel,
  409 ao excluir cartão, validação de data de fim); tela no navegador (desktop e celular),
  frontend Docker reconstruído.
- ⚠️ Rodando `mvn spring-boot:run` pela sandbox do Claude o Tomcat não sobe ("Unable to
  establish loopback connection") - é a sandbox, não o código. Subir num terminal normal.
- Contas de teste no banco local: e-mails `teste.*@exemplo.test` (podem ser apagadas).
- Tela de **Faturas** também mostra os recorrentes previstos (linhas listradas, "+ R$ X
  previsto" ao lado do total, fora dele). Mesma conta do painel (`RegraRecorrencia.previstas`,
  com testes). `mvn test` 20/20; conferido na API e no navegador (desktop e celular).
- Celular: na tabela de faturas a coluna Parcela some e vira uma linha pequena embaixo da
  descrição ("parcela 3/10", "previsto · parcela 1/12"); antes a descrição cortava o nome.

## v5 - hint por categoria no painel (19/11, pedido do Romulo)

- Passar o mouse numa categoria do painel mostra os gastos dela naquela fatura (todos os
  cartões): data, descrição, parcela, valor, maiores primeiro; até 8 itens + "N outros" e o
  total. No celular, um toque abre e outro fecha (hover só reage a `pointerType === "mouse"`).
- Sem mudança no backend: o painel busca também `/api/faturas?mes=` e agrupa no frontend
  (`agruparPorCategoria` em `components/graficos.tsx`). Recorrentes previstos não entram no
  hint (a barra da categoria também é só do que já foi lançado).

## v6 - compartilhar painel por link (19/11, pedido do Romulo: "para a esposa ver")

- Minha conta → Compartilhar painel: cria links com nome, lista (criado / último acesso),
  revoga. Página pública `/ver#<token>`: painel inteiro (com hint e troca de mês), sem
  login, sem menu, "Somente leitura". Migration `V3__compartilhamentos.sql`. Ver README.
- Decisões (minhas, sem pergunta): token depois do `#` + header (fora de logs); banco guarda
  só SHA-256 → link mostrado uma vez só; sem data de expiração (revogar quando quiser);
  mostra só o painel (sem Lançamentos/Faturas/Recorrentes); até 20 links.
- Painel extraído para `components/painel-visao.tsx` (usado pelo painel e pelo `/ver`).
- Verificado: `mvn test` 23/23 (+ `TokenCompartilhamentoTest`); API (404 sem token/token
  errado/revogado, token do link não abre API privada, POST no público = 405, banco só com
  hash); navegador (criar link, abrir sem sessão, hint, troca de mês, celular, revogar →
  mensagem de link inválido, meta noindex/no-referrer).
- Produção: nada extra de infra (o proxy `/api` do Next repassa o header). Só fazer o deploy.

## v7 - criar categoria direto no lançamento (19/11, pedido do Romulo)

- No formulário de compra e no de recorrência, o select de categoria tem "+ Nova categoria…"
  no fim: abre um campo ali mesmo, cria (cor = próxima livre da paleta, sem limite mensal) e
  já deixa selecionada. Enter cria a categoria (não envia a compra); Esc cancela. Nome
  repetido mostra o erro do backend ("Você já tem uma categoria com esse nome").
- Componente `components/seletor-categoria.tsx`; as telas recarregam a lista
  (`useDadosBase().recarregar`) para os filtros e os outros formulários verem a nova.
- Só frontend. Conferido no navegador (lançamento e recorrência).
