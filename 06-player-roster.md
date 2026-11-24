# 06 — Initial Player Roster

## Cleanup
The supplied list contained 43 rows. The user confirmed that the repeated `Lenpu` / `Keeper` row was a duplicate. It is included only once below.

**Total unique players: 42.**

## Position totals
| Position | Count |
|---|---:|
| Keeper | 5 |
| Defender | 14 |
| Midfielder | 14 |
| Striker | 9 |
| **Total** | **42** |

Normalised storage labels:
- `Keeper` → `KEEPER`
- `Defender` / `defender` → `DEFENDER`
- `Mid Field` → `MIDFIELDER`
- `Stricker` → `STRIKER`

Display names are retained as supplied.

## Roster

| # | Player | Primary position |
|---:|---|---|
| 1 | Lenpu | Keeper |
| 2 | Joseph | Striker |
| 3 | Savio | Striker |
| 4 | Zamsei | Midfielder |
| 5 | Thonglam | Defender |
| 6 | Robert | Defender |
| 7 | Mimin Baite | Defender |
| 8 | Lalboi | Striker |
| 9 | Eric | Midfielder |
| 10 | Seiminthang | Defender |
| 11 | Joshua | Midfielder |
| 12 | Lalchaneo | Defender |
| 13 | Mimin Touthang | Midfielder |
| 14 | Paokai | Midfielder |
| 15 | Johny | Defender |
| 16 | Paolal | Defender |
| 17 | Haopu | Striker |
| 18 | D S Thangboi | Defender |
| 19 | Mangrose | Midfielder |
| 20 | Manglal | Defender |
| 21 | Boilun | Midfielder |
| 22 | Paoneo | Midfielder |
| 23 | James Kipgen | Striker |
| 24 | TL Mang | Striker |
| 25 | Manggou | Midfielder |
| 26 | Seilen | Striker |
| 27 | Nelson | Midfielder |
| 28 | Bob | Defender |
| 29 | Seiminlen | Defender |
| 30 | Mynlun | Midfielder |
| 31 | Ngambom | Defender |
| 32 | Tonglen | Striker |
| 33 | Thangminlen | Striker |
| 34 | Thangminlun | Defender |
| 35 | Jack | Midfielder |
| 36 | Thangtinlun | Midfielder |
| 37 | James Singson | Keeper |
| 38 | Andrew | Keeper |
| 39 | Paogoulen Kilong | Keeper |
| 40 | Mang Doungel | Midfielder |
| 41 | Peter | Defender |
| 42 | Jangkholen | Keeper |

## Data notes
- Use stable internal IDs, not names, as database keys.
- The admin can correct spelling and positions.
- This is the initial 42-player roster provided, not necessarily the full community membership.
