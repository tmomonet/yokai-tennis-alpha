package com.cafeyokai.tennis.engine.gesture;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Synthetic-trace tests for every required case in
 * contracts/gesture-classifier.md (FR-034).
 *
 * Convention: all traces swing "up" (+y). With swing direction (0,1) the
 * signed perpendicular is -vx, so a +x spike at the apex reads as SLICE
 * (against the swing) and a -x spike as TOPSPIN (with it).
 */
class GestureClassifierTest {

    private final GestureTuning tuning = GestureTuning.defaults();
    private final GestureClassifier classifier = new GestureClassifier(tuning);

    /** Straight +y flick from 0 to 0.9 displacement over {@code duration} seconds. */
    private static List<GestureTrace.Sample> upwardFlick(float duration) {
        List<GestureTrace.Sample> s = new ArrayList<>();
        int steps = Math.round(duration / 0.01f);
        for (int i = 0; i <= steps; i++) {
            float t = i * 0.01f;
            s.add(new GestureTrace.Sample(t, 0f, 0.9f * i / steps));
        }
        return s;
    }

    private ShotGesture classify(List<GestureTrace.Sample> samples) {
        Optional<ShotGesture> r = classifier.classify(new GestureTrace(samples));
        assertTrue(r.isPresent(), "expected a valid gesture");
        return r.get();
    }

    @Test
    @DisplayName("Slow smooth flick → LOB")
    void slowFlickIsLob() {
        ShotGesture g = classify(upwardFlick(0.45f)); // ~2 units/s
        assertEquals(ShotType.LOB, g.type());
        assertEquals(0f, g.directionX(), 1e-4);
        assertEquals(1f, g.directionY(), 1e-4);
        assertTrue(g.power() < 0.5f, "slow flick should be low power");
    }

    @Test
    @DisplayName("Fast straight flick → SMASH with full power and +y direction")
    void fastFlickIsSmash() {
        ShotGesture g = classify(upwardFlick(0.1f)); // ~9 units/s
        assertEquals(ShotType.SMASH, g.type());
        assertEquals(1f, g.directionY(), 1e-4);
        assertEquals(1f, g.power(), 1e-4);
    }

    @Test
    @DisplayName("Fast flick + perpendicular spike at apex against swing → SLICE")
    void apexSpikeAgainstSwingIsSlice() {
        List<GestureTrace.Sample> s = upwardFlick(0.1f);
        s.add(new GestureTrace.Sample(0.11f, 0.15f, 0.85f)); // vx=+15 → perp −15
        assertEquals(ShotType.SLICE, classify(s).type());
    }

    @Test
    @DisplayName("Fast flick + perpendicular spike at apex with swing → TOPSPIN")
    void apexSpikeWithSwingIsTopspin() {
        List<GestureTrace.Sample> s = upwardFlick(0.1f);
        s.add(new GestureTrace.Sample(0.11f, -0.15f, 0.85f)); // vx=−15 → perp +15
        assertEquals(ShotType.TOPSPIN, classify(s).type());
    }

    @Test
    @DisplayName("Sloppy fast flick with mild wobble below jerkThreshold → SMASH, not spin")
    void mildWobbleDoesNotFalsePositiveAsSpin() {
        List<GestureTrace.Sample> s = new ArrayList<>();
        for (int i = 0; i <= 10; i++) {
            float t = i * 0.01f;
            float wobble = (i % 2 == 0) ? 0.01f : -0.01f; // perp speed 2 < threshold 3
            s.add(new GestureTrace.Sample(t, wobble, 0.9f * i / 10));
        }
        assertEquals(ShotType.SMASH, classify(s).type());
    }

    @Test
    @DisplayName("Sub-threshold jitter → empty Optional")
    void subThresholdJitterIsNoGesture() {
        List<GestureTrace.Sample> s = new ArrayList<>();
        for (int i = 0; i <= 10; i++) {
            float t = i * 0.01f;
            s.add(new GestureTrace.Sample(t, (i % 2 == 0) ? 0.05f : -0.05f, 0.08f));
        }
        assertTrue(classifier.classify(new GestureTrace(s)).isEmpty());
    }

    @Test
    @DisplayName("Perpendicular spike well AFTER the apex window → no spin")
    void spikeAfterApexWindowIgnored() {
        List<GestureTrace.Sample> s = upwardFlick(0.1f); // apex at t=0.10
        // Stick eases back toward center...
        for (int i = 1; i <= 14; i++) {
            float t = 0.10f + i * 0.01f;
            s.add(new GestureTrace.Sample(t, 0f, 0.9f - 0.025f * i));
        }
        // ...then a hard sideways spike at t=0.25, far outside the 0.06s window.
        s.add(new GestureTrace.Sample(0.25f, 0.15f, 0.55f));
        assertEquals(ShotType.SMASH, classify(s).type());
    }

    @Test
    @DisplayName("Perpendicular spike well BEFORE the apex window → no spin")
    void spikeBeforeApexWindowIgnored() {
        List<GestureTrace.Sample> s = new ArrayList<>();
        for (int i = 0; i <= 10; i++) {
            float t = i * 0.01f;
            float x = (i == 2) ? 0.15f : 0f; // spike at t=0.02, apex at t=0.10
            s.add(new GestureTrace.Sample(t, x, 0.9f * i / 10));
        }
        assertEquals(ShotType.SMASH, classify(s).type());
    }

    @Test
    @DisplayName("Deterministic: same trace + tuning → identical result")
    void deterministic() {
        List<GestureTrace.Sample> s = upwardFlick(0.1f);
        s.add(new GestureTrace.Sample(0.11f, 0.15f, 0.85f));
        GestureTrace trace = new GestureTrace(s);
        assertEquals(classifier.classify(trace), classifier.classify(trace));
    }
}
