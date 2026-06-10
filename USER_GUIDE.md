# Codezilla CRM — User Manual

A practical guide to setting up and running your Codezilla CRM workspace.

This manual is written for the **SME owner / admin** who will use the dashboard day‑to‑day. A short [self‑host setup](#appendix-a--self-hosting-the-platform) appendix at the end covers running your own instance.

---

## Table of contents

1. [What this CRM does](#1-what-this-crm-does)
2. [Create your workspace](#2-create-your-workspace)
3. [Dashboard tour](#3-dashboard-tour)
4. [Connect WhatsApp](#4-connect-whatsapp)
5. [Connect Telegram (owner alerts)](#5-connect-telegram-owner-alerts)
6. [Auto‑reply: 3 ways to answer leads](#6-auto-reply-3-ways-to-answer-leads)
7. [Working a lead](#7-working-a-lead)
8. [Notes, tags & assignment](#8-notes-tags--assignment)
9. [Bulk actions, search & export](#9-bulk-actions-search--export)
10. [Public booking page](#10-public-booking-page)
11. [Outbound message templates](#11-outbound-message-templates)
12. [Invite your team](#12-invite-your-team)
13. [Two‑factor authentication (2FA)](#13-two-factor-authentication-2fa)
14. [API keys & public REST API](#14-api-keys--public-rest-api)
15. [Outbound webhooks](#15-outbound-webhooks)
16. [Billing & plan limits](#16-billing--plan-limits)
17. [Audit log](#17-audit-log)
18. [Troubleshooting](#18-troubleshooting)
19. [Appendix A — Self‑hosting the platform](#appendix-a--self-hosting-the-platform)
20. [Appendix B — Glossary](#appendix-b--glossary)

---

## 1. What this CRM does

Codezilla CRM captures every customer enquiry that hits your business and helps you reply fast:

- **Captures** leads from WhatsApp, Telegram, your website webform, your public booking page, or any external system via the public REST API.
- **Replies automatically** using one of three tiers (cheapest first):
  1. **FAQ engine** — instant, free, deterministic for common questions.
  2. **Local AI (Ollama)** — free, runs on the server, good enough for most replies.
  3. **OpenAI** — paid (Pro plan), best quality.
- **Notifies the owner** via Telegram the moment a lead lands.
- **Tracks status** `NEW → CONTACTED → QUALIFIED → WON / LOST` so nothing slips.
- **Works as a team** — invite agents, assign leads, leave internal notes, tag.

Everything is **multi‑tenant**: every business has its own isolated workspace; users only see their own data.

---

## 2. Create your workspace

1. Open the dashboard URL your provider gave you (e.g. `https://crm.example.com`).
2. Click **Create one** on the login page.
3. Fill in:
   - **Business name** — appears in auto‑replies (e.g. "Thanks for contacting Acme!").
   - **Industry** (optional) — used to bias the AI ("you are a sales assistant for an SME in `retail`…").
   - **Your name** (optional)
   - **Email + password** (min 8 chars)
4. Check **I agree to the Terms and Privacy Policy** → **Create workspace**.
5. You'll be redirected to a 3‑step **onboarding wizard**:
   - Step 1 — paste your WhatsApp credentials (or skip)
   - Step 2 — pick an auto‑reply template
   - Step 3 — create a sample lead to verify everything works

> If your provider has email verification turned on, you'll be asked to click a link in your inbox before you can log in.

Your first user is the **OWNER** of the workspace. Only an OWNER can later transfer ownership or delete the workspace.

---

## 3. Dashboard tour

The left sidebar gives you everything:

| Icon | Page | What it's for |
|------|------|---------------|
| 🏠 | **Dashboard** | KPI cards (new leads, conversion rate, response time) + 30‑day trend + funnel + sources. |
| 📥 | **Leads** | The inbox. Search, filter by status / source / assignee, bulk actions. |
| 📜 | **Audit log** | Every change in the workspace, filterable + CSV export. |
| 👥 | **Users** | Invite teammates, change roles, disable accounts. |
| 📚 | **Knowledge** | RAG documents that AI consults before replying. |
| 💬 | **FAQs** | Rule‑based instant answers (free, no LLM call). |
| ✉️ | **Templates** | Reusable outbound messages with `{{name}}` placeholders. |
| 💳 | **Billing** | Current plan, usage bar, upgrade / customer portal. |
| ⚙️ | **Settings** | Business info, integrations, AI provider, booking slug. |
| 👤 | **Profile** | Your own password, 2FA, last login. |

The **Dashboard** page is your morning glance. The **Leads** page is where you'll spend most of your time.

---

## 4. Connect WhatsApp

You need a Meta WhatsApp Cloud API number — Codezilla does **not** use the unofficial WhatsApp Web protocol.

### One‑time setup at Meta

1. Go to <https://developers.facebook.com/apps/> and create a Business app.
2. Add the **WhatsApp** product.
3. Note three values from the API Setup page:
   - **Phone number ID** (a long numeric string)
   - **Permanent access token** (start with a temporary one for testing)
   - **Verify token** — pick any random string; you'll paste the same value into Codezilla.
4. Set the **callback URL** on Meta to:
   `https://<your-crm-domain>/webhook/whatsapp`
5. Subscribe to the `messages` field.

### Paste into Codezilla

Go to **Settings → Integrations → WhatsApp** and paste:

- Phone number ID
- Access token
- Verify token (same string you gave Meta)

The fields are write‑only — once saved they're encrypted at rest and you'll only see "configured" / "not set" indicators. You can clear a value at any time by saving an empty string.

### Test it

Send a WhatsApp message from your personal phone to the business number. Within seconds:

1. A new lead should appear in **Leads** with source `whatsapp`.
2. You should get a Telegram ping (if Telegram is configured).
3. The auto‑reply should land back in your personal WhatsApp.

If it doesn't, check the [troubleshooting section](#18-troubleshooting).

---

## 5. Connect Telegram (owner alerts)

Telegram is used for **owner notifications** — every new lead pings you in your own private chat.

1. Open Telegram → search for **@BotFather** → `/newbot` → follow prompts. You'll get a **bot token**.
2. Start a chat with your new bot, send any message.
3. Get your chat ID: visit `https://api.telegram.org/bot<TOKEN>/getUpdates` in a browser; copy the `chat.id` (a number).
4. In Codezilla: **Settings → Integrations → Telegram** → paste the bot token and chat ID → save.

You'll get a "test ping" — confirm it lands in Telegram. Subsequent leads notify automatically.

---

## 6. Auto‑reply: 3 ways to answer leads

Every incoming lead message goes through the **AI Gateway** in this order:

```
Lead message → FAQ engine → Local AI (Ollama) → OpenAI (Pro plan)
```

The first tier that can answer wins. If a tier is unavailable (no FAQs, Ollama down, no OpenAI key) it falls through to the next.

You can pin the order per workspace in **Settings → Business → AI provider**:

| Choice | Behavior |
|--------|----------|
| **Auto** (recommended) | FAQ → server default → fallback |
| **FAQ only** | Just the rule engine; no LLM. Cheapest, fully deterministic. |
| **FAQ → Ollama** | Free local LLM for non‑FAQ messages. |
| **FAQ → OpenAI** | Higher‑quality replies. Requires Pro plan; falls back to Ollama if your plan doesn't allow OpenAI. |

You can also disable AI entirely by un‑checking **Enable AI replies** — the system will then use the fixed **auto‑reply template** instead (e.g. "Hi {{name}}, thanks for contacting us! We'll get back to you shortly.").

### 6a. FAQs (instant, free)

Go to **FAQs** → **New FAQ**.

- **Pattern**: a piece of text to match against the lead's message (case‑insensitive substring). Keep it short and specific: `shipping`, `opening hours`, `refund`.
- **Reply**: what to send back.
- **Priority**: higher wins ties. Use this to make `shipping to malaysia` beat the more general `shipping`.

The matcher tries (priority DESC, pattern length DESC). The **Hits** column tells you which FAQs are actually being used so you can prune.

> **Why FAQs first?** A WhatsApp lead asking "how much is shipping?" doesn't need a 3‑second LLM round trip. The FAQ engine answers in ~5ms and costs nothing.

### 6b. Knowledge base for AI replies (RAG)

When the AI does run, it consults your **Knowledge** library first.

Go to **Knowledge** → **Add document** → paste your FAQ, product catalog, return policy, etc. Each document is embedded (turned into a vector) so the AI can pull the top 3 most relevant ones into its system prompt before generating a reply.

This means AI answers stay accurate to your business — not made up.

### 6c. Picking a model

- **Free plan → Ollama** (default). Runs the `qwen2.5:3b` model on the server. Quality is good for short replies; weaker on long, reasoning‑heavy questions.
- **Pro plan → OpenAI** (`gpt-4o-mini` by default). Much better at multi‑turn nuance and following complex instructions.

To use OpenAI you also need to paste your own API key in **Settings → Integrations → OpenAI** — Codezilla never charges your card for LLM usage, you pay OpenAI directly.

---

## 7. Working a lead

Click any row in **Leads** to open the detail page. You'll see:

- **Header** — name, phone, email, source, status badge.
- **Status selector** — change `NEW → CONTACTED → QUALIFIED → WON / LOST`.
- **Assignee picker** — assign to a teammate (or yourself).
- **Conversation** — every inbound + outbound message, in order, live‑updated via SSE (no need to refresh).
- **Reply box** — type a reply, hit send (goes via WhatsApp).
- **Internal notes** — visible only to your team, not the customer.
- **Tags** — colour‑coded labels you can filter by.
- **Appointments** — any bookings the customer made via your public booking page.
- **Send template** — pick a pre‑approved WhatsApp template (required by Meta for sends >24h after the last customer message).

The **status** is what most reporting hangs off — keep it current.

---

## 8. Notes, tags & assignment

### Notes

Click **Add note** on the lead detail page. Notes record the author and timestamp, are never visible to the customer, and survive forever (no auto‑delete).

### Tags

Create the tag once in **FAQs > Tags catalogue** (or inline on a lead). Then attach as many as you like per lead. The **Leads** list lets you filter by tag.

Suggested taxonomy: `vip`, `cold-call`, `referral`, `repeat`, `complaint`.

### Assignment

Two ways to give a lead to a teammate:

- **From the detail page** — click the assignee picker.
- **In bulk** — multi‑select on the leads list → "Assign to…".

The leads list has a **Mine** toggle that filters to leads assigned to you. Bookmark `/leads?mine=true` as your personal queue.

---

## 9. Bulk actions, search & export

### Search

Use the search box at the top of **Leads**. Searches across name, phone, email, and message body. Debounced; results update as you type.

### Bulk actions

Tick the checkboxes on multiple leads → a sticky action bar appears at the bottom:

- **Set status** — bulk transition (e.g. mark all losses).
- **Delete** — soft‑delete; rows disappear from the UI but are kept for 30 days for accidental‑restore by an admin via the API. After 30 days a nightly job hard‑deletes them.

### CSV export

- **Leads** page → **Export** button → downloads `leads.csv` of the current filter.
- **Audit log** page → **Export** button → CSV of the audit trail with whatever filters you applied.

These exports cover GDPR Art. 15 ("data access request") for your customer records.

---

## 10. Public booking page

Each workspace gets a public `/book/{slug}` page that anyone with the link can use to book an appointment with you — no login.

1. **Settings → Business → Booking** → set your **booking slug** (e.g. `acme-singapore`) → tick **Booking enabled** → optional **blurb** ("30‑min discovery call. Bring your existing CRM data if you have one.").
2. Share `https://<your-domain>/book/acme-singapore` on Instagram, your website, email signatures, anywhere.
3. When a customer fills the form, Codezilla creates **both** a Lead and an Appointment, and you get the usual Telegram + WhatsApp auto‑reply flow.

Appointments show up on the corresponding lead's detail page.

---

## 11. Outbound message templates

Different from **FAQs** — FAQs are *automatic* replies. **Templates** are messages *you* trigger.

Use them for: follow‑ups ("Just checking in"), payment reminders, post‑sale thank‑yous.

**Templates** page → **New template**:

- **Channel** (`whatsapp` / `email`)
- **Event** — when it applies (`reply`, `follow_up`, etc.)
- **Body** — supports `{{name}}`, `{{phone}}`, `{{email}}`, `{{source}}`, `{{business}}` placeholders.
- **Default for this (channel, event)** — exactly one template per channel/event can be the default.

The auto‑reply flow picks: DB default for `whatsapp/auto_reply` → workspace `auto_reply_template` setting → hard‑coded fallback. So setting a "Default" template once means all new tenants don't need to write their own.

---

## 12. Invite your team

**Users** page → **Invite user**:

- Email, name, role (OWNER / ADMIN / AGENT / VIEWER), temporary password.

### Role permissions

| Action | OWNER | ADMIN | AGENT | VIEWER |
|--------|:-----:|:-----:|:-----:|:------:|
| View leads, messages, audit | ✅ | ✅ | ✅ | ✅ |
| Reply, change status, assign | ✅ | ✅ | ✅ | ❌ |
| Manage FAQs, templates, knowledge | ✅ | ✅ | ❌ | ❌ |
| Invite / disable users | ✅ | ✅ | ❌ | ❌ |
| Change integrations (WhatsApp etc) | ✅ | ✅ | ❌ | ❌ |
| Manage billing | ✅ | ❌ | ❌ | ❌ |
| Delete workspace | ✅ | ❌ | ❌ | ❌ |

OWNER ⊃ ADMIN ⊃ AGENT ⊃ VIEWER (a higher role can do anything a lower role can).

### Disable vs delete

Use **disable** when an employee leaves — their account stays in the audit log but they can no longer log in. Deleting a user removes them from audit history; almost never the right choice.

---

## 13. Two‑factor authentication (2FA)

Strongly recommended for any account with the ADMIN or OWNER role.

1. **Profile → Security → Enable 2FA**.
2. Scan the QR code in Google Authenticator / Authy / 1Password.
3. Enter the 6‑digit code to confirm enrolment.
4. Save your recovery codes somewhere safe (password manager).

On future logins the system will ask for the 6‑digit code after your password. You can disable 2FA from the same page (re‑authentication required).

If you lose your authenticator and your recovery codes: an OWNER can reset 2FA for any user from **Users → … → Reset 2FA**.

---

## 14. API keys & public REST API

For when you want to push leads in from your own website, or read leads from another system.

### Create a key

**Profile → API keys → Create key**:

- **Name** — descriptive ("website contact form").
- **Expires in days** — optional but recommended; rotate yearly.

The cleartext key (`crm_xxxxxxxx…`) is shown **once**. Copy it to your secrets manager immediately — Codezilla only stores a SHA‑256 hash and the last 4 characters for display.

A key inherits your user role, so a VIEWER's key can read but not write.

### Use the key

```bash
# List leads
curl https://crm.example.com/v1/leads \
  -H "Authorization: Bearer crm_xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx"

# Create a lead
curl https://crm.example.com/v1/leads \
  -H "Authorization: Bearer crm_xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx" \
  -H "Content-Type: application/json" \
  -d '{"name":"Jane","phone":"+6591234567","source":"website","message":"Quote please"}'

# Update status
curl -X PATCH https://crm.example.com/v1/leads/<lead-id>/status \
  -H "Authorization: Bearer crm_xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx" \
  -H "Content-Type: application/json" \
  -d '{"status":"CONTACTED"}'
```

### Revoke a key

Same page, click **Revoke**. The key stops working immediately; existing requests are not affected mid‑flight.

---

## 15. Outbound webhooks

Get notified in your own systems (Zapier, n8n, your custom CRM) whenever something happens.

**Settings → Webhooks → Add subscription** (admin only):

- **Target URL** — where Codezilla will POST.
- **Events** — comma‑separated, supports wildcards. `lead.*` matches every lead event. `lead.created` only matches creates.

On save, a **signing secret** (`whsec_…`) is shown **once**. Store it in your endpoint so you can verify the signature on incoming requests.

### What you'll receive

Every event is a JSON POST:

```json
{
  "id": "f0e1d2c3-…",
  "event": "lead.created",
  "tenantId": "a1b2c3d4-…",
  "createdAt": "2026-06-10T10:30:00Z",
  "data": { "id": "...", "name": "Jane", "phone": "...", "message": "..." }
}
```

Headers:

- `X-Crm-Event: lead.created`
- `X-Crm-Signature-256: sha256=<hex>` — HMAC‑SHA256 over the raw body, using the signing secret. **Verify this before trusting the payload.**

### Reliability

- 5‑second timeout per delivery, single attempt per event.
- After **10 consecutive failures** a subscription is auto‑disabled. The Webhooks page shows `lastError` so you can fix and re‑enable.
- For higher reliability run a small queue on your side (Kafka / RabbitMQ / SQS) that absorbs delivery and retries within your own SLA.

---

## 16. Billing & plan limits

**Billing** page shows your current plan, lead usage for the month, and the **Manage subscription** button (links to Stripe's Customer Portal).

### Plan tiers

| Plan | Leads / month | AI replies | Outbound webhooks | API keys |
|------|---------------|:----------:|:-----------------:|:--------:|
| **Free** | 100 | FAQ + Ollama | ✅ | ✅ |
| **Pro** | 5,000 | + OpenAI | ✅ | ✅ |

When you hit the cap:

- **Webhook & booking‑form leads** are still accepted (you don't lose customers because of a billing limit) but flagged in the audit log with `OVER_QUOTA`.
- **Manual lead creation** (dashboard + `/v1` API) returns **HTTP 402 Payment Required** with an upgrade message.

### Upgrade

Click **Upgrade to Pro** → you're sent to Stripe Checkout. After successful payment, the webhook from Stripe updates your plan in real time — typically within 2 seconds. If you don't see the change immediately, refresh the page once.

### Cancel

Click **Manage subscription** in the Customer Portal → cancel. You keep Pro features until the end of the current billing period, then automatically drop to Free.

---

## 17. Audit log

Every meaningful change in the workspace is recorded in **Audit log**: lead creates, status changes, user invites, settings edits, integration tokens being set/cleared (the values themselves are never logged).

### Filters

- **Entity type** (`lead`, `user`, `tenant`, `subscription`, …)
- **Action** (`CREATE`, `UPDATE`, `DELETE`, `LOGIN`, `OVER_QUOTA`, …)
- **Actor** — substring match on user email
- **Date range**

### Export

Filtered view → **Export** → CSV. Useful for monthly compliance reports or investigating "who changed X?".

---

## 18. Troubleshooting

### "Forgot password" link arrived but doesn't work

Reset tokens expire after 60 minutes. Request a new one.

### Locked out — too many login attempts

After 5 failed attempts the account is locked for 15 minutes. Wait, then try again, or have an admin reset your password from **Users**.

### WhatsApp messages arrive but auto‑reply doesn't send

Check **Audit log** for an `OVER_QUOTA` event (you may have hit the monthly cap). Then check **Settings → Integrations → WhatsApp** — if "Access token configured" went back to "not set", Meta probably expired your token. Generate a permanent one in the Meta developer console.

### `/v1` API returns 401

- Header must be exactly `Authorization: Bearer crm_...` (no `Token`, no `ApiKey`).
- Check the key isn't revoked or expired (**Profile → API keys**).
- The user who owns the key must still be enabled (check **Users**).

### Webhook subscription got auto‑disabled

Open **Settings → Webhooks**, read the `lastError`. Common causes:
- DNS resolution failed (typo in the URL?)
- Your endpoint returned 5xx for 10 events in a row
- TLS certificate expired

Fix the endpoint, then click **Re‑enable**; the failure counter resets.

### Telegram notifications stopped

Bot tokens can be revoked by BotFather. Generate a fresh token, paste in **Settings → Integrations → Telegram**.

### AI replies sound weird / inaccurate

Add or refine documents in **Knowledge**. The AI uses the **top 3 most relevant docs** as context. Short, factual, structured documents (FAQ‑style) work best; long marketing copy works worst.

For very common questions, move them to a **FAQ** — deterministic, free, never hallucinates.

### "AI gateway status" shows all providers unavailable

- **Ollama**: in self‑host, make sure you ran `docker compose --profile ai up` (dev) or that the `ollama` and `ollama-init` services are healthy (prod).
- **OpenAI**: you need an API key configured AND a Pro plan AND `ai_provider` set to `openai` or `auto`.

---

## Appendix A — Self‑hosting the platform

For the sysadmin running Codezilla on their own infrastructure.

### Requirements

- A Linux server with **Docker 24+** and **Docker Compose v2**
- 4 GB RAM minimum (8 GB recommended once Ollama is running; Qwen 2.5 3B uses ~3 GB)
- 20 GB disk (Postgres + Ollama model files + backups)
- A domain name pointing to the server's public IP
- Stripe account (only if you want to charge customers)
- An SMTP provider (Amazon SES, Postmark, Mailgun, …) for transactional email

### Development setup (laptop)

```powershell
git clone https://github.com/<your-org>/customer_recovery_system.git
cd customer_recovery_system
copy .env.example .env
docker compose up --build
```

Open <http://localhost:3000>. Default seeded login: `admin@demo.local` / `password123` (change immediately).

To enable the local LLM:

```powershell
docker compose --profile ai up -d ollama ollama-init
```

First start pulls the `qwen2.5:3b` model (~2 GB). Subsequent restarts are instant — the model is cached on a named volume.

### Production setup

```bash
# 1. Clone and configure
git clone https://github.com/<your-org>/customer_recovery_system.git
cd customer_recovery_system
cp .env.prod.example .env.prod
$EDITOR .env.prod   # fill in every value, especially:
                    # POSTGRES_PASSWORD, JWT_SECRET, APP_ENCRYPTION_KEY,
                    # PUBLIC_BASE_URL, GRAFANA_ADMIN_PASSWORD,
                    # BACKUP_S3_BUCKET, AWS_*, plus integrations.

# 2. Generate strong secrets
openssl rand -base64 32          # APP_ENCRYPTION_KEY (must be base64 of 32 bytes)
openssl rand -base64 48          # JWT_SECRET (>= 32 chars after decoding)

# 3. Bring up the stack
docker compose -f docker-compose.prod.yml --env-file .env.prod up -d

# 4. Tail logs once
docker compose -f docker-compose.prod.yml logs -f app
```

Nginx (in the compose file) handles TLS via Let's Encrypt the first time it starts — point your DNS at the server and give it 1–2 minutes.

See [DEPLOY.md](DEPLOY.md) for the deep dive: backups, rollbacks, secrets managers (Doppler / AWS SM / Vault / SOPS), monitoring (Prometheus + Grafana on `:3001`), uptime checks, log aggregation.

### Updating

```bash
git pull
docker compose -f docker-compose.prod.yml --env-file .env.prod pull   # if using GHCR images
docker compose -f docker-compose.prod.yml --env-file .env.prod up -d --build app
```

Flyway runs any new migrations automatically on app startup. If a migration goes wrong, undo scripts live in `src/main/resources/db/undo/`; run with `deploy/scripts/rollback.sh`.

### Backups

Nightly Postgres dumps are uploaded to your S3 bucket by the `backup` service (configured in `.env.prod`). To restore:

```bash
# Pull latest backup
aws s3 cp s3://$BACKUP_S3_BUCKET/$BACKUP_S3_PREFIX/crm/$(date +%Y%m%d)T*.sql.gz ./restore.sql.gz

# Restore into a fresh DB (DESTRUCTIVE — drop + recreate first if needed)
gunzip -c restore.sql.gz | docker compose exec -T postgres psql -U $POSTGRES_USER $POSTGRES_DB
```

### Monitoring

Grafana ships on `http://localhost:3001` (or whatever you put in `GRAFANA_ROOT_URL`). Default dashboard shows: HTTP request rate, p95 latency, JVM memory, Hikari pool saturation, 5xx rate. Configure alerts (BetterStack / UptimeRobot) against the `/actuator/health` endpoint.

### Error tracking

Create projects at <https://sentry.io> — one for the Spring Boot backend, one for the Next.js frontend. Paste DSNs into `.env.prod`:

```
SENTRY_DSN=https://...@sentry.io/123             # backend
NEXT_PUBLIC_SENTRY_DSN=https://...@sentry.io/124 # frontend
```

The SDK is inert until DSN is set, so dev stays clean. Events are auto‑tagged with `tenant_id` for per‑customer filtering.

---

## Appendix B — Glossary

| Term | Meaning |
|------|---------|
| **Tenant** | One customer business / workspace. Everything in Codezilla is tenant‑scoped. |
| **Lead** | A potential customer who reached out. Created by webhook, manual entry, booking page, or `/v1` API. |
| **AI Gateway** | The router that picks which tier (FAQ / Ollama / OpenAI) answers a lead. |
| **FAQ** | A rule‑based pattern → reply. Tried before any LLM. |
| **RAG** | Retrieval‑Augmented Generation — pulling relevant Knowledge docs into the AI prompt. |
| **Stub mode** | An integration that pretends to work but doesn't actually call the third‑party API. Set `WHATSAPP_MODE=stub` etc. for tests. |
| **OWNER / ADMIN / AGENT / VIEWER** | Role hierarchy. OWNER is the workspace creator. |
| **API key** (`crm_…`) | Personal token for the public REST API. |
| **Signing secret** (`whsec_…`) | Per‑subscription secret used to HMAC‑sign outbound webhook bodies. |
| **Audit log** | Immutable record of every change. CSV‑exportable. |
| **Soft delete** | Marked as deleted, hidden from the UI, kept 30 days, then hard‑deleted by a scheduled job. |

---

**Need help?** Open an issue at <https://github.com/your-org/customer_recovery_system/issues> or email `support@codezilla.example`.
