# F4F Application — Comprehensive Testing Strategy

## 1. Testing Philosophy

In football match-day operations, failure is not an abstract bug report: it means 35 players standing on the pitch shouting at the organiser because two players were placed on both Team 1 and Team 3, or because a team was sent out without a goalkeeper.

Therefore, testing focuses heavily on:
1. **Mathematical correctness and determinism of the matchmaking engine.**
2. **ACID transaction guarantees for team assignments and swaps.**
3. **Resilience under unexpected attendance counts and position distributions.**
4. **Consistency between screen state and exported WhatsApp text.**

---

## 2. Test Classification Pyramid

```
           ┌────────────────────────┐
           │   E2E Acceptance Tests │   (Manual match-day scenarios)
           ├────────────────────────┤
           │   Android UI & VM Tests│   (Turbine, Compose tests)
           ├────────────────────────┤
           │   API Integration Tests│   (pgx + testcontainers / DB)
           ├────────────────────────┤
           │   Unit Tests (Engine)  │   (Pure algorithmic edge cases)
           └────────────────────────┘
```

---

## 3. Matchmaking Engine Test Matrix (`internal/matchmaker`)

The Go matchmaking engine must be 100% unit-tested with table-driven tests against every boundary condition.

### Edge Case Scenarios

| Test Case ID | Test Scenario | Input Configuration | Expected Outcome |
|---|---|---|---|
| **TC-ENG-01** | Empty Attendees | 0 players | Returns error `ErrNoAttendees`. Does not panic. |
| **TC-ENG-02** | Insufficient for Single Team | 4 players for 7-a-side | Returns warning/error; all 4 marked as unassigned/substitutes or forms 1 partial team with warnings. |
| **TC-ENG-03** | Exact Divisibility (Standard) | 28 players for 7-a-side (4 GK, 12 DEF, 8 MID, 4 STR) | Produces 4 teams of exactly 7 players (1 GK, 3 DEF, 2 MID, 1 STR each). 0 substitutes. |
| **TC-ENG-04** | Remainder Attendees (Substitutes Policy) | 30 players for 7-a-side (`STRICT_SUBSTITUTES`) | Produces 4 teams of 7 players (28 assigned) and exactly 2 substitutes. |
| **TC-ENG-05** | Remainder Attendees (Distribute-All Policy) | 30 players for 7-a-side (`DISTRIBUTE_ALL`) | Produces 4 teams with sizes 8, 8, 7, 7. Zero substitutes. |
| **TC-ENG-06** | Zero Keepers | 14 outfield players, 0 Keepers (7v7) | Produces 2 teams; Keepers vacant; returns explicit warning `ErrKeeperShortage`. |
| **TC-ENG-07** | Keeper Shortage | 2 Keepers, 26 outfielders for 4 teams | Teams 1 & 2 receive 1 Keeper each; Teams 3 & 4 have GK unassigned; returns warning. |
| **TC-ENG-08** | Keeper Surplus | 6 Keepers, 8 outfielders for 2 teams | 2 Keepers assigned to goal; remaining 4 distributed as outfielders/substitutes with notice. |
| **TC-ENG-09** | Extreme Monoposition | 28 Strikers, 0 others | 4 teams generated with 7 Strikers each; warnings generated for missing GK/DEF/MID. |
| **TC-ENG-10** | Duplicate Input Detection | List contains duplicate player ID | Engine returns validation error `ErrDuplicatePlayerID`. |
| **TC-ENG-11** | Zero Player Omission | Any valid input list $N$ | Invariant test: $\text{count}(\text{assigned}) + \text{count}(\text{subs}) \equiv N$. |
| **TC-ENG-12** | Strict Single Assignment | Any valid input list | Invariant test: $\text{Set}(\text{assigned}) \cap \text{Set}(\text{subs}) = \emptyset$. No player in two places. |
| **TC-ENG-13** | Performance Benchmark | 50 players, 7-a-side | Execution completes in under **5 milliseconds**. |

---

## 4. API & Integration Testing (`backend/internal/...`)

Integration tests run against a real PostgreSQL instance (via local container or Docker Compose test service):

1. **Player CRUD & Constraints:**
   - Verify cannot insert duplicate display name without warning.
   - Verify cannot delete player referenced in match history (restricted foreign key).
   - Verify deactivating player excludes them from active attendance queries.
2. **Match Attendance Atomic Sync:**
   - Verify `PUT /attendees` replaces attendee list atomically.
   - Verify non-existent player ID in attendee list rejects the request.
3. **Manual Swap & Move Integrity:**
   - Test swapping Player 1 (Team A) and Player 2 (Team B).
   - Test moving Player 1 to substitutes.
   - Test race conditions: simultaneous swap requests on same player must not produce duplicate assignments.
4. **WhatsApp Export Reflection:**
   - Generate teams -> Query export -> Swap players -> Query export again. Verify text changed to match the swap.

---

## 5. Android Client Testing (`android/app/src/test/...`)

1. **ViewModel Unit Tests (using Kotlin Coroutines Test & Turbine):**
   - `RosterViewModelTest`: Loading roster, filtering by search query, position filter pills.
   - `AttendanceViewModelTest`: Toggling attendance checkbox, updating live selected counter, position breakdown calculation.
   - `TeamBoardViewModelTest`: Handling swap events, dragging player, undoing move, clipboard copy trigger.
2. **Local Formatter Unit Tests:**
   - Plain-text WhatsApp formatter matches PRD rules (bold asterisks, emoji icons, position grouping, substitute listing, omission of empty metadata).
3. **Compose UI Tests:**
   - Ensure touch targets meet 48dp accessibility standards.
   - Test orientation change retaining attendance state.

---

## 6. End-to-End Match-Day Simulation Checklist

Before deploying any production release, perform a complete simulated rehearsal:
- [ ] 1. Start application with fresh database seeded with 42 players.
- [ ] 2. Create Match: "Saturday 7-a-side Session".
- [ ] 3. Select 30 attendees (simulating 2 substitutes).
- [ ] 4. Tap "Generate Teams" (verify 4 teams generated in < 1 second).
- [ ] 5. Swap one Defender from Team 1 with a Defender from Team 2.
- [ ] 6. Move one substitute onto Team 3 (testing uneven expansion).
- [ ] 7. Tap "Copy WhatsApp Message".
- [ ] 8. Paste into a text editor / WhatsApp web:
  - Check that all 30 attendees are present.
  - Check that no player is listed twice.
  - Check that substitutions are correctly noted.
  - Check that formatting displays properly on WhatsApp mobile.
