# 01 — Product Overview

## Product
F4F App is an Android app for organising football sessions and distributing attendees into position-aware teams.

## Context
- Community size: approximately 90–100 registered members.
- Typical weekday attendance: approximately 25–30.
- Typical Saturday attendance: approximately 30–40.
- Initial roster supplied: 42 unique players.
- Usage is concentrated around match time, often for 20–30 minutes.

These are planning estimates, not system limits.

## Problem
The organiser needs to identify actual attendees, choose a match format, distribute players by position, handle last-minute changes and share the final teams in WhatsApp.

## Users
**Admin/organiser:** maintains roster, selects attendees, generates and edits teams, and exports the final distribution.

**Players:** initially represented as roster records. Self-service availability and accounts are later enhancements.

## Goals
- Generate valid teams in seconds.
- Distribute positions as evenly as practical.
- Never assign one player twice.
- Account for every selected attendee as assigned or substitute.
- Allow admin overrides.
- Generate a WhatsApp-ready message reflecting the final edited teams.
- Keep the first release simple and inexpensive.

## MVP non-goals
- In-app payments or automated UPI reconciliation.
- Chat, social networking, player ratings or league statistics.
- Push notifications unless the core workflow is already stable.
- Guaranteed production-grade uptime at ₹0/month.

## Success criteria
An organiser can select attendees, choose a format, generate teams, make corrections and copy a correct WhatsApp message without manually reformatting it.

## Product principle
**The algorithm recommends; the admin decides.** The organiser always has final control.
