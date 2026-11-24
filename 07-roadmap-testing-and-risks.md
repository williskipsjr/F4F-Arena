# 07 — Roadmap, Testing and Risks

## Recommended build sequence

### Phase 1 — Matchmaking MVP
1. Android app shell and admin flow.
2. Player roster CRUD.
3. Match setup and attendee selection.
4. Go team-generation package and unit tests.
5. 6-, 7- and 9-a-side templates.
6. Team display and substitutes.
7. Manual move/swap and late-arrival handling.
8. WhatsApp message generation and clipboard copy.
9. Persist matches and final allocations.

Do not build payments or push notifications before this workflow is reliable.

### Phase 2 — Operational improvements
- Player accounts and self-service availability.
- Match history.
- Multiple admin roles.
- Android share sheet.
- Locked assignments and better late-arrival handling.

### Phase 3 — Payments and notifications
- Admin UPI QR display.
- Paid/unpaid tracking.
- Player declares payment; admin verifies manually.
- Match reminders and announcements through FCM.

A UPI QR code does not prove payment. Self-declaration is not verified payment. Automated reconciliation requires a suitable payment-provider integration and additional security/compliance work.

## Test cases

### Roster and attendance
- Empty roster and one-player roster.
- Duplicate player ID.
- Inactive player selected.
- Add/remove player after generation.

### Team generation
- Exact full-team attendance.
- Divisible and non-divisible attendance.
- Fewer Keepers than teams and no Keepers.
- All attendees in one position.
- Position shortage and surplus.
- Every supported format.
- Repeat generation with identical input.

### Manual editing
- Move a player.
- Swap two players.
- Mark substitute.
- Add late arrival.
- Verify no duplicate assignment.
- Regenerate after edits and verify warning.

### WhatsApp export
- Copy immediately after generation.
- Copy after move/swap.
- Copy with substitutes.
- Copy with empty position groups.
- Confirm every attendee appears exactly once across teams and substitutes.

## Key risks

| Risk | Impact | Mitigation |
|---|---|---|
| Incorrect/duplicate roster data | Wrong teams | Stable IDs, duplicate checks and admin review |
| Position balance mistaken for skill balance | Teams may feel uneven | State limitation; add trusted skill data only if useful |
| Keeper shortage | Team lacks natural Keeper | Warn admin and allow emergency assignment |
| Stale WhatsApp export | Wrong message shared | Generate from current final allocation |
| Free host sleeps/reclaims VM | Match-time outage | Pilot testing, backups and migration plan |
| Data loss | Lost roster/history | Off-server backups and restore tests |
| Scope creep | MVP delayed | Prioritise roster → attendance → teams → edits → WhatsApp |

## Release checklist
- [ ] 42 roster entries reviewed.
- [ ] Position totals are 5 / 14 / 14 / 9.
- [ ] Admin can select attendees.
- [ ] 6-, 7- and 9-a-side tested.
- [ ] No duplicate or silently omitted players.
- [ ] Manual changes persist.
- [ ] WhatsApp export reflects final state.
- [ ] Admin authentication enabled before wider rollout.
- [ ] HTTPS enabled.
- [ ] Backup and restore tested.
- [ ] Real match-day pilot completed.
