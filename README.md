# SME Customer Recovery System (Codezilla)

A multi-tenant SaaS-style CRM for SMEs. Captures leads from WhatsApp / web forms / Telegram, replies automatically (template or AI), notifies the owner, and tracks status `NEW → CONTACTED → QUALIFIED → WON / LOST`.

See [Project.MD](Project.MD) for the full design.

## Stack

- Java 21, Spring Boot 3.3 (modular monolith)
- PostgreSQL 16 + Flyway, Redis 7
- Spring Security (JWT for `/api`, API-key for `/webhook`)
- Spring Events for `LeadCreated` flow
- Multi-tenant via `tenant_id` + Hibernate filter
- Stub & real clients for WhatsApp Cloud, Telegram Bot, OpenAI

## Project layout

```
src/main/java/com/codezilla/crm
├── CrmApplication.java
├── auth/          # /auth/login → JWT
├── config/        # WebClient, async, error handling
├── integration/   # WhatsApp / Telegram / OpenAI clients (stub + real)
├── lead/          # Lead entity, repo, service, controller, events
├── message/       # Conversation history
├── messaging/     # Outbound replies (auto + AI)
├── ai/            # AiReplyService
├── notification/  # Telegram alerts to owner
├── security/      # JwtService, JwtAuthFilter, WebhookApiKeyFilter, SecurityConfig
├── tenant/        # Tenant entity + TenantContext + TenantAwareEntity + filter
├── user/          # User entity for admin login
└── webhook/       # /webhook/{whatsapp,webform,telegram}
```

## Run locally (Docker)

```powershell
copy .env.example .env
docker compose up --build
```

App: http://localhost:8080  •  Postgres: 5432  •  Redis: 6379

## Run locally (without Docker)

Requires Java 21 + Maven + a running Postgres/Redis (use `docker compose up postgres redis`).

```powershell
./mvnw spring-boot:run
```

## Stub vs real integrations

Each external integration has two implementations selected by an env var:

| Integration | Env var          | Values         |
|-------------|------------------|----------------|
| WhatsApp    | `WHATSAPP_MODE`  | `stub` / `real` |
| Telegram    | `TELEGRAM_MODE`  | `stub` / `real` |
| OpenAI      | `OPENAI_MODE`    | `stub` / `real` |

Stubs log payloads (and keep an in-memory outbox) so the system runs end-to-end with zero credentials. Flip to `real` and supply the relevant token in `.env` to hit the real APIs.

## Try it

Demo tenant API key: `dev-tenant-key`  •  Demo admin: `admin@demo.local` / `password123`

```powershell
# Submit a lead via webhook
curl -X POST http://localhost:8080/webhook/webform `
  -H "Content-Type: application/json" -H "X-Api-Key: dev-tenant-key" `
  -d '{"name":"John","phone":"+6591234567","source":"web","message":"I want pricing"}'

# Login
curl -X POST http://localhost:8080/auth/login `
  -H "Content-Type: application/json" `
  -d '{"email":"admin@demo.local","password":"password123"}'

# List leads (use the token returned above)
curl http://localhost:8080/api/leads -H "Authorization: Bearer <token>"
```

## Roadmap

- **Phase 1 (this scaffold):** lead capture, auto/AI reply, owner notify, admin API, JWT, multi-tenant.
- **Phase 2:** Next.js admin dashboard, Redis rate limiting on webhooks, WhatsApp signature validation, Google Sheets export.
- **Phase 3:** Kafka event bus, schema-per-tenant for enterprise, Nginx + EC2 deploy.
