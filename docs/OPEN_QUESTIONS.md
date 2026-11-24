# F4F Application — Open Questions, Decision Log & Technical Risks

## 1. Decisions Requiring User Input & Clarification

Before starting Phase 0 implementation, the following architectural and product decisions should be reviewed and agreed upon:

### Decision 1: Emergency Keeper Allocation Strategy
- **Current PRD Guidance:** *"If there are fewer Keepers than teams, flag the shortage and allow an emergency Keeper assignment (typically a Defender) by the admin."*
- **The Decision Needed:** When running `Generate Teams` with 4 teams but only 2 natural Keepers:
  - **Option A (Strict - Recommended):** The engine assigns the 2 Keepers to Teams 1 and 2, leaves Keeper empty on Teams 3 and 4 (`🧤 Keeper: [Needs Emergency GK]`), and flags a warning. The admin manually assigns a player to goal.
  - **Option B (Automated):** The engine automatically picks the most suitable Defender to play as Keeper on Teams 3 and 4, flagging them as `(Emergency GK)`.
- **Recommendation:** **Option A.** In veteran football, outfield players strongly resist being forced into goal by an algorithm. The organiser knows who is willing to rotate in goal.

---

### Decision 2: Default Remainder Policy (Leftovers)
- **Current PRD Guidance:** PRD `03` recommends `floor(attendees / team_size)` with leftovers as substitutes, while offering an optional distribute-all policy.
- **The Decision Needed:** What should be the default toggle when 30 players attend a 7-a-side match?
  - **Option A (Substitutes Default - Recommended):** 4 teams of 7 players + 2 substitutes.
  - **Option B (Distribute-All Default):** 4 teams of 8, 8, 7, 7 with 0 substitutes.
- **Recommendation:** **Option A.** 7-a-side turfs are usually physically constrained. Extra players crowd the pitch. Having 2 substitutes on a rotating bench is the standard turf practice.

---

### Decision 3: Handling Guest / Walk-in Players
- **The Issue:** The 42-player roster represents regular members. In real matches, guests ("friends of players") occasionally show up.
- **The Decision Needed:**
  - **Option A:** Allow quick inline creation of a "Guest Player" directly on the Attendance check-in screen (e.g. "Guest 1", Position: Striker).
  - **Option B:** Strict mode: Any player must first be formally added to the Roster before they can be checked into a match.
- **Recommendation:** **Option A.** Matches happen in a rush; requiring navigation away from attendance to add a one-off player creates friction.

---

### Decision 4: Team Naming & Bib Colors
- **The Issue:** PRD `04` uses `*TEAM 1*`, `*TEAM 2*`.
- **The Decision Needed:** Should the app provide default bib color presets (e.g. Orange, Green, Blue, White, Black) alongside team numbers?
- **Recommendation:** Yes. Displaying both (e.g. `*TEAM 1 (Bibs: Orange)*`) significantly reduces match-day confusion on pitch.

---

### Decision 5: Admin Authentication Mechanism for MVP
- **The Issue:** PRD `05` specifies admin authentication.
- **The Decision Needed:** What level of authentication is appropriate for the initial pilot?
  - **Option A (Static Secret Key / Passcode - Recommended):** A shared admin passcode stored securely in the app, sent via `X-Admin-Key` header. Minimal overhead, zero login friction on match day.
  - **Option B (Full User Accounts / JWT):** Email + password / OTP login with user sessions and token refresh.
- **Recommendation:** **Option A.** For a community of 90–100 players with 1–2 organisers in the MVP stage, a simple, robust passcode provides security without match-day login timeouts or credential expiration.

---

### Decision 6: Hosting Provider Selection for Phase 6
- **Current Requirement:** Cost-conscious infrastructure, aiming for ₹0/month initially.
- **Options:**
  1. **Oracle Cloud Free Tier (Always Free ARM Compute):** 4 OCPUs, 24GB RAM, static IP, 100% free forever. Excellent if the user has an existing account, but registration approval can be difficult.
  2. **Render / Fly.io / Koyeb Free Tiers + Neon/Supabase PostgreSQL:** True ₹0 setup, but instances sleep after 15 minutes of inactivity (causing 45-second cold starts when first opened).
  3. **Low-Cost VPS ($3.50–$4.50/month / ~₹350/mo on Hetzner or DigitalOcean):** Dedicated 2GB RAM, zero cold starts, 100% uptime, fixed IP.
- **Recommendation:** Build and test locally with Docker Compose first (Phase 0–5). Evaluate free-tier availability during Phase 6, with a clear fallback to a $4 VPS if cold starts impact match-day usability.

---

## 2. Technical Risk Matrix & Mitigation Plans

| # | Risk Description | Severity | Probability | Mitigation Strategy |
|---|---|---|---|---|
| **R1** | **Cold start delays on free hosting** during Saturday morning kickoff. | High | High | Implement client-side loading indicators and offline caching. Keep Go binary ultra-small. Plan for low-cost VPS if needed. |
| **R2** | **Intermittent mobile data connection** at the football ground. | High | Medium | Android app caches match state in memory / Room; format WhatsApp message client-side so copying never fails if connectivity drops. |
| **R3** | **Player reassignment race conditions** during rapid manual swaps. | Medium | Low | Database transactions (`SERIALIZABLE` or row-level `FOR UPDATE`) and atomic swap endpoints (`/assignments/swap`). |
| **R4** | **Unbalanced positions due to extreme attendance** (e.g. 10 Strikers, 1 Defender). | Medium | Medium | Matchmaking algorithm reports visible warnings; UI highlights positional deficits clearly so admin can adjust. |
| **R5** | **Android Gradle & SDK configuration overhead** in local environment. | Low | Medium | Standard Gradle wrapper setup; clean dependency lockfile; lean Compose UI without heavy third-party libraries. |
