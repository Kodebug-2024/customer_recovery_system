# Production Deployment

Single-VM deploy on Ubuntu 22.04 with Docker, Nginx, and Let's Encrypt.
Works on any VPS: EC2 t3.small, DigitalOcean $12 droplet, Hetzner CX22, etc.

## 1. Server prep

```bash
sudo apt update && sudo apt -y upgrade
sudo apt -y install docker.io docker-compose-plugin git
sudo usermod -aG docker $USER
exit   # log back in
```

## 2. Clone and configure

```bash
git clone <your-repo-url> crm && cd crm
cp .env.prod.example .env.prod
nano .env.prod   # fill in DOMAIN, POSTGRES_PASSWORD, JWT_SECRET, integration secrets
```

Point your DNS A record `crm.yourdomain.com` → server IP. Wait for propagation.

Edit `deploy/nginx/conf.d/app.conf` and replace every `example.com` with your real domain.

## 3. Bootstrap TLS certificate

Start everything except certbot (HTTP-only mode first so certbot can solve the challenge):

```bash
docker compose -f docker-compose.prod.yml --env-file .env.prod up -d postgres redis app dashboard nginx
docker compose -f docker-compose.prod.yml --env-file .env.prod run --rm certbot \
  certonly --webroot -w /var/www/certbot \
  -d crm.yourdomain.com --agree-tos --email you@yourdomain.com --no-eff-email
docker compose -f docker-compose.prod.yml --env-file .env.prod up -d
```

Auto-renewal runs every 12h via the `certbot` service.

## 4. Verify

```bash
curl https://crm.yourdomain.com/actuator/health
# {"status":"UP"}
```

Browse to https://crm.yourdomain.com and log in with the seeded admin
(`admin@demo.local` / `password123`) — **change this password immediately** via the API.

## 5. Updates

```bash
git pull
docker compose -f docker-compose.prod.yml --env-file .env.prod up -d --build
```

## 6. Backups

Dump Postgres nightly:

```bash
0 3 * * * docker exec crm-postgres-1 pg_dump -U crm crm | gzip > /backup/crm-$(date +\%F).sql.gz
```

## 7. Hardening checklist

- [ ] UFW: `sudo ufw allow 22,80,443/tcp && sudo ufw enable`
- [ ] Fail2ban for sshd
- [ ] Disable password SSH; use keys only
- [ ] Rotate `JWT_SECRET` and any leaked tokens
- [ ] Set per-tenant `webhookSecret` so Meta HMAC validation is enforced
- [ ] Set up offsite backups (S3, Backblaze B2)

## 8. Secrets management

The container ships with `deploy/scripts/entrypoint.sh` that optionally pulls
secrets from an external manager _before_ the JVM starts. Selection via
`SECRETS_PROVIDER`:

| Value            | Required env                                                          | Notes                                                                                             |
| ---------------- | --------------------------------------------------------------------- | ------------------------------------------------------------------------------------------------- |
| (unset) / `none` | —                                                                     | Use plain env vars set by compose / k8s. No-op.                                                   |
| `doppler`        | `DOPPLER_TOKEN`                                                       | `doppler secrets download` populates the env. Add `doppler` CLI to the image to enable.           |
| `aws-secrets`    | `AWS_REGION`, `AWS_SECRETS_NAMES` (comma-separated JSON secret names) | Uses IAM role on EC2/ECS/EKS. Each secret is a JSON object of `KEY:VALUE`.                        |
| `vault`          | `VAULT_ADDR`, `VAULT_TOKEN`, `VAULT_SECRET_PATH` (KV v2)              | Path like `secret/data/crm/prod`.                                                                 |
| `sops-file`      | mount `/run/secrets/secrets.enc.env`                                  | Decrypted in-place with `sops`. Best for git-ops workflows with age/PGP. Add `sops` to the image. |

Recommended for small teams: **SOPS + age**. Encrypted file lives in your
repo, decrypted in-container at start, no extra infrastructure.

```bash
brew install sops age
age-keygen -o ~/.config/sops/age/keys.txt
sops -e --age "$(grep public ~/.config/sops/age/keys.txt | cut -d' ' -f4)" secrets.env > secrets.enc.env
# Commit secrets.enc.env. Never commit secrets.env.
```

In compose:

```yaml
services:
  app:
    environment:
      SECRETS_PROVIDER: sops-file
    secrets:
      - source: secrets-enc
        target: secrets.enc.env
secrets:
  secrets-enc:
    file: ./secrets.enc.env
```

## 9. Uptime monitoring

The app exposes Spring Boot Actuator endpoints (no auth required, safe to ping
from outside):

| URL                                                 | What to monitor for                          |
| --------------------------------------------------- | -------------------------------------------- |
| `https://crm.example.com/actuator/health`           | Overall — alert if not 200 / `"status":"UP"` |
| `https://crm.example.com/actuator/health/db`        | Postgres connectivity                        |
| `https://crm.example.com/actuator/health/redis`     | Redis connectivity                           |
| `https://crm.example.com/actuator/health/liveness`  | App alive (used by k8s liveness probe)       |
| `https://crm.example.com/actuator/health/readiness` | App ready to serve traffic                   |

### Quick setup with BetterStack (free tier: 10 monitors, 30s checks)

1. Create an account at https://betterstack.com/uptime
2. **New monitor → HTTPS** → URL `https://crm.example.com/actuator/health`
3. Required string to find: `"status":"UP"`
4. Check interval: 60 seconds
5. Add an Alerts channel (email / Slack / SMS).
6. Repeat for `/actuator/health/db` and `/actuator/health/redis` if you want
   per-dependency alerts.

### Alternative: UptimeRobot

1. **+ New Monitor → HTTP(s) keyword**
2. URL `https://crm.example.com/actuator/health`
3. Keyword: `UP`, "alert when keyword **not exists**"
4. Monitoring interval: 5 minutes (free tier)

### What "DOWN" means

- 503 from `/actuator/health/db` → Postgres unreachable. Check `docker compose logs postgres`.
- 503 from `/actuator/health/redis` → Redis unreachable. Rate limit and login lockout will fail-open; the app still works but anti-abuse is degraded.
- Total connection failure → either Nginx or the `app` container is down.
  Check `docker compose ps` and `docker compose logs nginx app`.
