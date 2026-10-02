# EventHub

Plataforma full stack para publicar eventos gratuitos, controlar inscrições e validar ingressos com QR Code. O projeto foi construído como um monólito modular, com foco em segurança, concorrência e uma experiência mobile adequada para a operação na entrada do evento.

**Online:** [abrir EventHub](https://eventhub-alpha-sandy.vercel.app) · [API e Swagger](https://eventhub-api-3c8f.onrender.com/swagger-ui.html) · [health check](https://eventhub-api-3c8f.onrender.com/actuator/health)

A demonstração pública utiliza Vercel Hobby, Render Free, Neon Free e Cloudinary Free. O envio de e-mails permanece em sandbox até existir um domínio de remetente verificado. Crie sua própria conta para experimentar; as credenciais de demonstração abaixo existem apenas no ambiente local com seed habilitado.

![Catálogo público do EventHub](docs/screenshots/home.png)

<details>
<summary>Ver catálogo mobile</summary>

<img src="docs/screenshots/mobile.png" alt="Catálogo do EventHub no celular" width="320" />
</details>

## O que já está implementado

- Cadastro e login com perfis `USER`, `ORGANIZER` e `ADMIN`.
- Access token JWT RS256 curto e refresh token opaco, rotativo e persistido apenas como hash.
- BFF no Next.js com cookies `HttpOnly`, `Secure` em produção e `SameSite=Lax`.
- Criação, edição, publicação e cancelamento de eventos gratuitos.
- Catálogo público com busca, filtro por cidade/data e paginação.
- Upload direto e assinado de capas para o Cloudinary, com validação de 5 MB no navegador.
- Até 10 tipos de ingresso gratuito por evento, reserva em grupo de até quatro nomes por conta e tipo, com cancelamento individual.
- Lista de espera FIFO por pedido, promoção automática do grupo inteiro e reserva atômica de capacidade no PostgreSQL.
- Um código público e QR Code assinado por ingresso, versionado e sem dados pessoais no token.
- Scanner mobile e check-in único protegido por restrição no banco.
- Dashboard do organizador com métricas, inscritos e gráfico de ocupação.
- Outbox transacional para entrega de e-mails pelo Resend, com tentativas e modo sandbox.
- Problem Details com códigos estáveis, Swagger/OpenAPI, Flyway, Docker Compose e CI.

## Arquitetura

```mermaid
flowchart LR
    B[Browser] --> N[Next.js 16\nApp Router + BFF]
    N -->|REST / cookies seguros| S[Spring Boot 4\nJava 17]
    S --> P[(PostgreSQL 17)]
    S --> C[Cloudinary]
    S --> R[Resend]
```

O back-end é organizado por domínio em `auth`, `users`, `events`, `registrations`, `tickets`, `checkin`, `media`, `notifications` e `reporting`. O Spring Modulith verifica os limites entre esses módulos. O PostgreSQL é a única fonte de verdade; Redis e RabbitMQ foram intencionalmente deixados para uma evolução posterior.

No front-end, páginas públicas usam Server Components. Formulários, gráficos e scanner usam Client Components com TanStack Query, React Hook Form e Zod. Requisições autenticadas passam pelo BFF; nenhum token é exposto ao `localStorage`.

## Modelo de dados

```mermaid
erDiagram
    USERS ||--o{ USER_ROLES : possui
    USERS ||--o{ REFRESH_TOKENS : autentica
    USERS ||--o{ EVENTS : organiza
    EVENTS ||--|{ TICKET_TYPES : oferece
    USERS ||--o{ REGISTRATIONS : realiza
    EVENTS ||--o{ REGISTRATIONS : recebe
    TICKET_TYPES ||--o{ REGISTRATIONS : reserva
    REGISTRATIONS ||--|{ TICKETS : emite
    USERS ||--o{ WAITLIST_REQUESTS : solicita
    TICKET_TYPES ||--o{ WAITLIST_REQUESTS : enfileira
    WAITLIST_REQUESTS ||--|{ WAITLIST_ATTENDEES : inclui
    TICKETS ||--o| CHECK_INS : valida
    EVENTS ||--o{ CHECK_INS : registra
    USERS ||--o{ CHECK_INS : escaneia
```

As alterações de schema vivem exclusivamente em `backend/src/main/resources/db/migration`. O Hibernate usa `ddl-auto=validate` e nunca cria tabelas em produção.

## Concorrência: a última vaga

A reserva não faz a sequência insegura “consultar e depois incrementar”. A API executa uma única atualização condicional dentro da transação:

```sql
UPDATE ticket_types
SET confirmed_count = confirmed_count + :quantity
WHERE id = :id AND confirmed_count + :quantity <= capacity;
```

O back-end bloqueia a linha do tipo de ingresso antes de verificar o limite de quatro por conta e a fila. Somente quem obtém uma linha alterada continua. Inscrição, todos os ingressos do grupo e mensagens da outbox são inseridos na mesma transação; qualquer falha reverte também o contador. O grupo é confirmado por inteiro ou não é confirmado.

A fila é ordenada pelo identificador sequencial do pedido. Um grupo menor não ultrapassa outro à frente que ainda não caiba. Cancelamento individual, saída da fila e aumento de capacidade tentam promover pedidos nessa ordem sob o mesmo bloqueio. O contador representa ingressos ativos, não inscrições.

O check-in segue o mesmo princípio: `INSERT ... ON CONFLICT DO NOTHING` sobre uma restrição única em `ticket_id`. Entre duas leituras simultâneas do mesmo QR, exatamente uma recebe `201 CHECKED_IN`; a outra recebe `409 ALREADY_CHECKED_IN` com o horário da primeira entrada.

## Stack

| Camada | Tecnologias |
| --- | --- |
| Web | Next.js 16, React 19, TypeScript, Tailwind CSS 4 |
| Estado e formulários | TanStack Query, React Hook Form, Zod |
| API | Spring Boot 4, Java 17, Spring Security, Spring Data JPA |
| Dados | PostgreSQL 17, Flyway, Hibernate |
| Integrações | Cloudinary, Resend, QR Code assinado |
| Qualidade | JUnit, Testcontainers, Vitest, Testing Library, Playwright, JaCoCo |
| Operação | Docker Compose, OpenAPI/Swagger, GitHub Actions |

## Executar com Docker

Pré-requisito: Docker Desktop com Docker Compose.

```bash
cp .env.example .env
docker compose up --build
```

Depois do health check do PostgreSQL:

- Aplicação: `http://localhost:3000`
- Swagger UI: `http://localhost:8080/swagger-ui.html`
- OpenAPI JSON: `http://localhost:8080/v3/api-docs`
- Health check: `http://localhost:8080/actuator/health`

O seed local cria um evento publicado e duas contas:

| Perfil | E-mail | Senha |
| --- | --- | --- |
| Organizador | `organizador@eventhub.dev` | `Demo@123` |
| Participante | `participante@eventhub.dev` | `Demo@123` |

Desative o seed em ambientes públicos com `DEMO_SEED=false`. Fora do Docker Compose, o seed já vem desativado.

## Executar para desenvolvimento

Com PostgreSQL disponível em `localhost:5432`, defina `JWT_ALLOW_EPHEMERAL=true` e um `QR_SIGNING_SECRET` de pelo menos 32 caracteres apenas no ambiente local:

```bash
cd backend
./mvnw spring-boot:run
```

Em outro terminal:

```bash
cd frontend
pnpm install
pnpm dev
```

Copie `frontend/.env.example` para `frontend/.env.local` quando precisar alterar a URL interna da API.

## Variáveis de ambiente

Use `.env.example` como referência. Em produção, configure obrigatoriamente:

- `DB_URL`, `DB_USERNAME` e `DB_PASSWORD`.
- `JWT_ISSUER`, `JWT_PRIVATE_KEY` e `JWT_PUBLIC_KEY` — chaves RSA em PEM.
- `QR_SIGNING_SECRET` — segredo aleatório com pelo menos 32 caracteres.
- `FRONTEND_URL` — origem pública do Next.js.
- `CLOUDINARY_CLOUD_NAME`, `CLOUDINARY_API_KEY` e `CLOUDINARY_API_SECRET`.
- `RESEND_API_KEY` e `RESEND_FROM_EMAIL` quando houver domínio verificado.

No Docker Compose local, `JWT_ALLOW_EPHEMERAL=true` gera chaves temporárias. Em produção, deixe essa opção desativada e configure o par RSA; também use `DEMO_SEED=false`.

Sem credenciais do Resend, a outbox continua funcional e o adaptador registra a entrega em modo sandbox. O ingresso permanece disponível em **Meus ingressos**.

## Testes e verificações

```bash
# Back-end, incluindo PostgreSQL real via Testcontainers
cd backend
./mvnw verify

# Front-end
cd frontend
pnpm lint
pnpm typecheck
pnpm test
pnpm build
pnpm exec playwright install chromium
pnpm test:e2e
```

Os testes do back-end cobrem a assinatura do QR, os limites do monólito modular, a migração de dados V1, a disputa pela última vaga, o limite de quatro sob concorrência, fila FIFO com grupo bloqueante, promoção por cancelamento e aumento de capacidade, tipos independentes e dois check-ins simultâneos. Os testes de integração são ignorados automaticamente somente quando não existe um daemon Docker disponível.

Com a API rodando, os contratos TypeScript podem ser atualizados diretamente do OpenAPI:

```bash
cd frontend
pnpm generate:api
```

O pipeline em `.github/workflows/ci.yml` executa lint, typecheck, testes e builds. Depois, sobe a aplicação completa com Docker Compose, verifica a geração de tipos a partir do OpenAPI e testa criação, publicação, inscrição, ingresso, check-in repetido e dashboard.

## Contrato de erros

Erros de negócio seguem `application/problem+json` e incluem um `code` estável. Exemplos:

- `EVENT_SOLD_OUT`
- `ALREADY_REGISTERED`
- `INVALID_TICKET`
- `ALREADY_CHECKED_IN`
- `FORBIDDEN`

Isso permite que o front-end traduza mensagens sem depender do texto retornado pela API.

## Publicar sem custo e sem cartão

Esta instância está publicada em **Vercel Hobby + Render Free + Neon Free**. Não use o PostgreSQL gratuito da Render: ele expira após 30 dias. Para reproduzir a publicação em contas próprias:

1. Crie um projeto **PostgreSQL 17 no Neon**, região AWS `us-east-1` (Virgínia). Guarde host, database, usuário e senha no painel do provedor.
2. Importe este repositório como **Blueprint na Render** usando `render.yaml`. Ele seleciona apenas a instância `free`, na Virgínia, com Docker em `backend/`, health check `/actuator/health`, heap Java máximo de 256 MB e pool de duas conexões. Na criação, preencha `DB_URL` no formato `jdbc:postgresql://HOST/DATABASE?sslmode=require`, `DB_USERNAME`, `DB_PASSWORD`, `JWT_PRIVATE_KEY` e `JWT_PUBLIC_KEY`. Gere o par RS256 com `scripts/generate-jwt-keys.ps1` em um terminal privado e **nunca** o inclua no Git, no README ou em capturas de tela. O segredo do QR é gerado pela Render e precisa continuar estável entre redeploys.
3. Após a API estar saudável, importe o mesmo repositório na **Vercel** com `frontend` como Root Directory e configure `API_URL=https://SUA-API.onrender.com/api/v1` na Production Environment. Mantenha `COOKIE_SECURE` ausente, para que o modo de produção o ative. Não configure `NEXT_PUBLIC_API_URL` com segredos ou com um endereço interno.
4. Configure `FRONTEND_URL` na Render com a URL `https://...vercel.app` real e faça um redeploy. Para upload de capas, crie uma conta Cloudinary Free e configure `CLOUDINARY_CLOUD_NAME`, `CLOUDINARY_API_KEY` e `CLOUDINARY_API_SECRET` **somente na Render**. O browser recebe apenas uma assinatura curta para enviar diretamente a imagem.
5. Mantenha `RESEND_API_KEY` ausente: o worker confirma a outbox em modo sandbox, sem enviar e-mail. O ingresso continua disponível em **Meus ingressos**. Ative o Resend apenas depois de verificar um domínio remetente.

Depois de cada push em `main`, confira CI e os deployments da Render e da Vercel. Para redeploy manual, abra o serviço `eventhub-api` na Render e use **Manual Deploy**; na Vercel, abra o projeto `eventhub`, selecione o último deployment e use **Redeploy**. Verifique o [health check](https://eventhub-api-3c8f.onrender.com/actuator/health), o [Swagger](https://eventhub-api-3c8f.onrender.com/swagger-ui.html) e a [página pública](https://eventhub-alpha-sandy.vercel.app). A instância Render Free adormece após 15 minutos; o primeiro acesso pode levar vários minutos (205 segundos na verificação de 01/10/2026). O catálogo espera até 20 segundos por tentativa e, se a API não responder, exibe uma mensagem de serviço iniciando com nova tentativa automática a cada 25 segundos, sem confundir falha com busca vazia. Evite serviços artificiais de ping para contornar os limites gratuitos.

Teste no endereço público: cadastro de organizador, publicação de evento com capa, cadastro de participante, inscrição, QR, primeiro check-in, rejeição do segundo check-in e dashboard. Confirme também HTTPS, layout mobile, cookies `HttpOnly`/`Secure` e ausência de segredos no Git. Se o processo Java não couber nos 512 MB gratuitos, interrompa o deploy e avalie outro plano somente com autorização explícita do proprietário.

Para montar a vitrine fictícia após o deploy, execute `node scripts/publish-demo.mjs` uma única vez. O script cria uma conta técnica com senha aleatória, envia as seis capas originais ao Cloudinary e publica os eventos com dois tipos de ingresso gratuitos cada. Ele imprime a senha apenas no terminal: guarde-a em um gerenciador de senhas e nunca a inclua no repositório. Se a execução parar no meio, confira os eventos já publicados e retome com `EVENTHUB_DEMO_EMAIL` e `EVENTHUB_DEMO_PASSWORD` no ambiente local; o script reutiliza a conta e ignora os eventos já criados.

## Próximas evoluções

Pagamentos, recuperação de senha, painel administrativo, Redis e RabbitMQ continuam fora desta entrega. A vitrine possui seis eventos fictícios com capas originais em `frontend/public/demo-covers`; inscrições e QR Codes são apenas para demonstrar o produto e não dão acesso a eventos reais. O envio público de e-mail continua desativado até existir um domínio verificado.
