# 🧠 SME Customer Recovery System (Codezilla)

---

# 🚀 Overview

The SME Customer Recovery System is a SaaS-style platform that helps small businesses:

- Capture customer leads from WhatsApp, web forms, and ads
- Respond instantly using auto-replies or AI-generated replies
- Store and manage leads in a lightweight CRM
- Notify business owners in real-time
- Track lead status from NEW → WON / LOST

---

# 🏗️ System Architecture

## High-Level Flow

Customer → WhatsApp/Web Form → Spring Boot Backend  
→ Lead Service stores data  
→ Event triggers (LeadCreated)  
→ Messaging Service sends reply  
→ AI Service generates response (optional)  
→ Notification Service alerts owner  
→ Data stored in PostgreSQL CRM  
→ Admin Dashboard displays everything

---

# ⚙️ Tech Stack

## Backend

- Spring Boot 3
- Spring Web (REST APIs)
- Spring Security (JWT auth)
- Spring Data JPA
- Spring Events (event-driven flow)

## Database

- PostgreSQL (main database)
- Redis (cache + rate limiting)

## Messaging / Async

- Kafka (scaling phase)

## External Integrations

- WhatsApp Cloud API (Meta)
- Telegram Bot API
- OpenAI API (AI replies)
- Google Sheets API (optional CRM export)

## Frontend

- Next.js (Admin Dashboard)

## Deployment

- Docker
- AWS EC2
- Nginx reverse proxy

---

# 🧱 Core Modules

---

# 1. Lead Service (CRM Core)

## Responsibilities

- Store all incoming leads
- Manage lead status lifecycle
- Store lead metadata and source
- Track conversions

## Lead Status Flow

NEW → CONTACTED → QUALIFIED → WON / LOST

---

## Database Schema

```sql
TABLE leads (
  id UUID PRIMARY KEY,
  name VARCHAR(255),
  phone VARCHAR(50),
  email VARCHAR(255),
  source VARCHAR(50),  -- whatsapp/web/ig/facebook
  message TEXT,
  status VARCHAR(50),
  assigned_to VARCHAR(100),
  created_at TIMESTAMP,
  updated_at TIMESTAMP
);
```

## Message History Table

```sql
TABLE messages (
  id UUID PRIMARY KEY,
  lead_id UUID,
  direction VARCHAR(10), -- INBOUND / OUTBOUND
  content TEXT,
  channel VARCHAR(20), -- whatsapp/telegram/email
  created_at TIMESTAMP
);
```

# 2. Webhook Ingestion Service

## Purpose

Receives incoming leads from external sources.

## Endpoints

```http
POST /webhook/whatsapp
POST /webhook/webform
POST /webhook/telegram
```

## Flow

Receive request → Validate → Create Lead → Publish Event (LeadCreated)

## Example Payload

```json
{
  "name": "John",
  "phone": "+65XXXXXX",
  "message": "I want pricing",
  "source": "whatsapp"
}
```

# 3. Messaging Service

## Responsibilities

- Send WhatsApp replies
- Send Telegram notifications
- Send email notifications (optional)
- Handle auto-replies and AI-generated replies

### WhatsApp API

```http
POST https://graph.facebook.com/v19.0/{phone_number_id}/messages
```

### Auto Reply Example

```http
Hi {{name}}, thanks for contacting us 😊
We will get back to you shortly.
```

# 4. AI Reply Service (Optional but powerful)

## Flow

Lead message → Prompt Builder → OpenAI → Response → Messaging Service

## Prompt Template

You are a sales assistant for a SME business.

Business type: {{industry}}

Rules:

- Keep replies short
- Be polite
- Ask 1 follow-up question
- Focus on conversion

Customer message:
{{message}}

### Example AI Output

Hi! Thanks for reaching out 😊
May I know what date you are looking for an appointment?

# 5. Notification Service

## Channels

- Telegram (primary)
- Email (optional)

## Telegram Alert Example

🚨 New Lead

Name: John
Phone: +65XXXX
Message: Wants pricing
Source: WhatsApp

## 🔄 Event-Driven Architecture

### Event: LeadCreated

```java
public class LeadCreatedEvent {
    private UUID leadId;
}
```

## Flow

Webhook → LeadService.save()
→ Publish LeadCreatedEvent
→ MessagingService listens
→ AIService (optional)
→ NotificationService triggers

## 🧠 Spring Boot Project Structure

com.codezilla.crm
│
├── controller
│ ├── WebhookController
│ ├── LeadController
│
├── service
│ ├── LeadService
│ ├── MessagingService
│ ├── AIService
│ ├── NotificationService
│
├── event
│ ├── LeadCreatedEvent
│ ├── EventListener
│
├── repository
│ ├── LeadRepository
│ ├── MessageRepository
│
├── model
│ ├── Lead
│ ├── Message
│
├── dto
│ ├── LeadRequest
│ ├── LeadResponse
│
├── integration
│ ├── WhatsAppClient
│ ├── OpenAIClient
│ ├── TelegramClient
│
└── config
├── SecurityConfig
├── WebhookSecurityConfig

## 🔐 Security Model

### Authentication

- JWT for admin dashboard

### API Protection

- API key for webhook endpoints
- Signature validation for WhatsApp webhook (optional)

## 📊 Admin Dashboard (Next.js)

### Pages

- Dashboard
- Total leads
- Conversion rate
- Active leads
- Leads Table
- Filter by status
- Filter by source
- Search leads
- Lead Detail
- Conversation history
- Manual reply option
- Status update
- Settings
  - WhatsApp API config
  - AI toggle ON/OFF
  - Auto-reply templates
