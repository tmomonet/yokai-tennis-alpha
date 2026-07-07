# Data Model — 001 Yokai Tennis MVP

All entities are in-memory only this chunk (persistence-free). Package root: `com.cafeyokai.tennis` in `:core`.

## Content entities

### Character
| Field | Type | Notes |
|---|---|---|
| id | String | `tess`, `demi`, `patreon_test` |
| name | String | "Tess", "Demi", "???" (patreon_test name TBD per SPEC.md roster) |
| spriteKey | String | e.g. `char.tess` — resolved via SpriteProvider (research R5) |
| unlockType | enum UnlockType | FREE, IAP, PATREON (IAP value exists, unused this chunk) |
| loreBlurb | String | one line, shown on Unlock screen |

Fixed roster of 3 (SPEC.md Character Roster). `Roster.all()` returns them in grid order.

### Court
| Field | Type | Notes |
|---|---|---|
| id | String | `court_default` |
| name | String | |
| backgroundKey | String | `court.default` |
| unlockType | enum | FREE only needed this chunk |

### GameSettings (session-scoped singleton)
| Field | Type | Notes |
|---|---|---|
| matchLength | enum MatchLength | SINGLE_SET, BEST_OF_3 (default) — FR-001a; resets on restart |
| aiDifficulty | DifficultyTier | fixed MEDIUM for Quick Match (FR-042); field exists for future chunks |

## Match engine state (pure Java, no LibGDX deps)

### TennisScore (state machine — FR-010..012)
| Field | Type | Notes |
|---|---|---|
| points | int[2] | 0,15,30,40 encoded 0..3; deuce/ad handled via states |
| gameState | enum | NORMAL, DEUCE, AD_PLAYER, AD_OPPONENT |
| games | int[2] | current set |
| sets | int[2] | sets won |
| completedSets | List<int[2]> | per-set game scores for Score screen ("6-3, 7-6") |
| tiebreak | TiebreakState? | non-null while 6-6 tiebreak active |
| setsToWin | int | 1 or 2 (from GameSettings.matchLength) |
| server | int | 0/1; alternates each game; tiebreak-internal rotation per rules |

TiebreakState: `points[2]` (first to 7, win by 2), `pointsPlayed` (serve rotates after point 1, then every 2).

Transitions: `pointWonBy(player)` → advances point→game→set→match; tiebreak entered at games 6-6; set recorded 7-6. Emits `MatchEvent` (GAME_WON, SET_WON, MATCH_OVER, TIEBREAK_STARTED) for HUD/flow.

### ServeState (FR-020..023)
| Field | Type | Notes |
|---|---|---|
| phase | enum | AIMING, POWER, ACCURACY, LAUNCHED |
| aimTarget | Vec2 | clamped to valid service box for current point |
| aimTimer | float | counts down from 10s; expiry = fault |
| powerMeter | float 0..1 | fills upward while POWER; tap locks |
| accuracyNeedle | float -1..1 | swings while ACCURACY; tap samples |
| faultCount | int | 0/1; second consecutive fault = double fault → point to receiver |

Derived: serve speed = f(power); lateral deviation = g(accuracyOffset); fault margin grows with power (FR-022).

### ShotGesture (classifier output — FR-031/032)
| Field | Type | Notes |
|---|---|---|
| type | enum ShotType | LOB, SMASH, SLICE, TOPSPIN |
| direction | Vec2 | normalized aim from flick vector |
| power | float 0..1 | flick speed → scaled by proximity at contact (FR-033) |

Input: `GestureTrace` = ordered (t, x, y) right-stick samples. Tuning constants in `GestureTuning`.

### BallState (research R6)
| Field | Type | Notes |
|---|---|---|
| pos | Vec2 | court-plane position |
| height | float | z above court |
| vel | Vec2 + vz | planar + vertical velocity |
| spin | enum SpinType | NONE, SLICE (low skid bounce), TOPSPIN (kick-up bounce) |
| lastHitBy | int | for scoring attribution |
| bounces | int | since last hit, for in/out + double-bounce point logic |

### RallyState / MatchPhase
`MatchController` phase: SERVING (delegates to ServeState) → RALLY → POINT_OVER → (score advance) → next SERVING | MATCH_OVER. Point resolution: ball out (bounce outside lines) → last hitter loses point; double bounce → receiver-side failure; net fault on serve.

### DifficultyTier (FR-040..041)
| Field | Type | Notes |
|---|---|---|
| tier | enum | EASY, MEDIUM, HARD |
| serveSpeed / servePlacementAccuracy | float | Easy: center+slow → Hard: corners+fast (FR-023) |
| reactionDelay | float | seconds before AI reacts to a hit |
| spinRead | enum | IGNORE (Easy), DELAYED (Medium), ANTICIPATE (Hard) — FR-041 |
| aimStrategy | — | all tiers target largest empty court space (FR-040) |

### CourtGeometry
Constants: court bounds, singles lines, net line, service boxes (per serving side/point parity), player range radius (FR-033). Used by physics, AI targeting, and serve fault checks.

## Presentation-layer objects

- **SpriteProvider**: `TextureRegion byKey(String key)` — runtime Pixmap placeholder impl this chunk (research R5).
- **VirtualJoystick**: renders base+knob, exposes displacement vector + sample stream; touch on Android, mouse-driven right stick on desktop; left stick fed by WASD on desktop (research R7).
- **Screens**: MainMenu, CharacterSelect, Unlock (Patreon gate placeholder), CourtSelect, Match (HUD per SPEC.md wireframe), Score, Settings (match length), Circuit/Tournament stubs.
