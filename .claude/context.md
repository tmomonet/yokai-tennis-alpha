<!-- AUTO-GENERATED-START -->
## Tech Stack (from .specswarm/features/001-mvp-gameplay-desktop-android/plan.md)
- **Language**: Java (compiled to 17 bytecode; Gradle runs on Corretto 25 — JAVA_HOME required, java not on PATH)
- **Framework**: LibGDX 1.14.2 (:core game logic, :desktop LWJGL3, :android launcher)
- **Build**: Gradle 9.4.1 wrapper + AGP 9.0.0; compileSdk 36 / targetSdk 35 / minSdk 21
- **Database**: none this feature (persistence-free chunk; Room is post-chunk target stack)
- **Key Libraries**: JUnit Jupiter 6 (junit-bom 6.0.0) for :core tests

## CRITICAL CONSTRAINTS (from tech-stack.md + constitution.md)
⚠️ **BEFORE suggesting ANY library, framework, or pattern:**
1. Read `.specswarm/tech-stack.md`
2. Verify your suggestion is APPROVED
3. If PROHIBITED, suggest approved alternative
4. If UNAPPROVED, warn user and require justification

**Prohibited / out-of-scope** (NEVER introduce in Feature 001):
- ❌ Online/async PvP, stat upgrades, leaderboards, mandatory login (constitution P2/P4)
- ❌ Portrait orientation → landscape-only, locked (constitution P5)
- ❌ On-screen shot buttons → dual virtual joystick gestures only (constitution P1)
- ❌ Hilt, Room, Play Billing, Patreon OAuth, Firestore → target stack for LATER chunks; do not scaffold in Feature 001

**Violation = Constitution violation** (see `.specswarm/constitution.md`)
<!-- AUTO-GENERATED-END -->
