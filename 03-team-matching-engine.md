# 03 — Team Matching Engine

## Purpose
Generate position-aware, reasonably balanced teams quickly and predictably. Position balancing is not the same as skill balancing.

## Inputs
- Selected attendees with stable player IDs.
- Primary position: Keeper, Defender, Midfielder or Striker.
- Format: 6-, 7- or 9-a-side.
- Future optional inputs: secondary position, skill rating, locked assignments.

## Default positional templates

| Format | Keeper | Defender | Midfielder | Striker |
|---|---:|---:|---:|---:|
| 6-a-side | 1 | 2 | 2 | 1 |
| 7-a-side | 1 | 3 | 2 | 1 |
| 9-a-side | 1 | 3 | 3 | 2 |

## Rules
1. A player can be assigned to at most one team.
2. Every selected attendee must be assigned or explicitly listed as substitute/unassigned.
3. Allocate natural Keepers first, aiming for one per team.
4. Spread each position across teams as evenly as possible.
5. If there are fewer Keepers than teams, flag the shortage and allow an emergency Keeper assignment (typically a Defender) by the admin.
6. Position shortages/surpluses must be visible.
7. Admin can override every generated assignment.
8. Do not claim equal team strength without reliable skill data.

## Team count and leftovers
Team size and number of teams are separate concepts.

Recommended default: `team_count = floor(attendee_count / players_per_team)`. Remainder players are shown as substitutes. Offer an explicit alternative to distribute everyone into uneven team sizes.

Example: 30 attendees at 7-a-side:
- Default: four full teams (28 players) and two substitutes.
- Optional distribute-all: four teams of 8, 8, 7 and 7.

Never silently create uneven team sizes.

## Suggested first algorithm
1. Validate unique attendee IDs and positions.
2. Calculate team count and target sizes.
3. Create empty teams and substitute list.
4. Allocate Keepers first.
5. Allocate Defenders, Midfielders and Strikers to teams with the largest relative deficit for that position.
6. Respect team-size policy.
7. Use a tie-breaker to avoid always assigning players in the same order.
8. Validate no duplicates, no silent omissions and correct team sizes.
9. Return teams, substitutes and warnings.

## Edge cases
- No attendees.
- Fewer attendees than one team.
- No natural Keepers.
- Fewer Keepers than teams.
- All attendees have the same position.
- Uneven attendance remainder.
- Duplicate IDs in input.
- Late arrivals after generation.

## Later enhancements
Secondary positions, skill-based balancing, captain assignment, locked players and custom positional templates.
