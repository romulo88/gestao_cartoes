# cartoes-gestao

Controle de gastos e parcelas de cartões de crédito - SaaS multiusuário da Romtech
(nome provisório **Romtech Cartões**, troca em `frontend/src/lib/marca.ts`).
Ver [`HANDOFF.md`](HANDOFF.md) para o estado do projeto e [`DEPLOY.md`](DEPLOY.md) para produção.

## Stack

Mesma do Commander: Next.js 16 (React 19, Tailwind 4) · Spring Boot 3.3 (Java 21) +
Spring Data JPA + Flyway + JWT · PostgreSQL 17 · Docker + Traefik · GitHub Actions → GHCR.

```
cartoes-gestao/
├── docker-compose.yml        # Postgres (5433) + frontend containerizado (3001), dev local
├── backend/                  # API Spring Boot (porta 8081) - repositório gestao-cartoes
├── frontend/                 # Next.js - repositório próprio gestao-cartoes-web
└── deploy/vps/               # compose + .env de PRODUÇÃO (vão para /opt/cartoes na VPS)
```

Portas escolhidas para não brigar com o Commander rodando na mesma máquina (5432/8080/3000):
Postgres 5433, API 8081, frontend 3001.

## Rodando localmente

```bash
docker compose up -d postgres     # Postgres em localhost:5433
cd backend
mvn spring-boot:run               # Flyway cria o schema; API em localhost:8081
```

Frontend no Docker (sem hot reload; rebuild a cada mudança de código):

```bash
docker compose up -d --build frontend   # localhost:3001 - /api/* vai para o backend na máquina (8081)
```

Frontend com hot reload (pare o container antes - os dois usam a 3001):

```bash
cd frontend
npm install
npm run dev                       # localhost:3001 (a 3000 é do Commander) - /api/* vai para localhost:8081
```

- Swagger: http://localhost:8081/swagger-ui.html (faça `POST /api/auth/login` e cole o token em **Authorize**)
- Primeira conta: tela **Criar conta** (autocadastro aberto). Ela já nasce com 10 categorias padrão.

Testes das regras (fatura, sugestão, recorrência): `cd backend && mvn test`.

## Regra de fatura

Fica em `backend/.../compra/RegraFatura.java`, sem dependência de framework:

- **Competência** de uma fatura = mês em que ela **fecha**.
- Compra **antes** do dia de fechamento → fatura que fecha no mesmo mês; **no dia ou depois** → mês seguinte.
- Dia de fechamento/vencimento além do fim do mês (29, 30, 31) vira o último dia do mês.
- Vencimento: mesmo mês se `diaVencimento > diaFechamento`, senão mês seguinte.
- Valor das parcelas em centavos inteiros; a sobra vai para a 1ª (a soma bate sempre).
- **Fechamento antecipado** (fim de semana/feriado) não é calculado: no lançamento dá para
  trocar o mês da 1ª parcela na mão (`primeiraFatura`). Cada compra guarda a fatura em que
  caiu, então mudar o dia de fechamento do cartão não mexe no histórico.

## Sugestão de categoria

`backend/.../categoria/RegraSugestao.java` (sem dependência de framework, com testes). Ao
digitar a descrição, a tela de lançamento preenche a categoria, nesta ordem:

1. **Histórico do usuário** - compras com a mesma descrição normalizada (sem acento,
   maiúsculas, números longos e prefixos como `MP *`, `PAG*`); senão, com a mesma primeira
   palavra. Vence a categoria mais usada; empate, a mais recente. Corrigiu uma vez, aprende.
2. **Palavras-chave padrão** (lista no próprio arquivo), casadas pelo NOME da categoria do
   usuário. A ordem da lista é a prioridade ("amazon prime" antes de "amazon", "mercado livre"
   antes de "mercado", "uber eats" antes de "uber").
3. **Outros**, se existir; senão fica sem categoria.

Escolheu a categoria na mão, a sugestão para de mexer. Sem tela de edição das palavras-chave
(decisão do Romulo) - para mudar, edite a lista no `RegraSugestao`.

## Lançamentos recorrentes

Assinaturas e anuidades (`backend/.../recorrencia/`, tela **Recorrentes**):

- A recorrência **gera uma compra comum** quando chega a data da cobrança. Daí em diante a
  compra é independente: entra em faturas/painel/limite e pode ser editada (valor diferente
  num mês, por exemplo). Compra gerada tem `recorrencia_id` e aparece com "↻ recorrente".
- Geração: ao subir a aplicação e todo dia às 00:05 (`GeradorRecorrencias`), e na hora ao
  criar/editar. Próxima cobrança no passado → gera todas até hoje (cadastro retroativo).
  Linha travada na geração + índice único `(recorrencia_id, data_compra)`: não duplica.
- Mensal ou anual; parcelas por cobrança (anuidade "R$ 480 em 12x" = valor 480, 12 parcelas).
  Dia 31 vira o último dia do mês e volta a 31 no mês seguinte (`RegraRecorrencia`, com testes).
- Cobranças futuras aparecem como **previsão**: barra listrada no gráfico do painel
  (`previstoRecorrente`) e linhas listradas "previsto" na tela de Faturas (`previstos` /
  `totalPrevisto` de cada fatura). Não entram no total da fatura nem no limite comprometido.
  As duas telas usam a mesma conta (`RegraRecorrencia.previstas`).
- Editar muda só as próximas cobranças. Pausar/data de fim param a geração. Excluir mantém
  as compras já geradas. Cartão com recorrência não pode ser excluído.

## Compartilhar o painel (link somente leitura)

Em **Minha conta → Compartilhar painel** o usuário cria links com nome ("Esposa") para
alguém ver o painel **sem login e sem alterar nada** (`backend/.../compartilhamento/`,
página `frontend/src/app/ver`).

- Link: `https://<site>/ver#<token>`. O token (256 bits aleatórios, base64url) vai **depois
  do `#`**: o navegador não manda o fragmento ao servidor, então ele não cai em log de
  acesso. A página lê o fragmento e manda o token no header `X-Painel-Token`.
- O banco guarda só o **SHA-256** do token. Por isso o link aparece uma única vez, na
  criação; perdeu = revoga e cria outro. Revogar apaga a linha e corta o acesso na hora.
- `/api/publico/painel/{info,dashboard,faturas}`: só GET, só o que o painel mostra (as
  faturas servem ao hint das categorias). Token inválido/revogado = 404. O token do link
  não serve como JWT na API privada. Máximo de 20 links por usuário.
- Página `/ver` com `noindex` e `no-referrer`; sem menu nem links para telas logadas.

## API

| Método | Caminho | |
|---|---|---|
| POST | `/api/auth/cadastro`, `/api/auth/login` | públicos; devolvem o token |
| POST | `/api/auth/renovar`, `/api/auth/trocar-senha` | |
| GET/POST/PUT/DELETE | `/api/cartoes`, `/api/categorias` | CRUD do usuário logado |
| GET | `/api/categorias/sugestao?descricao=` | categoria sugerida (ver abaixo) |
| GET | `/api/compras?cartaoId&categoriaId&busca&emAberto&pagina` | 30 por página |
| GET | `/api/compras/previsao-fatura?cartaoId&dataCompra` | "em qual fatura cai?" |
| POST/PUT/DELETE | `/api/compras` | gera/regera as parcelas |
| GET/POST/PUT/DELETE | `/api/recorrencias` | assinaturas/anuidades; POST/PUT já geram as cobranças vencidas |
| GET/POST/DELETE | `/api/compartilhamentos` | links do painel; o POST devolve o token uma única vez |
| GET | `/api/publico/painel/{info,dashboard,faturas}` | **sem login**, header `X-Painel-Token` |
| GET | `/api/faturas?mes=2026-12` | uma fatura por cartão, com os itens |
| GET | `/api/dashboard?mes=2026-12` | totais, limites, categorias, série de 13 meses |

Toda tabela de domínio tem `usuario_id` e toda consulta filtra pelo usuário do token.
