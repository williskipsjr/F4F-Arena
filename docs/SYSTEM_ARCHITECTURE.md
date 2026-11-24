# F4F Application — System Architecture

## 1. High-Level Architecture Overview

The F4F system follows a modern, lean, three-tier client-server architecture:

```
┌────────────────────────────────────────────────────────┐
│                   Android Client                       │
│    (Kotlin, Jetpack Compose, Material 3, Clean Arch)   │
│                                                        │
│   ┌──────────────┐   ┌──────────────┐   ┌──────────┐   │
│   │ Compose UI   │◄──┤  ViewModels  │◄──┤ Local DB │   │
│   │ (Screens)    │   │  (StateFlow) │   │ (Room)   │   │
│   └──────────────┘   └──────┬───────┘   └──────────┘   │
│                             │                          │
│                      ┌──────▼───────┐                  │
│                      │ Repository   │                  │
│                      │ (Retrofit)   │                  │
│                      └──────┬───────┘                  │
└─────────────────────────────┼──────────────────────────┘
                              │ HTTPS / JSON REST
                              ▼
┌────────────────────────────────────────────────────────┐
│                   Host / Linux VM                      │
│                                                        │
│   ┌────────────────────────────────────────────────┐   │
│   │              Caddy Reverse Proxy               │   │
│   │     (Automatic TLS / Let's Encrypt, CORS)      │   │
│   └───────────────────────┬────────────────────────┘   │
│                           │ HTTP :8080 (Docker Network)│
│                           ▼                            │
│   ┌────────────────────────────────────────────────┐   │
│   │                  Go API Server                 │   │
│   │  ┌────────────┐   ┌────────────┐   ┌────────┐  │   │
│   │  │ HTTP / REST│──►│ Business   │──►│ Data   │  │   │
│   │  │ Handlers   │   │ Services   │   │ Access │  │   │
│   │  └────────────┘   └─────┬──────┘   └────┬───┘  │   │
│   │                         │               │      │   │
│   │                   ┌─────▼──────┐        │      │   │
│   │                   │Matchmaking │        │      │   │
│   │                   │Engine (pkg)│        │      │   │
│   │                   └────────────┘        │      │   │
│   └─────────────────────────────────────────┼──────┘   │
│                                             │ SQL      │
│                                             ▼          │
│   ┌────────────────────────────────────────────────┐   │
│   │            PostgreSQL 16 Database              │   │
│   │   (Relational storage, foreign keys, ACID)     │   │
│   └────────────────────────────────────────────────┘   │
└────────────────────────────────────────────────────────┘
```

---

## 2. Technology Stack Evaluation & Rationale

| Layer | Chosen Technology | Evaluation & Trade-offs |
|---|---|---|
| **Android Client** | **Kotlin + Jetpack Compose + Material 3** | **Approved.** Compose offers a reactive, declarative UI paradigm that is ideal for dynamic match-day states (checking/unchecking players, dragging/swapping teams, live badge updates). Material 3 provides accessible, modern components with dynamic theming. |
| **Backend API** | **Go (Golang 1.22+)** | **Approved.** Go offers ultra-low memory footprint (<20MB RAM for the entire server), instantaneous cold-starts (<50ms), single-binary compilation, and exceptional concurrency. Its strict typing and standard library simplicity make it an ideal educational and production tool. |
| **HTTP Routing** | **Standard Library `net/http` + `chi` router** | **Recommendation:** Use `go-chi/chi/v5`. Chi is 100% compatible with standard `net/http`, lightweight, has zero external magic, provides elegant middleware composition, and teaches idiomatic Go without the heavyweight abstractions of full frameworks like Gin or Echo. |
| **Database** | **PostgreSQL 16** | **Approved.** Relational integrity is vital: ensuring a player cannot be on two teams in the same match requires foreign keys and composite unique constraints. PostgreSQL is robust, battle-tested, and supported across every cloud provider. |
| **DB Driver / Access**| **`jackc/pgx/v5` with raw SQL or `sqlc`** | **Recommendation:** Use raw parameterized SQL with `pgx/v5` or compile-time checked SQL with `sqlc`. Avoid bulky ORMs (like GORM). In Go, explicit SQL queries provide clarity, maximum performance, and transparent learning. |
| **Reverse Proxy / TLS**| **Caddy 2** | **Approved.** Caddy handles automatic Let's Encrypt HTTPS provisioning with zero configuration, automatic HTTP->HTTPS redirects, and proxying to the Go container with minimal operational overhead. |
| **Containerisation** | **Docker & Docker Compose** | **Approved.** Encapsulates Go API, PostgreSQL, and Caddy into a single-command deployable setup (`docker compose up -d`). |

---

## 3. Component Responsibilities & Boundaries

### 3.1 Android Client Layer
- **UI Layer (Jetpack Compose):** Renders screens (`RosterScreen`, `MatchSetupScreen`, `AttendanceScreen`, `TeamBoardScreen`). Observes immutable UI state from ViewModels.
- **Presentation / ViewModel Layer:** Holds screen state (`StateFlow`), handles user intents, dispatches asynchronous coroutine requests, and handles transient UI states (loading, errors, snackbars).
- **Domain Layer:** Pure data models and business formatting (e.g. WhatsApp clipboard formatter helper).
- **Data Layer:** Repository pattern mediating between remote network calls (Retrofit/OkHttp) and optional local cache (Room).

### 3.2 Go Backend Layer
- **`cmd/api/main.go`:** Application entry point, configuration loading, database connection pooling, graceful shutdown.
- **`internal/handler/`:** HTTP transport layer. Parses JSON requests, validates input parameters, invokes services, formats JSON responses, and maps domain errors to HTTP status codes.
- **`internal/service/`:** Business logic orchestration. Handles match lifecycle transactions, player management, and coordinates with the matchmaking engine.
- **`internal/matchmaker/`:** **Standalone pure package.** Accepts a list of players with positions and format rules; returns balanced team assignments, substitutes, and shortages. Contains **zero database or HTTP dependencies**, making it 100% unit-testable.
- **`internal/repository/`:** Database persistence layer executing parameterized SQL against PostgreSQL.

---

## 4. End-to-End Match-Day Data Flow

```mermaid
sequenceDiagram
    autonumber
    actor Admin as Admin Organiser
    participant App as Android Compose App
    participant API as Go REST API
    participant Engine as Matchmaker Engine
    participant DB as PostgreSQL

    Admin->>App: Opens App & Navigates to Match Setup
    App->>API: GET /api/v1/players?active=true
    API->>DB: SELECT * FROM players WHERE is_active=true
    DB-->>API: 42 Players
    API-->>App: 200 OK [Player List]
    
    Admin->>App: Creates Match (e.g., 7-a-side)
    App->>API: POST /api/v1/matches {format: "SEVEN_A_SIDE"}
    API->>DB: INSERT INTO matches ...
    DB-->>API: match_id
    API-->>App: 201 Created {match_id}

    Admin->>App: Ticks attending players (e.g., 28 players)
    App->>API: PUT /api/v1/matches/{id}/attendees {player_ids: [...]}
    API->>DB: Bulk INSERT/REPLACE match_attendees
    DB-->>API: OK
    API-->>App: 200 OK {attendee_count: 28}

    Admin->>App: Taps "Generate Teams"
    App->>API: POST /api/v1/matches/{id}/generate-teams
    API->>DB: Fetch match attendees with positions
    DB-->>API: 28 Players (4 GK, 12 DEF, 8 MID, 4 STR)
    API->>Engine: RunMatchmaking(attendees, 7-a-side)
    Engine-->>API: 4 Teams (Balanced) + 0 Subs + Warnings
    API->>DB: Transaction: Save teams & assignments
    DB-->>API: Commit
    API-->>App: 200 OK {teams: [...], substitutes: [], warnings: []}

    Admin->>App: Inspects teams; Swaps Player A & Player B
    App->>API: POST /api/v1/matches/{id}/assignments/swap {player_a_id, player_b_id}
    API->>DB: Swap team_id in team_assignments (atomic)
    DB-->>API: Commit
    API-->>App: 200 OK {updated_teams}

    Admin->>App: Taps "Copy WhatsApp Message"
    App->>App: Formats text & copies to Android Clipboard
    Admin->>Admin: Switches to WhatsApp & pastes to group
```

---

## 5. Proposed Project Repository Structure

A clean, modular monorepo structure allows the Go backend, Android app, and documentation to live in harmony with distinct versioning and build setups:

```
f4f-app/
├── docs/                           # Architecture, PRDs, specifications
│   ├── PROJECT_ANALYSIS.md
│   ├── SYSTEM_ARCHITECTURE.md
│   ├── DEVELOPMENT_ROADMAP.md
│   ├── DATABASE_DESIGN.md
│   ├── API_SPECIFICATION.md
│   ├── TESTING_STRATEGY.md
│   └── OPEN_QUESTIONS.md
│
├── backend/                        # Go API Service
│   ├── cmd/
│   │   ├── api/
│   │   │   └── main.go             # Server entry point
│   │   └── seed/
│   │       └── main.go             # Roster seeder (42 players)
│   ├── internal/
│   │   ├── config/                 # Env & configuration parsing
│   │   ├── domain/                 # Core domain entities & interfaces
│   │   │   ├── player.go
│   │   │   ├── match.go
│   │   │   └── team.go
│   │   ├── handler/                # HTTP Handlers (Chi router)
│   │   │   ├── player_handler.go
│   │   │   ├── match_handler.go
│   │   │   ├── attendance_handler.go
│   │   │   ├── team_handler.go
│   │   │   └── export_handler.go
│   │   ├── matchmaker/             # Pure matchmaking algorithm package
│   │   │   ├── engine.go
│   │   │   ├── templates.go
│   │   │   ├── models.go
│   │   │   └── engine_test.go      # Comprehensive unit tests
│   │   ├── repository/             # Database access (pgx)
│   │   │   ├── postgres/
│   │   │   │   ├── db.go
│   │   │   │   ├── player_repo.go
│   │   │   │   ├── match_repo.go
│   │   │   │   └── team_repo.go
│   │   └── service/                # Business orchestration
│   │       ├── player_service.go
│   │       ├── match_service.go
│   │       └── team_service.go
│   ├── migrations/                 # SQL migration files (.up.sql / .down.sql)
│   │   ├── 000001_create_players_table.up.sql
│   │   ├── 000002_create_matches_table.up.sql
│   │   └── 000003_create_teams_and_assignments.up.sql
│   ├── Dockerfile
│   ├── go.mod
│   ├── go.sum
│   └── Makefile                    # Local build, test, and migration tasks
│
├── android/                        # Android Client Application
│   ├── app/
│   │   ├── src/
│   │   │   ├── main/
│   │   │   │   ├── java/com/f4f/app/
│   │   │   │   │   ├── F4FApplication.kt
│   │   │   │   │   ├── data/             # Network, DTOs, Repository Impls
│   │   │   │   │   │   ├── api/          # Retrofit interfaces
│   │   │   │   │   │   ├── model/        # Remote & Local data classes
│   │   │   │   │   │   └── repository/   # Repositories
│   │   │   │   │   ├── domain/           # Use cases, Domain models
│   │   │   │   │   │   ├── model/
│   │   │   │   │   │   └── usecase/
│   │   │   │   │   └── ui/               # Jetpack Compose UI
│   │   │   │   │       ├── theme/        # Color, Type, Shape
│   │   │   │   │       ├── components/   # Shared Compose widgets
│   │   │   │   │       ├── roster/       # Roster list & edit screen
│   │   │   │   │       ├── match/        # Match setup & attendance
│   │   │   │   │       └── teams/        # Team review, swap & export
│   │   │   │   └── res/                  # Icons, Drawables, Strings
│   │   │   └── test/                     # ViewModel & Unit tests
│   │   └── build.gradle.kts
│   ├── build.gradle.kts
│   ├── settings.gradle.kts
│   └── gradle/
│
├── deployments/                    # Deployment & Infrastructure
│   ├── docker-compose.yml          # Backend + Postgres + Caddy
│   ├── Caddyfile                   # HTTPS Reverse Proxy config
│   └── .env.example                # Sample environment variables
│
└── README.md                       # Project overview & local run guide
```
