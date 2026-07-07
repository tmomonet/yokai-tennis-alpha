package com.cafeyokai.tennis.engine.gesture;

import java.util.List;
import java.util.Optional;

/**
 * Classifies right-stick gesture traces into shots (SPEC.md Shot Mechanic):
 *
 * 1. Flick speed (peak radial velocity) below {@code smashThreshold} is a
 *    LOB, at/above it a SMASH.
 * 2. A perpendicular velocity spike above {@code jerkThreshold} within
 *    {@code apexWindowSeconds} of peak displacement overrides to spin:
 *    spike counter-clockwise of the swing direction ("with" the follow-through)
 *    is TOPSPIN, clockwise ("against") is SLICE.
 * 3. Direction is the normalized apex displacement; power is
 *    clamp01(flickSpeed / maxSpeed).
 *
 * Deterministic: no randomness, no wall clock — same trace + tuning always
 * yields the same result.
 */
public final class GestureClassifier {

    private final GestureTuning tuning;

    public GestureClassifier(GestureTuning tuning) {
        this.tuning = tuning;
    }

    public Optional<ShotGesture> classify(GestureTrace trace) {
        List<GestureTrace.Sample> s = trace.samples();
        if (s.size() < 2) {
            return Optional.empty();
        }

        // Apex: sample of peak displacement magnitude.
        int apexIndex = 0;
        float apexMag = s.get(0).magnitude();
        for (int i = 1; i < s.size(); i++) {
            float m = s.get(i).magnitude();
            if (m > apexMag) {
                apexMag = m;
                apexIndex = i;
            }
        }
        if (apexMag < tuning.minDisplacement()) {
            return Optional.empty(); // sub-threshold wiggle, no gesture
        }

        GestureTrace.Sample apex = s.get(apexIndex);
        float dirX = apex.x() / apexMag;
        float dirY = apex.y() / apexMag;

        // Flick speed: peak outward (radial) velocity across the window.
        float flickSpeed = 0f;
        for (int i = 0; i + 1 < s.size(); i++) {
            float dt = s.get(i + 1).tSeconds() - s.get(i).tSeconds();
            if (dt <= 0f) {
                continue;
            }
            float radial = (s.get(i + 1).magnitude() - s.get(i).magnitude()) / dt;
            if (radial > flickSpeed) {
                flickSpeed = radial;
            }
        }

        ShotType type = flickSpeed < tuning.smashThreshold() ? ShotType.LOB : ShotType.SMASH;

        // Apex jerk: strongest signed perpendicular velocity near the apex.
        float peakPerp = 0f;
        for (int i = 0; i + 1 < s.size(); i++) {
            float dt = s.get(i + 1).tSeconds() - s.get(i).tSeconds();
            if (dt <= 0f) {
                continue;
            }
            float segmentTime = s.get(i + 1).tSeconds();
            if (Math.abs(segmentTime - apex.tSeconds()) > tuning.apexWindowSeconds()) {
                continue;
            }
            float vx = (s.get(i + 1).x() - s.get(i).x()) / dt;
            float vy = (s.get(i + 1).y() - s.get(i).y()) / dt;
            float perp = dirX * vy - dirY * vx; // cross product z: + is CCW of swing
            if (Math.abs(perp) > Math.abs(peakPerp)) {
                peakPerp = perp;
            }
        }
        if (Math.abs(peakPerp) >= tuning.jerkThreshold()) {
            type = peakPerp > 0f ? ShotType.TOPSPIN : ShotType.SLICE;
        }

        float power = Math.min(1f, Math.max(0f, flickSpeed / tuning.maxSpeed()));
        return Optional.of(new ShotGesture(type, dirX, dirY, power));
    }
}
