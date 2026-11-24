# F4F Application — Phase 0 Detailed Specification & Execution Checklist

## 1. Phase 0 Goal & Scope

The objective of **Phase 0** is to establish a rock-solid, production-conscious, yet beginner-friendly project foundation. At the end of Phase 0, both the Go backend and the Android client will compile, run locally, communicate over HTTP, and pass automated sanity tests.

---

## 2. Directory & Git Repository Structure

```
f4f-app/
├── .gitignore                      # Git ignore rules for Go, Android, Docker, and IDEs
├── README.md                       # Comprehensive onboarding & run guide
├── Makefile                        # Single entry point for developer commands
├── docs/                           # Architecture, PRDs, and technical specs
│   ├── 01-product-overview.md      # (PRD Pack preserved)
│   ├── ...
│   ├── PROJECT_ANALYSIS.md
│   ├── SYSTEM_ARCHITECTURE.md
│   ├── DEVELOPMENT_ROADMAP.md
│   ├── DATABASE_DESIGN.md
│   ├── API_SPECIFICATION.md
│   ├── TESTING_STRATEGY.md
│   ├── OPEN_QUESTIONS.md
│   └── PHASE_0_SPECIFICATION.md
│
├── deployments/                    # Container orchestration
│   ├── docker-compose.yml          # PostgreSQL 16 + Adminer (optional web DB viewer)
│   └── .env.example                # Sample environment configuration
│
├── backend/                        # Go API Service
│   ├── cmd/
│   │   └── api/
│   │       └── main.go             # Server entry point & graceful shutdown
│   ├── internal/
│   │   ├── config/                 # Environment variables loader
│   │   │   └── config.go
│   │   ├── handler/                # HTTP handlers (Chi router)
│   │   │   ├── health.go           # /health endpoint
│   │   │   └── health_test.go      # Automated handler unit test
│   │   └── database/               # PostgreSQL connection pool & health check
│   │       └── postgres.go
│   ├── migrations/                 # Versioned SQL migrations
│   │   ├── 000001_initial_schema.up.sql
│   │   └── 000001_initial_schema.down.sql
│   ├── go.mod
│   ├── go.sum
│   └── Dockerfile                  # Multi-stage lightweight Go build
│
└── android/                        # Android Client Application
    ├── build.gradle.kts            # Root Gradle configuration
    ├── settings.gradle.kts         # Module settings
    ├── gradle/
    │   └── wrapper/                # Gradle wrapper binaries & properties
    └── app/
        ├── build.gradle.kts        # App dependencies (Compose, Retrofit, Coroutines)
        ├── proguard-rules.pro
        └── src/
            ├── main/
            │   ├── AndroidManifest.xml
            │   ├── java/com/f4f/app/
            │   │   ├── F4FApplication.kt
            │   │   ├── MainActivity.kt        # Jetpack Compose entry point
            │   │   ├── data/
            │   │   │   └── api/
            │   │   │       ├── HealthApi.kt   # Retrofit interface for /health
            │   │   │       └── NetworkClient.kt
            │   │   └── ui/
            │   │       ├── theme/             # Material 3 colors, shapes, typography
            │   │       └── HealthScreen.kt    # Visual connection indicator screen
            │   └── res/
            └── test/                          # Android unit test harness
```

---

## 3. Step-by-Step Task Breakdown for Phase 0

### Task 0.1: Git Repository Initialisation & Gitignore
- **What:** Initialize Git repository and add a comprehensive `.gitignore` preventing commit of compiled Go binaries, Android `.gradle`/`build` outputs, local `.env` secrets, and IDE artifacts.
- **Files to create:** `.gitignore`
- **How to run:** `git init && git add .gitignore docs/ README.md && git commit -m "chore: project foundation and documentation"`
- **Verification:** Run `git status` to verify no temporary, cached, or binary files are tracked.

---

### Task 0.2: Local Infrastructure Setup (Docker Compose & PostgreSQL)
- **What:** Configure Docker Compose to run PostgreSQL 16 on alpine with health checks and persistent volume storage.
- **Files to create:**
  - `deployments/docker-compose.yml`
  - `deployments/.env.example`
- **Configuration Details:**
  - DB Name: `f4f_db`
  - User: `f4f_user`
  - Password: `f4f_dev_password`
  - Port: `5432`
- **How to run:** `docker compose -f deployments/docker-compose.yml up -d`
- **Verification:** Run `docker ps` and check health status:
  `docker compose -f deployments/docker-compose.yml exec postgres pg_isready -U f4f_user` returns "accepting connections".

---

### Task 0.3: Database Migrations Setup
- **What:** Create the initial base schema migration using standard raw SQL compatible with `golang-migrate`.
- **Files to create:**
  - `backend/migrations/000001_initial_schema.up.sql`
  - `backend/migrations/000001_initial_schema.down.sql`
- **Contents:** Enums (`player_position`, `match_format`, `match_status`, `substitute_policy`) and tables (`players`, `matches`, `match_attendees`, `teams`, `team_assignments`).
- **How to run:** `make migrate-up` or run via Go migration runner.
- **Verification:** Connect to PostgreSQL and query `\dt` in `psql` to verify all 5 tables and 4 enum types exist.

---

### Task 0.4: Go Backend Initialisation & Health Endpoint
- **What:** Initialize the Go module (`f4f/backend`), configure Chi router, setup `jackc/pgx/v5` connection pool, and expose `GET /health` returning DB connectivity and API version.
- **Files to create:**
  - `backend/go.mod`
  - `backend/cmd/api/main.go`
  - `backend/internal/config/config.go`
  - `backend/internal/database/postgres.go`
  - `backend/internal/handler/health.go`
  - `backend/internal/handler/health_test.go`
  - `backend/Dockerfile`
  - `Makefile`
- **How to run:** `cd backend && go run cmd/api/main.go`
- **Verification:** Run `curl -i http://localhost:8080/health`. Expect HTTP 200 with:
  ```json
  {"status":"healthy","database":"connected","version":"1.0.0"}
  ```
- **Automated Test:** Run `go test -v ./...` in `backend/` to verify the handler test passes.

---

### Task 0.5: Android Client Scaffold (Kotlin + Jetpack Compose + Material 3)
- **What:** Configure a modern Android project with Gradle Kotlin DSL (`build.gradle.kts`), Jetpack Compose BOM, Material 3, and Retrofit/OkHttp for network connectivity.
- **Files to create:**
  - `android/settings.gradle.kts`
  - `android/build.gradle.kts`
  - `android/app/build.gradle.kts`
  - `android/app/src/main/AndroidManifest.xml`
  - `android/app/src/main/java/com/f4f/arena/MainActivity.kt`
  - `android/app/src/main/java/com/f4f/arena/F4FArenaApplication.kt`
  - `android/app/src/main/java/com/f4f/arena/data/api/HealthApi.kt`
  - `android/app/src/main/java/com/f4f/arena/data/api/AuthApi.kt`
  - `android/app/src/main/java/com/f4f/arena/data/api/NetworkClient.kt`
  - `android/app/src/main/java/com/f4f/arena/ui/HealthScreen.kt`
  - `android/app/src/main/java/com/f4f/arena/ui/theme/*`
- **Key Architecture Decision (Base URL configuration):**
  Android emulator uses `10.0.2.2` to refer to host `localhost`. A physical device connects via LAN IP (e.g. `192.168.1.X`). We inject this via `BuildConfig.API_BASE_URL` configurable in `local.properties`.
- **How to run:**
  - In Android Studio: Open `/android` folder, sync Gradle, and run on emulator/device.
  - Command line: `./gradlew assembleDebug`
- **Verification:** Launching the app displays the `HealthScreen`, performs a `GET /health` to the Go backend, and displays a green indicator: `"Go Backend Online (DB: connected)"`.

---

## 4. Phase 0 Acceptance Checklist

- [x] **Git & Monorepo:** Repository initialized, `.gitignore` active, remote set to `https://github.com/williskipsjr/F4F-Arena.git`.
- [x] **Docker:** PostgreSQL 16 container starts up cleanly with `docker compose -f deployments/docker-compose.yml up -d` and is healthy.
- [x] **Database Migrations:** Explicit migration tool (`cmd/migrate/main.go`) executed; all 6 tables verified in PostgreSQL (`match_attendees`, `matches`, `players`, `schema_migrations`, `team_assignments`, `teams`).
- [x] **Go API:** Compiles cleanly with standard Go toolchain (`go build ./...`); all 7 automated unit tests pass (`go test -v ./...`).
- [x] **Authentication:** Implemented server-side credentials and short-lived JWT token mechanism (`POST /api/v1/auth/login`, `GET /api/v1/auth/verify`). No hardcoded secrets in Android APK.
- [x] **Health Endpoint:** `curl http://localhost:8080/health` returns `200 OK` with JSON payload confirming active database connection.
- [x] **Android App Scaffold:** Configured package `com.f4f.arena` with Jetpack Compose, Material 3, dynamic theming, Retrofit/OkHttp, and configurable `local.properties.example` for emulator and physical device testing.
- [x] **Developer Experience:** Clean `Makefile` supporting `make up`, `make migrate-up`, `make run-backend`, `make test-backend`, and `make build-backend`.

---

## 5. Phase 0 Implementation Log & Verification Evidence

### Go Automated Tests
```
=== RUN   TestTokenGenerationAndValidation
--- PASS: TestTokenGenerationAndValidation (0.00s)
=== RUN   TestTokenExpiry
--- PASS: TestTokenExpiry (0.00s)
=== RUN   TestAuthMiddleware
--- PASS: TestAuthMiddleware (0.00s)
PASS ok github.com/williskipsjr/f4f-arena/backend/internal/auth 0.007s

=== RUN   TestAuthHandler_LoginSuccess
--- PASS: TestAuthHandler_LoginSuccess (0.00s)
=== RUN   TestAuthHandler_LoginInvalidCredentials
--- PASS: TestAuthHandler_LoginInvalidCredentials (0.00s)
=== RUN   TestHealthHandler_Healthy
--- PASS: TestHealthHandler_Healthy (0.00s)
=== RUN   TestHealthHandler_DBDisconnected
--- PASS: TestHealthHandler_DBDisconnected (0.00s)
PASS ok github.com/williskipsjr/f4f-arena/backend/internal/handler 0.007s
```

### PostgreSQL Migration Verification (`\dt`)
```
               List of relations
 Schema |       Name        | Type  |   Owner   
--------+-------------------+-------+-----------
 public | match_attendees   | table | f4f_admin
 public | matches           | table | f4f_admin
 public | players           | table | f4f_admin
 public | schema_migrations | table | f4f_admin
 public | team_assignments  | table | f4f_admin
 public | teams             | table | f4f_admin
(6 rows)
```

### Live Health Endpoint Verification (`GET /health`)
```http
HTTP/1.1 200 OK
Content-Type: application/json
Vary: Origin
Date: Tue, 24 Nov 2026 05:18:03 GMT
Content-Length: 133

{"status":"healthy","version":"1.0.0","service":"f4f-arena-api","database":"connected","timestamp":"2026-11-24T05:18:03.231601851Z"}
```

### Live Auth Login & Token Verification
```bash
# Valid Login
curl -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"f4f_arena_secret_passphrase"}'
# Response:
{"token":"eyJhbGciOiJI...","token_type":"Bearer","expires_at":"2026-11-25T05:16:13.577Z"}

# Token Verification on Protected Endpoint
curl http://localhost:8080/api/v1/auth/verify -H "Authorization: Bearer <token>"
# Response:
{"status":"authenticated","user":"admin","role":"ADMIN"}
```
