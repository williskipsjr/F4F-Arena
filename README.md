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

- **Player Roster Management** — Maintains the community roster (seeded with 42 initial players across `KEEPER`, `DEFENDER`, `MIDFIELDER`, and `STRIKER`), active/inactive status, and quick guest additions on match day.
- **Match Setup & Live Attendance** — Supports `6-a-side` (1-2-2-1), `7-a-side` (1-3-2-1), and `9-a-side` (1-3-3-2) formats with live headcount, suggested team count, substitute count, and goalkeeper deficit indicators.
- **Position-Aware Matchmaking Engine** — Standalone pure Go package (`internal/matchmaker`) that balances natural positions across teams, distributes goalkeepers round-robin, flags missing keepers (`Needs Emergency GK`) without forcing outfielders into goal, and supports both `STRICT_SUBSTITUTES` (rotating bench) and `DISTRIBUTE_ALL` remainder policies.
- **Interactive Team Review & Editing** — Tap-to-swap players across teams or the bench, move players directly (including late arrivals), track manual overrides, and lock final teams (`FINALISED`).
- **WhatsApp Export** — Generates a plain-text WhatsApp message grouped by position (with optional bib colours, emergency keeper tags, and substitutes) from both the API and offline Android UI state, with one-tap clipboard copy and Android share sheet support.
- **Admin JWT Authentication** — Public read access for viewing rosters, matches, and teams; server-side credentials issuing short-lived Bearer JWTs for all mutations.

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

Production deployment uses `deployments/docker-compose.prod.yml` with Caddy 2 handling automatic HTTPS certificates and `deployments/scripts/backup_db.sh` handling daily gzipped database backups.

- Full VPS setup, migration, and backup cron instructions: [docs/DEPLOYMENT_GUIDE.md](docs/DEPLOYMENT_GUIDE.md)
- Match-day rehearsal and verification steps: [docs/PILOT_CHECKLIST.md](docs/PILOT_CHECKLIST.md)
- REST API reference: [docs/API_SPECIFICATION.md](docs/API_SPECIFICATION.md)
- Database schema: [docs/DATABASE_DESIGN.md](docs/DATABASE_DESIGN.md)
