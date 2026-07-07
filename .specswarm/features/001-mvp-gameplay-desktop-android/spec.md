---
parent_branch: master
feature_number: 001
status: In Progress
created_at: 2026-07-07T12:45:18-06:00
references_consulted:
  - src/SPEC.md
---

# Feature: Yokai Tennis MVP — Playable Cross-Platform Demo

## Overview

Deliver the complete MVP demo defined in `src/SPEC.md` "Success Criteria": a player launches the game, navigates the menu flow, sees free and locked characters, encounters the Patreon gate on a locked character, then plays a full arcade tennis match against the AI through to a score screen. The game runs on two platforms from one shared codebase: a desktop build for fast development iteration and an Android build for the real target device.

This chunk is deliberately persistence-free and monetization-free: the Patreon gate is a visual placeholder (per SPEC.md Flow 2 steps 1–2 only), and nothing survives an app restart. The heart of the work is the gameplay itself — serve mechanic, gesture-driven shots, tennis scoring, and opponent AI — implemented verbatim from the corresponding SPEC.md sections.

## Clarifications

### Session 2026-07-07

- Q: How should desktop input map the dual-joystick controls, given a single mouse? → A: WASD/arrow keys drive movement (left joystick); mouse drag/flick drives the shot joystick. Touch joysticks render and work as specced on Android.
- Q: What happens at 6-6 in a set (SPEC.md is silent on tiebreaks)? → A: Standard 7-point tiebreak (first to 7, win by 2) at 6-6.
- Q: Include a short-match configuration to make end-to-end verification practical? → A: Yes, player-facing: the Settings screen offers a match-length option (single set / best of 3). In-memory only; resets to best-of-3 on restart.

## User Scenarios

### Scenario 1: End-to-end Quick Match (primary — mirrors SPEC.md Flow 1)
Jordan launches the app. The main menu shows Play, Circuit, Tournament, and Settings. Jordan taps Play, sees the character grid (Tess and Demi selectable; a third character shows a padlock), picks Demi, confirms, picks a court, and the match loads in landscape. Jordan serves using the aim → power meter → accuracy meter sequence, rallies using the two virtual joysticks, and plays a full best-of-3-sets match against a Medium AI. When the match ends, a score screen shows the result, and Jordan returns to the main menu.

### Scenario 2: Patreon gate discovery (mirrors SPEC.md Flow 2, steps 1–2)
Mia opens Character Select and taps the padlocked green character. An unlock screen appears: character portrait, name/lore line, and the message "This character is a Patreon exclusive. Connect your Patreon account to unlock." with a Connect Patreon button. The button is a non-functional placeholder in this chunk; Mia backs out to Character Select. No OAuth occurs.

### Scenario 3: Serving under pressure (mirrors SPEC.md Serve Mechanic)
At the start of a point, the serve indicator highlights the server and a 10-second aim window opens. The player drags to aim within the valid service box, taps Serve to start the power meter, taps again to lock power, then taps a third time as the accuracy needle swings to center it. A mistimed accuracy tap pushes the landing spot off-target; two consecutive faults award the point to the receiver; letting the 10-second window expire counts as a fault.

### Scenario 4: Gesture shots in a rally (mirrors SPEC.md Shot Mechanic)
During a rally the player moves with the left joystick and returns the ball with a right-joystick flick: a slow flick lobs deep, a hard flick smashes flat and fast, and a lateral jerk at the apex of the flick adds slice or topspin. If the player is outside the range radius when the ball arrives, the shot misses — there is no auto-teleport to the ball. Shot power scales with proximity to the ball at contact.

### Scenario 5: Developer iteration loop
A developer runs the desktop build, plays through the full flow on a 1280×720 landscape window without an emulator, and separately produces an Android debug APK from the same shared game code.

## Functional Requirements

### Screens & Navigation (SPEC.md Flow 1 + Wireframes)
- **FR-001**: Main Menu displays Play, Circuit, Tournament, and Settings. Play enters the Quick Match flow; Circuit and Tournament may be stub screens with a Back control.
- **FR-001a**: Settings screen offers a match-length option — Single Set or Best of 3 (default) — applying to the next Quick Match. The choice is held in memory only and resets to Best of 3 on restart (persistence-free chunk).
- **FR-002**: Character Select shows the three-character launch roster as a grid of geometric placeholder portraits, each referenced by a swappable asset key (SPEC.md Character Roster): Tess (red, FREE), Demi (blue, FREE), patreon_test (green, PATREON, padlock badge). Free characters are selectable with Confirm/Back controls.
- **FR-003**: Tapping the PATREON-locked character opens the Unlock screen: portrait, name, one-line lore blurb, the Patreon-exclusive message, and a Connect Patreon button that is visibly present but performs no OAuth (placeholder only). Back returns to Character Select.
- **FR-004**: Court Select shows a grid of court thumbnails after character confirmation; at least one free court is selectable. Confirm loads the match.
- **FR-005**: When a match completes, a Score screen displays the final result (set-by-set score, win/loss) and returns the player to the Main Menu.
- **FR-006**: All screens are landscape-only, locked (Constitution P5).

### Scoring (SPEC.md Resolved Decision 1)
- **FR-010**: Standard tennis scoring: 15/30/40, deuce/advantage, games to 6 with win-by-2, best of 3 sets. At 6-6 in a set, a standard 7-point tiebreak (first to 7, win by 2) decides the set (clarified 2026-07-07). The match ends the moment a player wins the required number of sets (2, or 1 when Single Set is selected in Settings per FR-001a).
- **FR-011**: The in-match HUD shows sets, games, and current game points at top center, plus a serve indicator (SPEC.md Wireframes: In-Game Court HUD).
- **FR-012**: The scoring state machine is fully unit-tested, including deuce/advantage cycles, win-by-2 game logic, tiebreak entry at 6-6 and tiebreak scoring (including serve rotation within the tiebreak), and set/match completion for both match lengths.

### Serve Mechanic (SPEC.md Serve Mechanic — verbatim)
- **FR-020**: Server chosen by coin toss at match start; service alternates each game thereafter.
- **FR-021**: Per point: 10-second aim window with a visible aimed-landing-spot indicator constrained to the valid service box; tap starts the power meter (fills upward, tap locks); accuracy meter (swinging needle) immediately follows, deviation from center shifting the landing position laterally.
- **FR-022**: Higher locked power produces a faster serve with a larger fault margin. A serve landing outside the service box is a fault; two consecutive faults on one point is a double fault awarding the point to the receiver; aim-window expiry counts as a fault.
- **FR-023**: AI serves skip the meter UI entirely; AI serve speed and placement derive from its difficulty tier (Easy: center/slow → Hard: corners/fast).

### Shot Mechanic (SPEC.md Shot Mechanic — verbatim)
- **FR-030**: Dual virtual joysticks, no shot buttons (Constitution P1): left joystick moves the character; right joystick aims and triggers shots.
- **FR-031**: Right-joystick flick speed selects shot type: slow flick = Lob (high arc, deep, soft), fast flick = Smash (flat, fast, can go out near max power).
- **FR-032**: A sudden lateral jerk at the apex of the flick adds spin: against swing direction = Slice (stays low, skids), with swing direction = Topspin (kicks up after bounce). Apex detection tracks joystick velocity and triggers on a perpendicular velocity spike at peak displacement, tuned to avoid false positives (SPEC.md implementation note). If tuning stalls, lob/smash-only is an acceptable fallback with slice/topspin deferred — but attempt the full mechanic first.
- **FR-033**: The player must be within a defined range radius of the ball to connect; outside range the swing misses (no auto-teleport). Shot power scales with distance to ball at contact — full power close in, reduced near the range boundary.
- **FR-034**: Shot-gesture classification (flick speed thresholds, apex-jerk detection) is fully unit-tested with synthetic joystick input traces.

### Opponent AI (SPEC.md Resolved Decision 2)
- **FR-040**: An Easy/Medium/Hard difficulty-tier framework exists; the core behavior at every tier is attempting to hit into empty court space.
- **FR-041**: Tiers modulate serve speed/placement (FR-023) and spin reading: Easy ignores spin and returns to center; Medium reacts to spin with delay; Hard anticipates spin direction.
- **FR-042**: Quick Match plays against the Medium tier.
- **FR-043**: The AI can win and lose: it plays a complete match under the same scoring, serve (sans meters), and range rules as the player.

### Platforms & Project Structure
- **FR-050**: One shared game codebase serves both platforms: all game logic, screens, and rendering are platform-agnostic (no Android-only dependencies in shared code).
- **FR-051**: The desktop build launches a fixed 1280×720 landscape window and supports the full flow end-to-end. Desktop input mapping (clarified 2026-07-07): WASD/arrow keys drive character movement (left-joystick equivalent); mouse drag/flick drives the shot joystick, including serve meter taps (mouse click). On Android both virtual joysticks are touch-driven as specced.
- **FR-052**: The Android build targets minSdk 21 / targetSdk 35, landscape locked, and produces an installable debug APK.
- **FR-053**: The pre-existing placeholder entry point (`com.cafeyokai.Main`) is migrated into the shared module or removed; existing unit tests remain green throughout.
- **FR-054**: All sprites and court backgrounds are geometric placeholders referenced by asset-manager keys so real art can be swapped in later without code changes (SPEC.md Tech Stack: Asset management).
- **FR-055**: Project documentation of the current build state (`.specswarm/tech-stack.md` "Current build.gradle.kts state" line) is updated to reflect the new module layout.

### Match Verifiability
- **FR-060**: The Settings match-length option (FR-001a) doubles as the practical verification path: end-to-end demo runs may use Single Set, while the definition-of-done demo remains a full best-of-3 match.

## Success Criteria

1. A first-time player can go from app launch to playing a match in under 60 seconds of navigation (menu → character → court → match start).
2. The SPEC.md Success Criteria demo passes end-to-end on desktop: launch in landscape → main menu → character select shows free and locked characters → tapping the Patreon-locked character shows the Patreon gate screen → select a free character → play a full match to completion against the AI → score screen shown.
3. The same demo flow is achievable on an Android device/emulator from the debug APK.
4. Every shot type in the spec (lob, smash, slice, topspin) can be reliably produced on demand by a player following the gesture descriptions, and misses occur when out of range.
5. A complete match against Medium AI is winnable and losable by a competent player (AI is neither unbeatable nor a pushover).
6. Automated tests fully cover the scoring state machine and shot-gesture classification, and the full test suite passes.
7. The Android artifact builds successfully and the desktop build runs, from a clean checkout, with standard project commands.

## Key Entities

All in-memory for this chunk (persistence-free by design):

- **Character** — id, name, spriteKey, unlockType (FREE | PATREON for this chunk; IAP enum value may exist but is unused). Roster fixed at 3 (SPEC.md Character Roster).
- **Court** — id, name, backgroundKey, unlockType (FREE only needed this chunk).
- **MatchState** — current server, point score (0/15/30/40/deuce/ad), tiebreak state (active flag, tiebreak points, serve rotation), games per set, sets won, sets-to-win (1 or 2 per match-length setting), fault count, match phase (serving/rally/point-over/match-over).
- **ShotGesture** — classified shot input: type (LOB | SMASH | SLICE | TOPSPIN), direction, power.
- **DifficultyTier** — EASY | MEDIUM | HARD with serve speed/placement and spin-read parameters.

## Out of Scope (explicit — do not scaffold)

Per the feature request and SPEC.md Non-Goals / Post-MVP boundaries:

- Dependency injection framework (Hilt), local database (Room), Google Play Billing, real Patreon OAuth, Firestore — none of these are added or scaffolded.
- Circuit/Tournament progression logic (menu entries are stubs).
- Persistence of any kind — no match history, unlock records, or profile survive restart.
- Online/async PvP, stat upgrades, leaderboards, cloud save (Constitution P4).
- Real character/court art (placeholders only), IAP characters (none at launch per Resolved Decision 8/12).

## Assumptions

- **Coin toss presentation**: a brief on-screen indicator of who serves first is sufficient; no elaborate animation required.
- **Court physics presentation**: top-down court view (SPEC.md Wireframes) with ball height represented implicitly (shadow/scale) — arcade fidelity, not simulation.
- **Single free court** is sufficient to satisfy Court Select for this chunk; additional courts are content, not mechanics.
- **AI vs AI is not required**; the AI only plays as the opponent.
- **Stub screens** (Circuit, Tournament) show a title and Back control only. Settings shows the match-length option (FR-001a) plus Back.
- **Patreon gate copy** uses the exact message from SPEC.md Flow 2 step 2; the lore blurb for `patreon_test` is placeholder text (name TBD per roster table).
- **Desktop window** is fixed-size 1280×720; resizability is not required this chunk.

## Sources

This spec was generated by consulting the following references (per `.specswarm/references.md`):

| Source | Sections informing this spec |
|--------|------------------------------|
| `src/SPEC.md` | Success Criteria (definition of the demo); Flow 1 (screen order); Flow 2 steps 1–2 (Patreon gate placeholder); Wireframes (Main Menu, Character Select, HUD, Court Select, Unlock screen); Serve Mechanic (FR-020..023); Shot Mechanic (FR-030..034); Resolved Decisions 1, 2, 4, 5, 6, 8, 9 (scoring, AI, input, courts, roster, SDK levels); Character Roster table (FR-002); Tech Stack table (platforms, asset keys); Non-Goals (out-of-scope fence) |
| `.specswarm/constitution.md` | P1 (no shot buttons → FR-030), P2 (no login gate → gate is placeholder only), P4 (non-goals fence), P5 (landscape-only → FR-006) |
| Memory directory (`references.md`) | Empty — no memory files to consult yet |

No section was fabricated without a corresponding source citation OR `[NEEDS CLARIFICATION]` marker.
