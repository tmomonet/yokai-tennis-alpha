# Research — 001 Yokai Tennis MVP

All versions below were verified against live repositories on 2026-07-07 (not recalled from memory).

## R1. LibGDX version

- **Decision**: LibGDX **1.14.2** (`com.badlogicgames.gdx:gdx`, `gdx-backend-lwjgl3`, `gdx-platform` natives, `gdx-backend-android`).
- **Rationale**: Latest release on Maven Central (metadata lastUpdated 2026-06-05). Desktop LWJGL3 backend is the supported dev-iteration path per SPEC.md Tech Stack.
- **Alternatives considered**: 1.13.5 (older stable) — no reason to pin back; libKTX (Kotlin) — project is Java per SPEC.md.

## R2. Android Gradle Plugin + Gradle

- **Decision**: AGP **9.0.0** with Gradle **9.4.1** wrapper (bumped from 9.3.0 during T004 — AGP 9.0.0 requires Gradle ≥ 9.4.1).
- **Rationale**: 9.0.0 is the max AGP supported by the installed Android Studio (9.2.1 built fine but broke IDE sync; downgraded 2026-07-07). AGP 9.x is the Gradle-9-compatible line.
- **Risk**: If AGP 9.0.0 rejects Gradle 9.3.0 or `minSdk 21` (AGP has been raising its minSdk floor), fallbacks in order: AGP 9.3.0-rc01; raise minSdk only with explicit user sign-off since SPEC.md Resolved Decision 9 pins minSdk 21.

## R3. JDK / toolchain

- **Decision**: Run Gradle with **JDK 25** (`C:/Users/e48994/.jdks/corretto-25.0.3` — only JDK on the machine; `java` is NOT on PATH, so command-line builds set `JAVA_HOME` or `org.gradle.java.home`). Compile all modules to **Java 17** bytecode (`options.release = 17` / Android `compileOptions` 17) so D8 dexing and LibGDX are comfortably in range.
- **Rationale**: AGP 9 requires JDK 17+ to run; Java 17 bytecode is safely dexable and LibGDX-compatible.

## R4. Android SDK levels

- **Decision**: `compileSdk 36`, `targetSdk 35`, `minSdk 21`.
- **Rationale**: Only platform installed locally is android-36.1 (build-tools 36.1.0/37.0.0); compileSdk must be installed, targetSdk need not be. targetSdk 35 and minSdk 21 are pinned by SPEC.md Resolved Decision 9. `local.properties` with `sdk.dir` will be generated (and added to `.gitignore` — currently missing from it).

## R5. Placeholder art strategy (FR-054)

- **Decision**: No binary asset files. A `SpriteProvider` interface resolves string keys (e.g., `char.tess`, `court.default`) to `TextureRegion`s; the MVP implementation generates solid-color geometric `Pixmap` textures at runtime. Real art later = swap the provider/AssetManager implementation, keys unchanged.
- **Rationale**: Satisfies SPEC.md's "all sprites referenced by key, swappable" without committing placeholder PNGs; keeps the repo clean.
- **Alternatives**: Committed placeholder PNGs via AssetManager — more moving parts for zero MVP value.

## R6. Ball physics model

- **Decision**: 2.5D arcade model: ball position is (x, y) on the court plane plus a height z with its own vertical velocity/gravity; rendering is top-down (SPEC.md wireframe) with height shown via shadow offset + sprite scale. Fixed-timestep simulation (e.g., 1/120s accumulator) so gameplay is deterministic and testable.
- **Rationale**: Matches the top-down GBA-style presentation; deterministic stepping makes scoring/AI/gesture logic unit-testable without a GL context.

## R7. Desktop input mapping (clarified 2026-07-07)

- **Decision**: In `:core`, input goes through a small abstraction: left-stick vector + right-stick sample stream. On `ApplicationType.Desktop`, WASD/arrows produce the left-stick vector and mouse drag produces right-stick samples (click = serve-meter tap); on Android both are touch joysticks. Standard LibGDX API only — no platform code in `:core`.

## R8. Gesture classification (FR-031/032)

- **Decision**: Classifier consumes a time-stamped sample trace of right-stick displacement. Flick speed = peak radial velocity over the gesture window → LOB below threshold, SMASH above. Apex jerk = perpendicular velocity spike within a tolerance window around peak displacement → SLICE (against swing) / TOPSPIN (with swing). All thresholds live in one `GestureTuning` constants class so tuning is a data change; classifier is pure Java, tested with synthetic traces (FR-034).
- **Fallback** (spec-sanctioned): if apex detection can't be made reliable, ship LOB/SMASH only and defer spin — decision point during implementation, surfaced to user before dropping scope.

## R9. Module layout

- **Decision**: Three Gradle subprojects: `:core` (java-library; all game code + tests), `:desktop` (application; LWJGL3 launcher), `:android` (com.android.application; launcher only). Root `build.gradle.kts` loses the `java` plugin (currently compiles root `src/`); `src/main/java/com/cafeyokai/Main.java` is deleted (superseded); `src/SPEC.md` stays where it is.
- **Rationale**: Standard gdx-setup layout; keeps SPEC.md path untouched.

## R10. Test framework

- **Decision**: Keep JUnit Jupiter 6 (junit-bom 6.0.0) exactly as in the current root build, moved to `:core`. Engine code under test (scoring, gestures, serve math, AI targeting) has no LibGDX rendering dependencies.
