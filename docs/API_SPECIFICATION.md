# F4F Application — REST API Specification

## 1. API Conventions & Standards

- **Base URL:** `/api/v1`
- **Protocol:** HTTPS only
- **Content Type:** `application/json; charset=utf-8`
- **Date/Time Format:** ISO 8601 UTC (`YYYY-MM-DDTHH:MM:SSZ`)
- **ID Representation:** String UUIDv4 (`550e8400-e29b-41d4-a716-446655440000`)
- **Authentication (MVP):** Simple Admin API Key / Bearer token via `X-Admin-Key` or `Authorization: Bearer <token>` header on mutating endpoints. Public reads can be relaxed or similarly protected.

---

## 2. Standard Error Response Structure

All error responses adhere to a consistent structured envelope based on RFC 7807 principles:

```json
{
  "error": {
    "code": "RESOURCE_NOT_FOUND",
    "message": "Player with ID '...' not found",
    "details": [
      {
        "field": "player_id",
        "issue": "Invalid UUID format"
      }
    ]
  }
}
```

### Standard HTTP Status Codes
- `200 OK`: Request succeeded.
- `201 Created`: Resource successfully created.
- `400 Bad Request`: Validation failure, invalid payload, or rule violation.
- `401 Unauthorized`: Missing or invalid admin credentials.
- `404 Not Found`: Target entity does not exist.
- `409 Conflict`: Business constraint violation (e.g., assigning a player already on another team).
- `500 Internal Server Error`: Unexpected server fault.

---

## 3. Endpoints Breakdown

### 3.1 Health & Diagnostics
#### `GET /health`
Returns system status and database connectivity.
- **Response (200 OK):**
```json
{
  "status": "healthy",
  "version": "1.0.0",
  "database": "connected",
  "timestamp": "2026-11-23T18:00:00Z"
}
```

---

### 3.2 Player Roster Endpoints

#### `GET /api/v1/players`
Lists all players with optional filtering.
- **Query Parameters:**
  - `active` (boolean, optional): Filter by `true` or `false`.
  - `position` (string, optional): `KEEPER`, `DEFENDER`, `MIDFIELDER`, `STRIKER`.
  - `query` (string, optional): Search by name prefix/substring.
- **Response (200 OK):**
```json
{
  "data": [
    {
      "id": "f47ac10b-58cc-4372-a567-0e02b2c3d479",
      "display_name": "Lenpu",
      "primary_position": "KEEPER",
      "is_active": true,
      "notes": null,
      "created_at": "2026-11-23T10:00:00Z"
    }
  ],
  "total": 42
}
```

#### `POST /api/v1/players`
Creates a new player record.
- **Request Body:**
```json
{
  "display_name": "David",
  "primary_position": "DEFENDER",
  "is_active": true,
  "notes": "Prefers left back"
}
```
- **Response (201 Created):** Returns created player object.

#### `PATCH /api/v1/players/{id}`
Updates player details or toggles active status.
- **Request Body:**
```json
{
  "display_name": "David Haokip",
  "primary_position": "MIDFIELDER",
  "is_active": false
}
```
- **Response (200 OK):** Returns updated player object.

---

### 3.3 Match & Attendance Endpoints

#### `POST /api/v1/matches`
Creates a new match session.
- **Request Body:**
```json
{
  "title": "Saturday Morning Session",
  "match_date": "2026-11-28T06:30:00Z",
  "venue": "Turf Ground A",
  "format": "SEVEN_A_SIDE",
  "sub_policy": "STRICT_SUBSTITUTES"
}
```
- **Response (201 Created):** Returns created match metadata with status `DRAFT`.

#### `GET /api/v1/matches/{id}`
Retrieves match details including attendee counts and current lifecycle status.

#### `PUT /api/v1/matches/{id}/attendees`
Batch sets or updates the attending players for this match.
- **Request Body:**
```json
{
  "player_ids": [
    "f47ac10b-58cc-4372-a567-0e02b2c3d479",
    "c9bf9e57-1685-4c89-bafb-ff5af830be8a"
  ]
}
```
- **Response (200 OK):**
```json
{
  "match_id": "...",
  "attendee_count": 28,
  "position_breakdown": {
    "KEEPER": 4,
    "DEFENDER": 12,
    "MIDFIELDER": 8,
    "STRIKER": 4
  }
}
```

#### `POST /api/v1/matches/{id}/attendees/late`
Adds a late arrival to an ongoing match session.
- **Request Body:**
```json
{
  "player_id": "..."
}
```
- **Response (200 OK):** Player added as an unassigned attendee / substitute.

---

### 3.4 Matchmaking & Team Allocation Endpoints

#### `POST /api/v1/matches/{id}/generate-teams`
Runs the matchmaking algorithm against the selected attendees and persists teams.
- **Request Body (optional overrides):**
```json
{
  "sub_policy": "STRICT_SUBSTITUTES"
}
```
- **Response (200 OK):**
```json
{
  "match_id": "...",
  "format": "SEVEN_A_SIDE",
  "team_count": 4,
  "teams": [
    {
      "id": "team-uuid-1",
      "team_number": 1,
      "team_name": "Team 1",
      "bib_color": "Orange",
      "players": [
        {
          "id": "player-1",
          "display_name": "Lenpu",
          "assigned_position": "KEEPER",
          "is_manual_override": false
        },
        {
          "id": "player-2",
          "display_name": "Thonglam",
          "assigned_position": "DEFENDER",
          "is_manual_override": false
        }
      ]
    }
  ],
  "substitutes": [
    {
      "id": "player-29",
      "display_name": "Jack",
      "assigned_position": "MIDFIELDER"
    }
  ],
  "warnings": [
    "Team 4 has no natural Keeper. Emergency assignment recommended."
  ]
}
```

#### `GET /api/v1/matches/{id}/teams`
Returns current team allocations, substitutes, and manual override flags.

#### `POST /api/v1/matches/{id}/assignments/swap`
Swaps two assigned players between teams (or between a team and substitutes).
- **Request Body:**
```json
{
  "player_a_id": "uuid-player-1",
  "player_b_id": "uuid-player-2"
}
```
- **Response (200 OK):** Updated teams payload.

#### `POST /api/v1/matches/{id}/assignments/move`
Moves a single player to a different team or demotes/promotes to substitutes.
- **Request Body:**
```json
{
  "player_id": "uuid-player-1",
  "target_team_id": "team-uuid-2",
  "is_substitute": false
}
```
- **Response (200 OK):** Updated teams payload.

#### `POST /api/v1/matches/{id}/finalise`
Locks the match allocations, marks status as `FINALISED`.

---

### 3.5 Export Endpoints

#### `GET /api/v1/matches/{id}/whatsapp-export`
Returns the WhatsApp-formatted plain-text message reflecting the latest allocation state.
- **Response (200 OK):**
```json
{
  "match_id": "...",
  "formatted_text": "⚽ *F4F — MATCH DAY TEAM DISTRIBUTION*\n\n*TEAM 1 (Orange)*\n🧤 Keeper: Lenpu\n🛡️ Defenders: Thonglam, Robert, Bob\n⚙️ Midfielders: Eric, Joshua\n⚡ Strikers: Joseph\n\n*TEAM 2 (Green)*\n...\n\n*SUBSTITUTES*\n- Jack\n\nLet's have a great game! ⚽🔥"
}
```
*(Note: Android client can also format locally directly from UI state, ensuring 100% immediate offline availability!)*
