# Contract — GestureClassifier

Pure-Java classifier in `:core`; consumes right-stick sample traces, emits ShotGesture. FR-034's tests verify this contract with synthetic traces.

## API surface

```java
GestureClassifier c = new GestureClassifier(GestureTuning tuning);
Optional<ShotGesture> result = c.classify(GestureTrace trace);
// GestureTrace: ordered samples (tSeconds, x, y), stick displacement in [-1,1]^2
// empty Optional = no valid gesture (e.g., sub-threshold wiggle)
```

## Classification rules (SPEC.md Shot Mechanic, verbatim)

1. **Flick speed** = peak radial velocity of displacement over the gesture window.
   - `speed < tuning.smashThreshold` → LOB
   - `speed >= tuning.smashThreshold` → SMASH
2. **Apex jerk**: within `tuning.apexWindow` seconds of peak displacement, a perpendicular velocity component exceeding `tuning.jerkThreshold`:
   - jerk against swing direction → SLICE
   - jerk with swing direction → TOPSPIN
   - no spike → shot type from rule 1 unchanged
3. **direction** = normalized dominant displacement vector; **power** = clamp01(speed / tuning.maxSpeed).
4. Deterministic: same trace + same tuning → same result, always (no randomness, no wall-clock).

## Required test traces (minimum)

- Slow smooth flick → LOB
- Fast straight flick → SMASH
- Fast flick + perpendicular spike at apex, against swing → SLICE
- Same but with swing → TOPSPIN
- Sloppy/imprecise fast flick with mild wobble (below jerkThreshold) → SMASH, NOT slice/topspin (false-positive guard, SPEC.md implementation note)
- Sub-threshold jitter → empty Optional
- Spike well before/after apex window → no spin

## Tuning

All thresholds in `GestureTuning` (data class, single source). Retuning must not require classifier code changes.
