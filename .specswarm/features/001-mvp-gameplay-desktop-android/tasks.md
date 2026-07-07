# Tasks — 001 Yokai Tennis MVP (desktop + Android)

<!-- Tech Stack Validation: PASSED -->
<!-- Validated against: .specswarm/tech-stack.md (2026-07-07, Feature 001 pins) -->
<!-- No prohibited technologies found; no unresolved conflicts in plan.md -->

**Feature**: Yokai Tennis MVP — Playable Cross-Platform Demo
**Story map**: US1 = end-to-end Quick Match (Scenario 1) · US2 = Patreon gate (Scenario 2) · US3 = serve mechanic (Scenario 3) · US4 = gesture shots (Scenario 4) · US5 = cross-platform dev loop (Scenario 5)
**Environment for every build task**: `JAVA_HOME="C:/Users/e48994/.jdks/corretto-25.0.3"` (java not on PATH); Android SDK at `%LOCALAPPDATA%/Android/Sdk`; Gradle wrapper 9.4.1 (AGP 9.2.1 minimum).

## Phase 1 — Setup: multi-module skeleton (US5, blocking all)

- [X] T001 [US5] Restructure root: remove `java` plugin from build.gradle.kts, include `core`/`desktop`/`android` in settings.gradle.kts, add gradle.properties (AndroidX flags, jvmargs), delete `src/main/java/com/cafeyokai/Main.java` and empty root `src/main`+`src/test` trees (KEEP `src/SPEC.md`), add `local.properties` (sdk.dir) and gitignore it — build.gradle.kts, settings.gradle.kts, gradle.properties, .gitignore
- [X] T002 [US5] Create `:core` module: java-library, options.release=17, LibGDX 1.14.2 api dep, JUnit Jupiter 6 (junit-bom 6.0.0) test deps moved from root, minimal `YokaiTennisGame extends Game` with a solid-color clear screen — core/build.gradle.kts, core/src/main/java/com/cafeyokai/tennis/YokaiTennisGame.java
- [X] T003 [US5] Create `:desktop` module: gdx-backend-lwjgl3 1.14.2 + natives-desktop, `DesktopLauncher` fixed 1280×720 window titled "Yokai Tennis". GATE: `gradlew desktop:run` opens a window; `gradlew test` passes — desktop/build.gradle.kts, desktop/src/main/java/com/cafeyokai/tennis/desktop/DesktopLauncher.java
- [X] T004 [US5] Create `:android` module: AGP 9.2.1, namespace com.cafeyokai.tennis, compileSdk 36/targetSdk 35/minSdk 21, landscape-locked manifest, `AndroidLauncher` delegating to YokaiTennisGame, gdx-backend-android + arm/x86 natives. GATE: `gradlew android:assembleDebug` produces APK. Front-loads the AGP/Gradle-9.3 risk (plan R2): if incompatible try AGP 9.3.0-rc01; if minSdk 21 rejected STOP and ask user (SPEC pin) — android/build.gradle.kts, android/src/main/AndroidManifest.xml, android/src/main/java/com/cafeyokai/tennis/android/AndroidLauncher.java

## Phase 2 — Pure engine + tests (US3/US4; no rendering deps)

- [X] T005 [P] [US1] Implement TennisScore + TiebreakState + MatchEvent per contracts/scoring-engine.md (deuce/ad, win-by-2, 7-pt tiebreak at 6-6, setsToWin 1|2, serve rotation, completedSetScores, event queue) — core/src/main/java/com/cafeyokai/tennis/engine/score/
- [X] T006 [US1] Unit-test TennisScore against ALL contract invariants 1–8 (FR-012) — core/src/test/java/com/cafeyokai/tennis/engine/score/TennisScoreTest.java
- [X] T007 [P] [US4] Implement GestureClassifier + GestureTrace + GestureTuning + ShotGesture per contracts/gesture-classifier.md (flick-speed lob/smash, apex perpendicular-jerk slice/topspin, deterministic) — core/src/main/java/com/cafeyokai/tennis/engine/gesture/
- [X] T008 [US4] Unit-test GestureClassifier with ALL required synthetic traces from the contract, including the sloppy-flick false-positive guard (FR-034) — core/src/test/java/com/cafeyokai/tennis/engine/gesture/GestureClassifierTest.java
- [X] T009 [P] [US1] Implement CourtGeometry (bounds, service boxes, net, range radius) + BallState/BallSimulator: 2.5D fixed-timestep (plan R6), spin bounce behavior (slice skids low, topspin kicks up), in/out + double-bounce detection — core/src/main/java/com/cafeyokai/tennis/engine/physics/
- [X] T010 [US1] Unit-test physics: landing in/out calls, service-box checks, slice/topspin bounce deltas, range-radius miss, distance-scaled power (FR-033) — core/src/test/java/com/cafeyokai/tennis/engine/physics/
- [X] T011 [US3] Implement ServeState machine per spec FR-020..023: AIMING (10s timer, target clamped to service box) → POWER (fill, tap-lock) → ACCURACY (needle, deviation shifts landing) → LAUNCHED; fault/double-fault; power→speed + fault-margin math — core/src/main/java/com/cafeyokai/tennis/engine/serve/
- [X] T012 [US3] Unit-test ServeState: window expiry = fault, double fault awards receiver, power/accuracy → landing math, meter phase ordering — core/src/test/java/com/cafeyokai/tennis/engine/serve/ServeStateTest.java
- [X] T013 [US1] Implement AiController + DifficultyTier (EASY/MEDIUM/HARD): empty-space targeting (FR-040), tier serve speed/placement (FR-023), spin read IGNORE/DELAYED/ANTICIPATE (FR-041), injectable Random seed — core/src/main/java/com/cafeyokai/tennis/engine/ai/
- [X] T014 [US1] Unit-test AI with seeded determinism: targets empty half, tier reaction/spin-read differences observable, serve placement spread per tier — core/src/test/java/com/cafeyokai/tennis/engine/ai/AiControllerTest.java

## Phase 3 — Screens & flow (US1/US2)

- [X] T015 [US1] Implement SpriteProvider (key→TextureRegion) + PlaceholderSprites (runtime Pixmap: red/blue/green character shapes per roster, court background, ball, padlock badge, joystick base/knob) + screen navigation in YokaiTennisGame — core/src/main/java/com/cafeyokai/tennis/gfx/, YokaiTennisGame.java
- [X] T016 [P] [US1] Implement content classes (Character, Court, Roster with tess/demi/patreon_test per SPEC roster, UnlockType, GameSettings with MatchLength default BEST_OF_3) + MainMenuScreen (Play/Circuit/Tournament/Settings) + SettingsScreen (match-length toggle, FR-001a) + Circuit/Tournament stub screens — core/src/main/java/com/cafeyokai/tennis/content/, core/src/main/java/com/cafeyokai/tennis/screens/
- [X] T017 [US1] Implement CharacterSelectScreen: 3-portrait grid via spriteKeys, padlock badge on patreon_test, Confirm/Back, free chars selectable, locked tap → UnlockScreen — core/src/main/java/com/cafeyokai/tennis/screens/CharacterSelectScreen.java
- [X] T018 [P] [US2] Implement UnlockScreen (Patreon gate): portrait, name, one-line lore, exact SPEC Flow 2 copy "This character is a Patreon exclusive. Connect your Patreon account to unlock.", visible non-functional Connect Patreon button, Back — core/src/main/java/com/cafeyokai/tennis/screens/UnlockScreen.java
- [X] T019 [P] [US1] Implement CourtSelectScreen (thumbnail grid, ≥1 free court, Confirm/Back) + ScoreScreen (set-by-set e.g. "6-3, 7-6", WIN/LOSS, continue → MainMenu) — core/src/main/java/com/cafeyokai/tennis/screens/CourtSelectScreen.java, ScoreScreen.java

## Phase 4 — Match integration (US1/US3/US4)

- [ ] T020 [US4] Implement VirtualJoystick widget (render base+knob, displacement vector, timestamped sample stream) + MatchInput mapping: Android touch both sticks; Desktop WASD/arrows → left stick, mouse drag/flick → right stick samples, click = meter tap (clarified 2026-07-07) — core/src/main/java/com/cafeyokai/tennis/input/
- [ ] T021 [US1] Implement MatchScreen rendering: top-down court from CourtGeometry, character sprites, ball with shadow/height scale, HUD score top-center (sets/games/points incl. tiebreak digits) + serve indicator top-left per SPEC wireframe — core/src/main/java/com/cafeyokai/tennis/screens/MatchScreen.java
- [ ] T022 [US3] Wire serve flow in MatchController + MatchScreen: aim drag/indicator in service box, 10s countdown display, power bar UI, accuracy needle UI, fault/double-fault messaging, AI serves skip meters using tier params — core/src/main/java/com/cafeyokai/tennis/engine/MatchController.java, MatchScreen.java
- [ ] T023 [US4] Wire rally loop: right-stick trace → GestureClassifier → shot execution with range-radius check + distance-scaled power → BallSimulator flight/bounce → point resolution (out/double-bounce/net) → TennisScore.pointWonBy → next serve or match over — core/src/main/java/com/cafeyokai/tennis/engine/MatchController.java
- [ ] T024 [US1] Integrate AI opponent end-to-end: AI movement toward intercept, AI shot selection/returns vs player spin per tier, Quick Match fixed to MEDIUM (FR-042) — core/src/main/java/com/cafeyokai/tennis/engine/MatchController.java, engine/ai/
- [ ] T025 [US1] Match completion flow: MATCH_OVER → ScoreScreen with real set scores → MainMenu; honor GameSettings.matchLength (single set vs best-of-3) — core/src/main/java/com/cafeyokai/tennis/screens/, engine/

## Phase 5 — Polish & verification (US1..US5)

- [ ] T026 [US4] Tuning pass: play-test gestures on desktop, adjust GestureTuning until all four shot types are reliably producible (Success Criterion 4) and Medium AI is winnable AND losable (Criterion 5); if apex-jerk cannot be stabilized, STOP and ask user before falling back to lob/smash-only (spec-sanctioned fallback) — core/src/main/java/com/cafeyokai/tennis/engine/gesture/GestureTuning.java, engine/ai/
- [ ] T027 [US5] Full definition-of-done verification: `gradlew test` green; `gradlew desktop:run` complete demo (menu → char select → Patreon gate on locked tap → free char → court → full best-of-3 vs Medium AI → score screen); `gradlew android:assembleDebug` APK; fix any fallout — whole repo
- [ ] T028 [US5] Update `.specswarm/tech-stack.md` "Current build.gradle.kts state" line to describe the new module layout (FR-055); confirm quickstart.md commands match reality — .specswarm/tech-stack.md, .specswarm/features/001-mvp-gameplay-desktop-android/quickstart.md

## Dependencies

- T001 → T002 → T003 (desktop gate) ; T004 after T002 (android gate) — Phase 1 strictly ordered except T003/T004 can follow T002 in either order.
- Phase 2: T005/T007/T009 independent [P]; each test task follows its impl (T006←T005, T008←T007, T010←T009); T011 needs T009 (service boxes); T013 needs T009 (court targeting); T012←T011, T014←T013.
- Phase 3 needs T002 only (rendering skeleton): T015 first, then T016–T019 (T016/T018/T019 parallel; T017 needs content from T016).
- Phase 4 needs Phases 2+3: T020 → T021 → T022 → T023 → T024 → T025 (same files, sequential).
- Phase 5 needs everything: T026 → T027 → T028.

## Implementation strategy

Front-load platform risk (T004) before writing engine code. Engine (Phase 2) is pure Java — iterate with `gradlew test` only, no window needed. Use Settings single-set mode for all manual play-testing; run the full best-of-3 once at T027. MVP checkpoint after T025: demo is functionally complete; T026–T028 are quality gates.
