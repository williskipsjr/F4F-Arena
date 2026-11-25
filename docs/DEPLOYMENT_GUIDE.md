# F4F Arena — Production Deployment Guide (₹0/month Target)

This guide covers deploying the F4F Arena Go API, PostgreSQL 16 database, and automatic HTTPS reverse proxy (Caddy) on a Linux VPS within the **₹0/month** target.

---

## 1. Recommended ₹0/month Infrastructure

| Option | Monthly Cost | Specs | Notes |
| :--- | :---: | :--- | :--- |
| **Oracle Cloud Always Free (ARM Ampere)** | ₹0 | Up to 4 OCPUs, 24 GB RAM | Best free tier for Docker + Postgres + Go |
| **Google Cloud Free Tier (`e2-micro`)** | ₹0 | 2 vCPU (shared), 1 GB RAM | Sufficient for F4F Arena (~150 MB RAM used total); add 1 GB swap |
| **Self-hosted Linux + Cloudflare Tunnel** | ₹0 | Any home/office Linux box | No public IP needed |

**Firewall Requirements:** Open inbound TCP ports `22` (SSH), `80` (HTTP ACME challenge), and `443` (HTTPS) only. Port `5432` (PostgreSQL) and `8080` (Go API) stay private inside the Docker bridge network.

---

## 2. Server Setup & Deployment Steps

1. **Install Docker & Docker Compose** on Ubuntu/Debian:
   ```bash
   curl -fsSL https://get.docker.com | sh
   sudo usermod -aG docker $USER
   ```

2. **Clone the repository and configure environment:**
   ```bash
   git clone https://github.com/williskipsjr/F4F-Arena.git
   cd F4F-Arena/deployments
   cp .env.production.example .env.production
   ```
   Edit `.env.production` and set strong random values for `POSTGRES_PASSWORD`, `ADMIN_PASSPHRASE`, `JWT_SECRET`, and your `DOMAIN_NAME`.

3. **Build and start the production stack:**
   ```bash
   docker compose --env-file .env.production -f docker-compose.prod.yml up -d --build
   ```

4. **Run explicit database migrations (never auto-run on boot):**
   ```bash
   docker compose --env-file .env.production -f docker-compose.prod.yml exec api ./f4f-arena-migrate up
   ```
   Verify all 42 initial players were seeded:
   ```bash
   curl -s https://<YOUR_DOMAIN>/api/v1/players | jq '.total'
   ```

5. **Verify HTTPS & Health:**
   ```bash
   curl -s https://<YOUR_DOMAIN>/health
   ```
   Expected response: `{"status":"healthy","database":"connected","version":"1.0.0"}`.

---

## 3. Automated Daily Backups

Make the backup scripts executable:
```bash
chmod +x deployments/scripts/backup_db.sh deployments/scripts/restore_db.sh
```

Add a daily 03:00 AM cron job (`crontab -e`):
```cron
0 3 * * * /home/ubuntu/F4F-Arena/deployments/scripts/backup_db.sh /home/ubuntu/f4f_backups >> /var/log/f4f_backup.log 2>&1
```

Backups are gzipped SQL dumps with automatic 14-day retention pruning. To restore:
```bash
./deployments/scripts/restore_db.sh /home/ubuntu/f4f_backups/f4f_arena_YYYYMMDD_HHMMSS.sql.gz
```

---

## 4. Pointing the Android App to Production

In `android/local.properties`, set (without quotes):
```properties
f4f.api.base_url=https://api.f4f-arena.example.com/
```
Build the signed release APK in Android Studio (`Build > Generate Signed Bundle / APK`) or via `./gradlew assembleRelease` (with signing properties configured in `local.properties`) and distribute to match-day admins.
