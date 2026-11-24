# F4F App — PRD Pack

A modular product requirements pack for the F4F veteran football community Android app.

## Documents
1. `01-product-overview.md` — problem, users, goals and scope.
2. `02-functional-requirements.md` — roster, attendance, team generation and admin requirements.
3. `03-team-matching-engine.md` — position-aware balancing rules and edge cases.
4. `04-whatsapp-team-export.md` — copy/paste message format and acceptance criteria.
5. `05-technical-architecture.md` — Android, Go, PostgreSQL and deployment direction.
6. `06-player-roster.md` — corrected initial roster of 42 unique players.
7. `07-roadmap-testing-and-risks.md` — phased roadmap, tests and risks.

## Decisions captured
- Android-first: Kotlin + Jetpack Compose.
- Backend: Go; database: PostgreSQL.
- Initial roster: 42 unique players after removing the duplicate Lenpu row.
- Positions: 5 Keepers, 14 Defenders, 14 Midfielders, 9 Strikers.
- Main flow: admin selects attendees → chooses format → generates teams → manually adjusts → copies WhatsApp-ready distribution.
- Payments, push notifications and player self-service are later phases.

The 42-player list is the initial roster supplied for this exercise; it is not assumed to represent the full 90–100-member community.
