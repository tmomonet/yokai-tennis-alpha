# Yokai Tennis — Project Specification

## One-liner
An arcade tennis game for Cafe Yokai webcomic fans featuring unlockable yokai characters, playable on Android with a GBA pixel art style.

---

## Value Proposition
Cafe Yokai fans get a fun, low-friction arcade tennis game that extends the world of the comic — free to play with base characters, with beloved yokai characters (including Patreon-exclusive ones like the Gashadokuro) unlockable via IAP or Patreon membership verification. It rewards the existing fan community rather than chasing a generic mobile audience.

---

## Personas

**Mia, 26** — Two-year Cafe Yokai fan, active Patreon supporter. Wants to play as her favorite characters from the comic. Will notice if art or names are wrong. Checks Patreon monthly.

**Jordan, 31** — Casual mobile gamer who discovered the comic through the game. Wants quick sessions, simple controls, satisfying progression. Not a Patreon member yet but open to subscribing if a character they want is locked.

---

## User Stories

### MVP
- As Mia, I want to see Cafe Yokai characters on the character select screen so the game feels like an extension of the comic.
- As Mia, I want to tap a Patreon-locked character and be shown a clear unlock flow so I know my membership gets me something.
- As Jordan, I want to pick a character and play a full match end-to-end so I can evaluate whether the game is fun.
- As Jordan, I want simple on-screen shot buttons (lob, smash, slice) in landscape mode so I can play with both thumbs.
- As Jordan, I want to see the final score after a match so I know if I won.

### Post-MVP
- As Mia, I want to log in with Patreon OAuth so my exclusive characters unlock automatically without entering a code.
- As Jordan, I want to progress through a circuit bracket so there's a goal beyond a single match.
- As Jordan, I want to purchase an IAP character from the unlock screen so I can expand my roster without a Patreon subscription.
- As Mia, I want my unlocked characters and match history to persist across sessions so I don't lose progress.
- As Mia, I want the current tournament bracket state to be saved so I can resume a tournament I started.

---

## Interaction Flows

### Flow 1: MVP End-to-End Match (Quick Match)
1. Launch app → Main Menu (Play, Circuit, Tournament, Settings)
2. Tap **Play** → Character Select screen (free characters selectable; locked chars shown with padlock)
3. Tap a free character → Confirm → Court Select screen
4. Tap a court → Confirm → Court loads
5. Match plays (arcade tennis, landscape, dual joystick controls)
6. Match ends → Score screen → Back to Main Menu

### Flow 2: Patreon Unlock
1. On Character Select, tap a Patreon-locked character
2. Unlock screen: "This character is a Patreon exclusive. Connect your Patreon account to unlock."
3. Tap **Connect Patreon** → OAuth webview opens (Patreon login)
4. On success, app receives OAuth token → checks membership tier via Patreon API
5a. If eligible tier → character unlocks, stored in Room, user returned to Character Select
5b. If ineligible tier → message shown explaining required tier

### Flow 3: Circuit Progression
1. Tap **Circuit** on Main Menu → Bracket screen showing round 1 opponent
2. Tap **Play** → Character Select → Match
3. Win → bracket advances to round 2, saved to Room
4. Lose → option to retry same round
5. Complete all rounds → Circuit Complete screen

### Flow 4: IAP Purchase
1. On Character Select, tap an IAP-locked character
2. Unlock screen: character preview + price
3. Tap **Buy** → Google Play Billing flow
4. On purchase confirmed → character unlocked, stored in Room

---

## Wireframes

### Main Menu
- Center: game logo / title
- Buttons stacked vertically: Play (Quick Match), Circuit, Tournament, Settings
- Bottom right: Patreon connect status indicator (connected / not connected)

### Character Select
- Grid of character portraits (geometric placeholder sprites for prototype)
- Free characters: fully selectable
- IAP characters: padlock icon + price label
- Patreon characters: padlock icon + Patreon logo
- Tapping a locked character opens the Unlock screen
- Bottom: Confirm / Back buttons

### In-Game Court (HUD)
- Court top-down view, landscape orientation
- Score display top center (sets / games / current game points)
- Serve indicator top left
- Left thumb area: virtual movement joystick
- Right thumb area: virtual aim/shot joystick (flick to hit; gesture determines shot type — no buttons)

### Tournament Bracket
- Single-elimination bracket tree, left to right
- Current match highlighted
- Completed matches show result (W / L)
- Tap any match to view details

### Court Select (Quick Match only)
- Grid of court thumbnails (background preview)
- Free courts: selectable
- Locked courts: padlock icon (IAP only — no Patreon courts)
- Bottom: Confirm / Back buttons

### Unlock / Shop Screen
- Character portrait (placeholder sprite)
- Name, lore blurb (one line)
- Unlock method: IAP price button OR Patreon connect button
- Back button

### Patreon OAuth Screen
- Webview: Patreon login page
- Cancel button top left
- On success: auto-closes, returns to previous screen

---

## Game Mechanics

### Serve Mechanic

**Server selection:** Coin toss at match start; alternates each game thereafter (standard tennis rules).

**Serve flow (per point):**
1. Serve indicator highlights the server. A 10-second aim window opens.
2. Player drags/taps to choose a target location within the valid service box. A visual indicator shows the aimed landing spot.
3. Player taps the Serve button to begin the **power meter** — a bar that fills upward. Player taps again at the desired power level to lock it in. Higher power = faster serve, larger fault margin.
4. Immediately after power is locked, the **accuracy meter** activates — a needle swings left/right. Player taps when the needle is centered. Deviation from center shifts the ball's landing position left or right of the aimed target.
5. Ball launches. If it lands in the service box: serve is good, rally begins. If it misses: fault.
6. **Fault rules:** Two attempts per point. Double fault (two consecutive faults) awards the point to the receiver.
7. If the 10-second aim window expires without a serve input: treated as a fault.

**AI serving:** AI skips the meter UI. Difficulty tier determines serve speed and placement accuracy (Easy: center, slow; Hard: corners, fast).

### Shot Mechanic

**Control layout:** Dual virtual joystick. Left joystick = character movement. Right joystick = shot aim and trigger. No separate shot buttons.

**Shot gestures (right joystick):**

| Gesture | Shot | Effect |
|---|---|---|
| Slow flick | Lob | High arc, lands deep near baseline. Soft pace. Difficult for AI to smash if it is caught out of position. |
| Fast/hard flick | Smash | Flat, fast, low trajectory. High power. Can go out if power is near max. |
| Flick + lateral jerk at apex | Slice / Topspin | At the peak of the flick motion, a sudden sideways jerk adds spin. Slice: jerk against swing direction (ball stays low, skids). Topspin: jerk with swing direction (ball kicks up after bounce). |

**Range requirement:** Player must be within a defined range radius of the ball to connect. Hitting outside range results in a miss (no auto-teleport to ball).

**Power modifier:** Power is scaled by distance from ball at moment of contact — closer = full power, near the range boundary = reduced power. Fast flick at close range = maximum smash power.

**AI response:** AI difficulty tier affects how well it reads spin. Easy: ignores spin, returns to center. Medium: reacts to spin with delay. Hard: correctly anticipates spin direction.

**Implementation note:** Apex detection requires tracking joystick velocity over the gesture duration. A sudden perpendicular velocity spike at peak displacement = spin trigger. Needs tuning to avoid false positives from imprecise flicks.

---

## Character Roster

Launch roster — 3 characters, all placeholder geometric sprites. No IAP characters at launch.

| ID | Name | Sprite | Unlock Type | Notes |
|---|---|---|---|---|
| tess | Tess | Red placeholder | FREE | |
| demi | Demi | Blue placeholder | FREE | |
| patreon_test | TBD | Green placeholder | PATREON | Validates Patreon unlock flow; name/lore TBD |

The `patreonTierRequired` value for `patreon_test` is a placeholder until Patreon tier names are confirmed (see Gaps).

---

## Tech Stack

| Concern | Choice |
|---|---|
| Language | Java |
| Game engine | LibGDX (core, android, desktop modules) |
| Architecture | MVVM |
| DI | Hilt |
| Local DB | Room (match history, unlocks, bracket state, win/loss record) |
| Cloud sync | None (MVP) — Firestore is a post-MVP stretch goal |
| Auth | Patreon OAuth2 (unlock verification only — no mandatory login to play) |
| IAP | Google Play Billing Library v6+ |
| Asset management | LibGDX AssetManager (all sprites referenced by key, swappable when real art arrives) |
| Orientation | Landscape only, locked |
| Min SDK | 21 (Android 5.0) |
| Target SDK | 35 (Android 15) |
| Dev iteration | Desktop module for fast local testing without emulator |

---

## Data Model

### Character
| Field | Type | Notes |
|---|---|---|
| id | String | |
| name | String | |
| spriteKey | String | AssetManager key |
| unlockType | enum | FREE, IAP, PATREON |
| iapProductId | String? | null if not IAP |
| patreonTierRequired | String? | null if not Patreon |

### Court
| Field | Type | Notes |
|---|---|---|
| id | String | |
| name | String | |
| backgroundKey | String | AssetManager key |
| unlockType | enum | FREE, IAP |

### Match
| Field | Type | Notes |
|---|---|---|
| id | UUID | |
| timestamp | long | |
| playerCharId | String | |
| opponentCharId | String | |
| courtId | String | |
| playerScore | String | e.g. "6-3, 6-4" |
| result | enum | WIN, LOSS |
| mode | enum | QUICK, CIRCUIT, TOURNAMENT |

### TournamentBracket
| Field | Type | Notes |
|---|---|---|
| id | UUID | |
| mode | enum | CIRCUIT, TOURNAMENT |
| rounds | int | |
| currentRound | int | |
| matchIds | List\<UUID\> | |
| completed | boolean | |

### UnlockRecord
| Field | Type | Notes |
|---|---|---|
| entityId | String | character or court id |
| entityType | enum | CHARACTER, COURT |
| unlockMethod | enum | FREE, IAP, PATREON |
| timestamp | long | |

### PlayerProfile
| Field | Type | Notes |
|---|---|---|
| id | int | local Room primary key |
| displayName | String | |
| patreonConnected | boolean | |
| patreonTier | String? | null if not connected |
| totalWins | int | |
| totalLosses | int | |

### Stretch Goal: Firestore Cloud Sync
Post-MVP. Would require adding Firebase Auth (anonymous uid or Google Sign-In) and mirroring Room tables to:
```
/users/{uid}/profile
/users/{uid}/unlocks/{entityId}
/users/{uid}/matches/{matchId}
/users/{uid}/brackets/{bracketId}
```

---

## Non-Goals
- Online / async PvP (local pass-and-play is a stretch goal only)
- Character stat upgrades or progression meta (pure bracket advancement)
- Cloud save cross-play with PC version
- Leaderboards

---

## Success Criteria
A demo where: app launches in landscape → main menu displays → character select shows free and locked characters → tap a Patreon-locked character and the Patreon gate screen appears → select a free character → play a full match end-to-end → score screen shown.

---

## Open Questions
1. **Patreon token expiry:** How often should the app re-verify Patreon membership? On every launch? Once per week? What happens if a user cancels their Patreon — do characters lock again immediately or after a grace period?

## Resolved Decisions
1. **Tennis ruleset:** Standard tennis scoring — 15/30/40/deuce/advantage, games to 6 (win by 2), best of 3 sets.
2. **Opponent AI:** Difficulty tiers (Easy/Medium/Hard). Core AI behavior: attempt to hit to empty court space. Circuit rounds scale difficulty. AI reads spin based on difficulty tier. Circuit rounds scale difficulty.
3. **Serve mechanic:** Timed dual-meter (power + accuracy), 10-second aim window, standard fault rules, server by coin toss. See Serve Mechanic section.
4. **Shot mechanic:** Dual virtual joystick. No shot buttons. Right joystick flick gesture determines shot type — slow = Lob, fast = Smash, apex lateral jerk = Slice/Topspin. Power scales with proximity to ball. See Shot Mechanic section.
5. **Movement input:** Left virtual joystick.
6. **Court selection:** Quick Match = player selects court after character select. Circuit = court fixed per round. Court Select screen mirrors Character Select (grid, locked courts via IAP only).
7. **Auth / persistence:** No mandatory login. Room-only for MVP. Patreon OAuth only for Patreon unlocks. Firestore is a post-MVP stretch goal.
8. **Launch roster:** 3 characters — Tess (red, FREE), Demi (blue, FREE), unnamed Patreon test character (green, PATREON). No IAP characters at launch.
9. **Android API level:** minSdk 21 (Android 5.0), targetSdk 35 (Android 15).
10. **Circuit vs. Tournament:** Tournament = single bracket event (single-elim or round robin). Circuit = series of Tournaments, Grand Prix style. Sub-questions (format per stop, number of stops, points vs. win-to-advance) are post-MVP decisions.

---

## Gaps

### Blocking — must be decided before implementation starts

1. ~~**Movement input method**~~ — *Resolved: dual virtual joystick. Left = movement, right = aim/shot trigger.*

2. ~~**Opponent AI definition**~~ — *Resolved: difficulty tiers; AI targets empty space; scales across circuit rounds.*

3. ~~**Tennis ruleset scope**~~ — *Resolved: standard scoring (15/30/40/deuce/ad, games to 6, best of 3 sets).*

4. ~~**Serve mechanic**~~ — *Resolved: see Serve Mechanic section.*

5. ~~**Shot mechanic depth**~~ — *Resolved: see Shot Mechanic section.*

6. ~~**Court selection**~~ — *Resolved: Quick Match = player selects court after character select. Circuit = court fixed per round.*

7. ~~**Google Sign-In / auth requirement**~~ — *Resolved: no mandatory login. Room-only persistence for MVP. Patreon OAuth surfaces only on locked character tap. Firestore sync is a post-MVP stretch goal.*

8. ~~**Character roster size at launch**~~ — *Resolved: 3 characters. See Character Roster section.*

9. ~~**Minimum Android API level**~~ — *Resolved: minSdk 21 (Android 5.0), targetSdk 35 (Android 15).*

### Blocking — must be decided before post-MVP work begins

10. ~~**Circuit vs. Tournament distinction**~~ — *Resolved: Tournament = a single bracket event (single-elimination or round robin). Circuit = a series of Tournaments, Mario Kart Grand Prix style. Sub-questions still open: see below.*

    10a. **Tournament format per circuit stop:** Is each stop in a Circuit always the same format (e.g. always single-elim), or can individual stops be single-elim or round robin? Who decides — the player or the circuit definition?

    10b. **Circuit structure:** How many tournament stops make up one Circuit? Are there multiple named Circuits (like Mushroom Cup / Star Cup)? Is there a points system across stops, or must you win each stop to advance?

    10c. **Data model impact:** The current `TournamentBracket` entity uses a flat `mode: CIRCUIT | TOURNAMENT`. With Circuit as a meta-layer, a `Circuit` entity is likely needed to hold an ordered list of `Tournament` ids. Needs redesign before post-MVP implementation.

11. ~~**Patreon tier name(s)**~~ — *Resolved: use placeholder `"tier_1"` defined as a named constant. Swap for real tier name before shipping.*

12. ~~**IAP pricing and product IDs**~~ — *Resolved: no IAP characters at launch. Scaffold billing integration with placeholder product ID `"iap_character_001"`; real IDs registered in Play Console before any IAP character ships.*

13. ~~**Offline behavior**~~ — *Resolved: moot. Room-only persistence means the app is always fully offline-capable. No sync conflicts possible.*

14. **Pass-and-play scope:** Listed as a stretch goal but completely unspecified. If it enters scope, the dual-joystick input layer and match state machine need significant changes — flag before that work is planned.
