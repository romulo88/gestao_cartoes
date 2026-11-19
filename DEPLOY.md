# Deploy

Mesma VPS e mesmo Traefik do Commander, em **projeto Compose separado** (`/opt/cartoes`):
banco, volumes e deploys independentes.

```
navegador ──HTTPS──> Traefik ──> cartoes.romtechsolucoes.com.br ──> cartoes-web (Next, 3000)
                                                                       │ /api/* (rede interna)
                                                                       v
                                                         cartoes-backend (Spring, 8081)
                                                                       │
                                                                  cartoes-db (Postgres)
```

- Só o `cartoes-web` entra na rede do Traefik. Backend e banco ficam na rede interna do projeto.
- Serviços com prefixo `cartoes-` **de propósito**: na rede compartilhada do Traefik, um serviço
  chamado `backend` poderia ser resolvido pelo DNS do Docker no lugar do backend do Commander
  (e vice-versa).
- Sem Swagger público. Para usar em produção: túnel SSH até o container `cartoes-backend`.

## Primeira vez (uma vez só)

1. **DNS** (Hostinger): registro A `cartoes` → IP da VPS.
2. **GitHub**: criar os repositórios `romulo88/gestao-cartoes` (raiz do projeto, `frontend/`
   ignorado) e `romulo88/gestao-cartoes-web` (pasta `frontend/` com git próprio) - mesmo
   esquema do Commander. Em cada um, os secrets `VPS_HOST`, `VPS_USER` (`deploy`) e
   `VPS_SSH_KEY` (pode ser a mesma chave de deploy do Commander).
3. **VPS**:
   ```bash
   sudo mkdir -p /opt/cartoes && sudo chown deploy:deploy /opt/cartoes
   # copiar deploy/vps/docker-compose.yml e deploy/vps/.env.example (como .env) para /opt/cartoes
   cd /opt/cartoes
   openssl rand -base64 48   # usar em DB_PASSWORD
   openssl rand -base64 48   # usar em JWT_SECRET
   nano .env && chmod 600 .env
   docker network ls         # achar a rede do Traefik -> TRAEFIK_NETWORK no .env
   ```
4. **Conferir os labels do Traefik** em `/opt/cartoes/docker-compose.yml` contra os do
   `/opt/condominio/docker-compose.yml`: nome do entrypoint HTTPS (`websecure`) e do
   certresolver (`letsencrypt`) têm que ser **os mesmos** que o Commander usa.
5. Push na `main` dos dois repositórios: primeiro `gestao-cartoes` (backend), depois
   `gestao-cartoes-web`. O smoke test do backend só fica verde depois que o frontend estiver
   no ar (ele testa a API pelo proxy do frontend) - na primeira vez, rode o workflow do
   backend de novo pelo botão *Run workflow* depois do frontend publicado.

## Como o deploy acontece

Push na `main` → GitHub Actions → build da imagem → GHCR → SSH na VPS → `docker compose pull`
+ `up -d` do serviço → espera o healthcheck → teste pelo domínio público.

| Repositório | Serviço | Imagem |
|---|---|---|
| `gestao-cartoes` | `cartoes-backend` | `ghcr.io/romulo88/gestao-cartoes-backend` |
| `gestao-cartoes-web` | `cartoes-web` | `ghcr.io/romulo88/gestao-cartoes-web` |

Os workflows usam o mesmo `flock /tmp/condominio-deploy.lock` do Commander: os quatro
deploys da VPS entram numa fila única e nunca rodam `compose up` ao mesmo tempo.

## Pegadinha do BACKEND_URL (a mesma do Commander)

O destino do proxy `/api/*` é gravado no **build** do Next. Em produção entra como build-arg
`BACKEND_URL=http://cartoes-backend:8081` (workflow do frontend). Mudar isso exige rebuild
da imagem, não só recriar o container.

## Runbook

```bash
ssh deploy@<ip-da-vps>
cd /opt/cartoes
docker compose ps
docker compose logs -f cartoes-backend
docker compose exec -T cartoes-db pg_dump -U cartoes cartoes_gestao | gzip > backup-$(date +%F).sql.gz
```

Voltar versão: trocar `:latest` pela tag do SHA no compose e `docker compose up -d <serviço>`.
