# 05 — Technical Architecture

## Proposed stack

| Layer | Technology |
|---|---|
| Android | Kotlin + Jetpack Compose |
| Backend | Go |
| Database | PostgreSQL |
| API | REST over HTTPS |
| Deployment | Docker Compose on Linux VM |
| Reverse proxy/TLS | Caddy |
| Push notifications (later) | Firebase Cloud Messaging |

## Architecture
Android app → HTTPS → Go API → PostgreSQL.

Keep the team-generation engine in a standalone Go package with unit tests. Initially, run generation in the backend so all clients use the same rules.

## Backend modules
- `players`: roster CRUD.
- `matches`: match/session lifecycle.
- `attendance`: selected attendees.
- `teams`: generation, edits and finalisation.
- `exports`: WhatsApp text generation.
- `admin`: authentication and authorisation.

## Initial data model
**Player:** id, display_name, primary_position, is_active, timestamps.

**Match:** id, match_date_time (nullable), format, status, created_by, timestamps.

**Match attendee:** match_id, player_id, attendance_status.

**Team:** id, match_id, team_number/name.

**Team assignment:** team_id, player_id, assigned_position, is_manual_override.

Represent substitutes as a match-level assignment state or separate table. Enforce that a player cannot be both assigned and substitute in one match.

## Example API
- `GET /api/v1/players`
- `POST /api/v1/players`
- `PATCH /api/v1/players/{id}`
- `POST /api/v1/matches`
- `PUT /api/v1/matches/{id}/attendees`
- `POST /api/v1/matches/{id}/generate-teams`
- `PATCH /api/v1/matches/{id}/assignments`
- `POST /api/v1/matches/{id}/finalise`
- `GET /api/v1/matches/{id}/whatsapp-export`
- `GET /health`

## Hosting and reliability
The workload is small; compute requirements are modest. Availability at match time matters more than raw performance.

A ₹0/month pilot may use an eligible free VM or service, but free providers can change limits, sleep, pause or reclaim idle instances. No free option should be treated as guaranteed production uptime. Test match-day availability, keep off-server backups and retain a migration path to a low-cost VPS.

## Security basics
- HTTPS only.
- Keep secrets out of source control.
- Do not expose PostgreSQL publicly.
- Restrict SSH access.
- Authenticate and authorise admin actions.
- Validate player IDs and assignments server-side.
- Back up the database off the VM and test restoration.

## Keep it simple
One Android app, one Go API and one PostgreSQL database are sufficient. Kubernetes, microservices and event streaming are unnecessary for the initial community scale.
