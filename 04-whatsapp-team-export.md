# 04 — WhatsApp Team Export

## Goal
The organiser should be able to copy the final team distribution and paste it directly into the F4F WhatsApp group.

## UI entry point
Show a prominent **Copy WhatsApp Message** action on the final team review screen.

## Suggested output

⚽ *F4F — MATCH DAY TEAM DISTRIBUTION*

*TEAM 1*
🧤 Keeper: Player Name
🛡️ Defenders: Player Name, Player Name
⚙️ Midfielders: Player Name, Player Name
⚡ Strikers: Player Name

*TEAM 2*
...

*SUBSTITUTES*
- Player Name

Let's have a great game! ⚽🔥

## Formatting requirements
- Use WhatsApp-compatible `*bold*` markers for headings.
- Put clear blank lines between teams.
- Group players by position.
- Omit empty position lines, or show a dash if a fixed layout is preferred.
- Preserve player display names.
- Include substitutes/unassigned attendees when present.
- Do not include internal IDs or technical metadata.
- Include date, time or venue only if entered by the admin; never invent details.

## Copy behaviour
- Copy plain text to the Android clipboard.
- Show a confirmation such as `Team distribution copied`.
- Admin opens WhatsApp and pastes the message.
- No WhatsApp API integration or automatic group posting is required for MVP.

## Acceptance criteria
- Every assigned player appears exactly once.
- Substitutes are clearly separated.
- Manual edits made immediately before copying appear in the text.
- Re-copying after an edit produces the updated message.
- Copy works even if WhatsApp is not installed.
