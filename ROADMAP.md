# Production Readiness Roadmap

Status tracker for the 52 features identified in the launch-readiness review.

Legend: ✅ done · 🟡 in progress · ⬜ not started · ⏭ deferred / out of scope

---

## 🔴 Must-have before any paying customer

| #   | Feature                                                         | Status | Notes                                                                                    |
| --- | --------------------------------------------------------------- | ------ | ---------------------------------------------------------------------------------------- |
| 1   | User management (invite, list, role, disable, reset password)   | ✅     | `/api/users` + `/users` dashboard page. Roles: OWNER / ADMIN / AGENT / VIEWER.           |
| 2   | Self-service signup + tenant creation                           | ⬜     | `/auth/register` → create tenant + first OWNER user, send verification email.            |
| 3   | Forgot password (email token) + change own password             | 🟡     | Change own password done (`/api/users/me/password`); forgot-password reset flow pending. |
| 4   | Per-tenant integration credentials (WhatsApp, Telegram, OpenAI) | ⬜     | Move from env vars to encrypted tenant columns; required for multi-tenant onboarding.    |
| 5   | Secrets at rest encryption (AES-GCM)                            | ⬜     | Needed by #4. Master key from env / KMS.                                                 |
| 6   | Email verification on signup                                    | ⬜     | Token in DB + email link.                                                                |
| 7   | Rate limit on `/auth/login`                                     | ⬜     | Redis fixed-window per email/IP. Reuse `WebhookRateLimitFilter` pattern.                 |
| 8   | Account lockout after N failed logins                           | ⬜     | `failed_login_count`, `locked_until` columns on users.                                   |
| 9   | Error tracking (Sentry / GlitchTip)                             | ⬜     | Backend + frontend SDK.                                                                  |
| 10  | Automated nightly Postgres backups to S3                        | ⬜     | Cron container in `docker-compose.prod.yml`.                                             |
| 11  | Readiness/liveness split + dependency health checks             | ⬜     | Custom Actuator HealthIndicator for Postgres + Redis.                                    |
| 12  | TOS / Privacy / Cookie banner                                   | ⬜     | Required by GDPR/PDPA before processing customer data.                                   |
| 13  | Data export per user request (GDPR Art. 15)                     | ✅     | CSV export at `/api/leads/export`.                                                       |
| 14  | Hard-delete job for soft-deleted records >30d                   | ⬜     | Scheduled @Scheduled task or pg_cron.                                                    |

## 🟡 Strongly recommended for launch

| #   | Feature                                      | Status | Notes                                                |
| --- | -------------------------------------------- | ------ | ---------------------------------------------------- |
| 15  | Stripe Billing / subscriptions               | ⬜     | Plans + customer portal.                             |
| 16  | Plan-based feature gating                    | ⬜     | Enforce limits (leads/mo, AI replies/mo).            |
| 17  | Onboarding wizard (3 steps after signup)     | ⬜     | Connect WhatsApp → set auto-reply → test.            |
| 18  | Lead assignment + "assigned to me" filter    | ⬜     | `assigned_to_user_id` FK on leads.                   |
| 19  | Notes on leads (internal-only)               | ⬜     | `lead_notes` table.                                  |
| 20  | Tags / labels on leads                       | ⬜     | `lead_tags` many-to-many.                            |
| 21  | Bulk actions (status change, delete)         | ⬜     | Frontend checkbox column + backend batch endpoints.  |
| 22  | WhatsApp template message support            | ⬜     | For sends outside 24h window. Required by Meta.      |
| 23  | Real-time conversation view (SSE or polling) | ⬜     | SSE preferred.                                       |
| 24  | Full-text search on leads + messages         | ⬜     | Postgres `tsvector` + GIN index.                     |
| 25  | Dashboard analytics charts                   | ⬜     | Leads/day, source breakdown, response time, funnel.  |
| 26  | Email template editor (multi-template)       | ⬜     | Variables + per-channel templates.                   |
| 27  | 2FA (TOTP) for ADMIN/OWNER                   | ⬜     | Use `dev.samstevens.totp` lib.                       |
| 28  | Audit log filters + export                   | 🟡     | Audit log exists; filter/export UI pending.          |
| 29  | Outbound webhooks for customer integrations  | ⬜     | `webhook_subscriptions` table + delivery with retry. |
| 30  | Public REST API + per-user API keys          | ⬜     | Extend `WebhookApiKeyFilter` pattern.                |

## 🟢 Nice-to-have post-launch

| #   | Feature                              | Status | Notes                                                |
| --- | ------------------------------------ | ------ | ---------------------------------------------------- |
| 31  | Mobile-responsive QA pass            | ⬜     | Tailwind already responsive; verify on real devices. |
| 32  | Mobile push notifications            | ⬜     | Web Push API or native wrapper.                      |
| 33  | Instagram DM + Facebook Messenger    | ⬜     | Same Meta Graph API.                                 |
| 34  | Calendar / appointment booking       | ⬜     | Calendly-style.                                      |
| 35  | RAG knowledge base for AI replies    | ⬜     | pgvector + upload UI.                                |
| 36  | Team chat / @mentions                | ⬜     |                                                      |
| 37  | i18n (EN / ZH / MS)                  | ⬜     | next-intl.                                           |
| 38  | Dark mode                            | ⬜     | Tailwind dark variant.                               |
| 39  | Admin "impersonate user" for support | ⬜     | Audit-logged.                                        |
| 40  | In-app changelog / what's new        | ⬜     | Markdown-driven.                                     |

## 🛠️ Engineering / DevOps before scaling

| #   | Feature                                        | Status | Notes                                                                                     |
| --- | ---------------------------------------------- | ------ | ----------------------------------------------------------------------------------------- |
| 41  | CI/CD (GitHub Actions: test → image → deploy)  | ⬜     |                                                                                           |
| 42  | Tenant isolation integration tests             | ⬜     | **Critical.** Verify tenant A cannot read tenant B's data. Testcontainers already set up. |
| 43  | DB migration rollback strategy                 | ⬜     | Document + Flyway `undo` (paid) or manual procedure.                                      |
| 44  | Secrets manager (Doppler / AWS SM / Vault)     | ⬜     | Replace `.env.prod`.                                                                      |
| 45  | Container image scanning (Trivy in CI)         | ⬜     |                                                                                           |
| 46  | Dependency vulnerability scanning (Dependabot) | ⬜     | Enable in repo settings.                                                                  |
| 47  | Staging environment                            | ⬜     | Mirror of prod.                                                                           |
| 48  | Monitoring (Prometheus + Grafana)              | ⬜     | Actuator metrics already exposed.                                                         |
| 49  | Uptime monitoring                              | ⬜     | UptimeRobot / BetterStack.                                                                |
| 50  | Log aggregation (Loki / Papertrail)            | ⬜     |                                                                                           |
| 51  | DB read replica                                | ⏭     | Only when load demands.                                                                   |
| 52  | Kafka / RabbitMQ for async events              | ⏭     | Stay on Spring events until scale demands.                                                |

---

## Recently completed

- **#4 + #5 — Per-tenant credentials + AES-GCM encryption** (2026-06-08): WhatsApp/Telegram/OpenAI tokens now stored encrypted per tenant. Settings UI lets admins paste credentials (write-only); API returns `*Configured` booleans only. Real integration clients resolve the active tenant's creds per call, falling back to env. 7 new tests (5 cipher, 2 settings isolation). 19/19 tests pass.
- **#42 — Tenant isolation tests + isolation hardening** (2026-06-08): 11 MockMvc cases verify A/B isolation across leads, users, settings, audit, messages. Exposed and fixed a real bug — `findAll()` queries weren't tenant-scoped because the `@Filter` interceptor didn't survive into service-layer transactions. Now all queries use explicit `findAllByTenantId*` methods. `LeadService.get()` also adds an explicit tenant check (Hibernate `@Filter` does not apply to find-by-PK).
- **#1 — User management** (2026-06-08): backend `/api/users` CRUD + role enum + disable + own/admin password change + last-login tracking; frontend `/users` and `/profile` pages.
- **Phase 3** (2026-06-04): Meta inbound adapter, audit log + soft delete, CSV import/export, SMTP email, Google Sheets export, Nginx prod compose + Let's Encrypt.
- **Phase 2**: webhook rate limit, WhatsApp signature verification, settings API, stats API, reply endpoint, Next.js dashboard.
- **Phase 1**: monolith scaffold, multi-tenant data model, JWT auth, stub/real integration pattern, Spring events for async flow.

## Next recommended

1. **#2 self-service signup** — needed for demos / sales motion. With per-tenant creds in place, new tenants can fully self-configure.
2. **#7 + #8 login rate limit + lockout** — minimal anti-abuse before public launch.
3. **#15 Stripe billing** — once you have a willing first customer.
4. **#41 CI/CD** — GitHub Actions: test → build image → deploy. Worth setting up before going live.
