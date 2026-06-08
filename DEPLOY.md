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
