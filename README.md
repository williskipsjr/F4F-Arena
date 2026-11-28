<p align="center">
  <img src="docs/assets/f4f-banner.jpg" alt="F4F Arena Banner" width="100%" />
</p>

<h1 align="center">F4F Arena — Match-Day Team Organisation Platform</h1>

<p align="center">
  <strong>F4F Football4Fitness Community • Position-Balanced Matchmaking & Live Polls</strong>
</p>

<p align="center">
  <img src="https://skillicons.dev/icons?i=go,kotlin,androidstudio,postgres,docker,linux" alt="Tech Stack: Go, Kotlin, Android Studio, PostgreSQL, Docker, Linux" />
</p>

F4F Arena replaces manual WhatsApp list-counting and ad-hoc team picking with a lightweight Android client backed by a Go REST API and PostgreSQL. Organisers can open match polls, track IN/OUT votes, generate position-balanced teams in under a millisecond, reshuffle or swap players on the pitch, and copy a formatted team sheet directly into WhatsApp.

---

## Core Features

- **Phone-First Onboarding & PIN Auth** — Players enter their phone number to instantly see `"Welcome, <Name>!"` from the pre-seeded community roster (or quick sign-up with Name, Phone, Age & 4-digit PIN), with phone-verified PIN reset and in-app password/PIN rotation.
- **Admin-Only Roster & Position Control** — The **Roster** tab is restricted to Admins, who manage players, assign/edit playing positions (`KEEPER`, `DEFENDER`, `MIDFIELDER`, `STRIKER`), active status, and emoji avatars.
- **Live Match Polls & Comments** — Open polls with automatic match timelines (`Starting at ...`, `ONGOING`, `Match ended ...`), `YES (IN)` / `NO (OUT)` counts, and a poll comments thread where Admin announcements display an **`ADMIN`** capsule badge.
- **All Formats + Smart Auto Even-Team Engine** — Supports `5-a-side` through `11-a-side` (`5v5`, `6v6`, `7v7`, `8v8`, `9v9`, `10v10`, `11v11`) plus an **Auto** mode that selects even team counts (`2`, `4`, `6`, ...) from attendance, while always honoring the Admin's explicit format & check-in list.
- **Football Pitch Lineup Card & Team Editing** — Toggle between a 2D **Pitch Lineup** formation card and **Squad List**, tap-to-swap or move players across teams/bench, reshuffle, finalise squads, and export to WhatsApp.
- **Post-Match Photos & Turf Fee Split** — Admins can attach a post-match squad photo (camera or gallery) and enter the total turf fee to automatically calculate and display the per-player split.
- **In-App APK Updater** — Checks `GET /api/v1/app/version` and downloads & launches the signed GitHub Releases APK update directly inside the app with a live progress bar.

---

## System Architecture

F4F Arena uses a three-tier client-server architecture designed to run on a single Linux host within a ₹0/month hosting budget.

```
┌──────────────────────────────────────────────────────────┐
│                     Android Client                       │
│         Kotlin • Jetpack Compose • Material 3            │
│   UI Screens ──► ViewModels (StateFlow) ──► Retrofit     │
└────────────────────────────┬─────────────────────────────┘
                             │ HTTPS / JSON REST
                             ▼
┌──────────────────────────────────────────────────────────┐
│                       Linux Host                         │
│                                                          │
│   ┌──────────────────────────────────────────────────┐   │
│   │                 Caddy 2 Proxy                    │   │
│   │        Automatic Let's Encrypt TLS (80/443)      │   │
│   └────────────────────────┬─────────────────────────┘   │
│                            │ HTTP :8080 (Docker network) │
│                            ▼                             │
│   ┌──────────────────────────────────────────────────┐   │
│   │                   Go API Server                  │   │
│   │   Chi Router ─► Services ─► pgx/v5 Repository    │   │
│   │                    └─► Matchmaker Engine (pure)  │   │
│   └────────────────────────┬─────────────────────────┘   │
│                            │ SQL                         │
│                            ▼                             │
│   ┌──────────────────────────────────────────────────┐   │
│   │                  PostgreSQL 16                   │   │
│   │       ACID transactions • Foreign keys • UUIDs   │   │
│   └──────────────────────────────────────────────────┘   │
└──────────────────────────────────────────────────────────┘
```

### Technology Stack

| Layer | Technology | Notes |
| :--- | :--- | :--- |
| **Android App** | Kotlin, Jetpack Compose, Material 3, Retrofit, OkHttp | Package `com.f4f.arena`; targets Android Studio + Emulator/Device |
| **Backend API** | Go 1.25, `go-chi/chi/v5`, `golang-jwt/jwt/v5` | Single static binary; clean handler → service → repository separation |
| **Database** | PostgreSQL 16, `jackc/pgx/v5` | Parameterised raw SQL (no ORM); explicit CLI migrations |
| **Infrastructure** | Docker, Docker Compose, Caddy 2 | Isolated internal network; automated `pg_dump` backups |

---

## Repository Structure

```text
F4F/
├── android/                        # Kotlin + Jetpack Compose Android app (com.f4f.arena)
│   └── app/src/
│       ├── main/java/com/f4f/arena/
│       │   ├── data/               # Retrofit APIs, models, repositories, SessionManager
│       │   ├── ui/                 # Compose screens & ViewModels (Roster, Match, TeamBoard)
│       │   └── util/               # Offline WhatsAppFormatter
│       └── test/                   # JUnit model & formatter tests
├── backend/                        # Go REST API & migration CLI
│   ├── cmd/
│   │   ├── api/main.go             # HTTP server entry point
│   │   └── migrate/main.go         # Explicit SQL migration runner
│   ├── internal/
│   │   ├── auth/                   # JWT generation & Bearer middleware
│   │   ├── config/                 # Environment configuration loader
│   │   ├── database/               # pgxpool connection manager
│   │   ├── domain/                 # Entities, enums, validation & domain errors
│   │   ├── handler/                # Chi HTTP handlers
│   │   ├── matchmaker/             # Pure position-balancing engine + 13 edge-case tests
│   │   ├── repository/postgres/    # Parameterised PostgreSQL repositories
│   │   └── service/                # Business orchestration & WhatsApp formatting
│   ├── migrations/                 # Versioned .up.sql / .down.sql files + 42-player seed
│   └── Dockerfile                  # Multi-stage production build (API + migrate binary)
├── deployments/
│   ├── docker-compose.yml          # Local PostgreSQL 16 container
│   ├── docker-compose.prod.yml     # Production stack (PostgreSQL + Go API + Caddy)
│   ├── Caddyfile                   # Automatic HTTPS reverse proxy config
│   ├── .env.example                # Local environment template
│   ├── .env.production.example     # Production environment template
│   └── scripts/                    # backup_db.sh & restore_db.sh
└── docs/                           # Architecture, API spec, DB design, deployment & pilot docs
```

---

## Local Development Setup

### Prerequisites

- **Go** 1.25+
- **Docker** & **Docker Compose**
- **Android Studio** (for building/running the Android app)

### 1. Start PostgreSQL

```bash
docker compose -f deployments/docker-compose.yml up -d
```

Wait until the `f4f_arena_postgres` container reports healthy (`docker ps`).

### 2. Run Database Migrations & Seed Roster

Migrations are explicit and never run automatically on API startup:

```bash
cd backend
go run cmd/migrate/main.go up
```

This creates the schema (`players`, `matches`, `match_attendees`, `teams`, `team_assignments`) and seeds the 42 initial players.

### 3. Start the Go API Server

```bash
cd backend
go run cmd/api/main.go
```

By default, the server listens on `http://localhost:8080`. Set `ADMIN_PASSPHRASE` (and `JWT_SECRET` in production) via environment variables or `.env` (never commit real passwords to the repository), and once signed in as admin you can change the admin password directly in-app.

Verify the server:

```bash
curl http://localhost:8080/health
```

### 4. Run Backend Tests

```bash
cd backend
go test -v -race ./...
```

*(Live PostgreSQL persistence tests in `internal/repository/postgres` automatically run when `localhost:5432` is reachable and skip otherwise.)*

### 5. Run the Android App

1. Open `android/` in **Android Studio**.
2. On an **Android Emulator**, the app automatically targets `http://10.0.2.2:8080/`.
3. On a **physical device** on the same Wi-Fi/LAN, set your machine's LAN IP in `android/local.properties` (without quotes):
   ```properties
   f4f.api.base_url=http://192.168.x.x:8080/
   ```
4. Launch the app and tap **Admin Login** in the top bar to authenticate before creating matches, checking in players, or editing teams.

---

## Production Deployment

- **Cloud (Render + Neon PostgreSQL)** — Zero-cost cloud hosting using `backend/Dockerfile` on [Render](https://render.com) (respects `PORT` automatically), serverless PostgreSQL on [Neon](https://neon.tech), and signed APK distribution via **GitHub Releases** + in-app updater (`APP_LATEST_VERSION_CODE`, `APP_LATEST_VERSION_NAME`, `APP_DOWNLOAD_URL`, `APP_RELEASE_NOTES`).
- **Self-Hosted VPS (Docker + Caddy 2)** — Uses `deployments/docker-compose.prod.yml` with Caddy 2 handling automatic HTTPS certificates and `deployments/scripts/backup_db.sh` handling daily gzipped database backups.

- Full VPS setup, migration, and backup cron instructions: [docs/DEPLOYMENT_GUIDE.md](docs/DEPLOYMENT_GUIDE.md)
- Match-day rehearsal and verification steps: [docs/PILOT_CHECKLIST.md](docs/PILOT_CHECKLIST.md)
- REST API reference: [docs/API_SPECIFICATION.md](docs/API_SPECIFICATION.md)
- Database schema: [docs/DATABASE_DESIGN.md](docs/DATABASE_DESIGN.md)
