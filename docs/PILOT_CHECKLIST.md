# F4F Arena — Match-Day Pilot Checklist

Use this checklist during the live match-day pilot to verify the complete end-to-end workflow under real conditions.

---

## Pre-Match (1 Day Before)
- [ ] Verify production server `/health` returns `healthy` and `database: connected`.
- [ ] Verify automated backup ran and `.sql.gz` file exists.
- [ ] Confirm all 42+ regular community players appear in the **Roster** tab with accurate primary positions (`GK`, `DEF`, `MID`, `STR`).
- [ ] Test **Admin Login** inside the Android app using the production passphrase.

## Match Day — Check-In (15–30 mins before kickoff)
- [ ] Create a new match session in the **Matches** tab (select `6-a-side`, `7-a-side`, or `9-a-side`).
- [ ] Check in arriving players using search & position filter chips.
- [ ] Use **Quick Add Guest (`+👤`)** for any walk-in/guest player not yet in the roster.
- [ ] Verify the live attendance summary strip (`Total Selected`, `Suggested Teams`, `Substitutes`, `GK Deficit`).
- [ ] Tap **Save Attendance**.

## Match Day — Team Generation & Editing
- [ ] Tap **Generate Teams ⚽** and verify position balance across all teams.
- [ ] If a team shows `Needs Emergency GK`, discuss with captains and tap-to-swap or assign a volunteer keeper.
- [ ] Test **Strict Subs** vs **Distribute All** if there are remainder players.
- [ ] Handle any late arrival via **Move** (`⋮` menu or late check-in) and verify `[Manual]` badge appears.
- [ ] Tap **Finalise Match Teams 🔒** once captains agree.

## Match Day — WhatsApp Distribution
- [ ] Tap **Export to WhatsApp 📲** on the Team Board.
- [ ] Verify the preview reflects the exact latest swaps/moves and substitutes bench.
- [ ] Tap **Copy** (or **Share**) and paste directly into the F4F community WhatsApp group.

## Post-Pilot Review
- [ ] Collect feedback from 2–3 admins on tap-to-swap speed, readability in sunlight, and WhatsApp message formatting.
- [ ] Log any UX tweaks or feature requests for post-MVP phases (player self-check-in, UPI payment tracking, match history).
