# 02 — Functional Requirements

## 1. Player roster
The app shall store:
- Stable player ID.
- Display name.
- Primary position: Keeper, Defender, Midfielder or Striker.
- Active/inactive status.

Admin can add, edit, deactivate and reactivate players. Names are not unique identifiers. The app should flag likely duplicate records.

## 2. Match and attendance
Admin can create a match/session and select actual attendees from the roster. The screen provides name search, position filters and a live selected-player count. Admin can add late arrivals or remove unavailable players.

Only selected attendees enter matchmaking; the app must not automatically include the entire roster.

## 3. Team formats
Initial formats and target positional templates:

| Format | Keeper | Defender | Midfielder | Striker | Total |
|---|---:|---:|---:|---:|---:|
| 6-a-side | 1 | 2 | 2 | 1 | 6 |
| 7-a-side | 1 | 3 | 2 | 1 | 7 |
| 9-a-side | 1 | 3 | 3 | 2 | 9 |

Templates are targets, not rigid constraints. The engine must handle shortages and surpluses.

## 4. Team generation
Admin taps **Generate Teams**. The app:
- Determines the number of full teams from attendee count and selected format.
- Assigns each selected attendee exactly once, or explicitly lists them as substitutes/unassigned.
- Tries to provide one natural Keeper per team.
- Flags teams without a natural Keeper when there are insufficient Keepers.
- Distributes positions as evenly as practical.
- Explains shortages and leftover players.

## 5. Review and manual editing
Admin can:
- View teams and their players grouped by position.
- Move a player between teams.
- Swap two players.
- Mark a player as substitute/unassigned.
- Add a late arrival.
- Regenerate teams.
- Confirm/finalise the allocation.

Regeneration must warn that existing manual changes may be replaced. A later version may support locked assignments.

## 6. WhatsApp export
The final review screen shall include **Copy WhatsApp Message**. It copies a plain-text, WhatsApp-formatted distribution based on the latest team state, including manual edits and substitutes. Show a confirmation after copying.

Native Android share-sheet integration is optional; clipboard copy is required for MVP.

## 7. Later phases
- Player accounts and self-service availability.
- Match announcements and FCM notifications.
- Admin UPI QR and paid/unpaid tracking with manual verification.
- Match history and multiple admins.

## 8. Acceptance criteria
1. Admin can maintain the roster.
2. Admin can select attendees and choose 6-, 7- or 9-a-side.
3. Every selected attendee is assigned once or explicitly listed as a substitute.
4. Position distribution is balanced as far as the roster permits.
5. Admin can edit assignments.
6. WhatsApp copy matches the final displayed allocation.
7. Empty, small and non-divisible attendance cases do not crash the app.
