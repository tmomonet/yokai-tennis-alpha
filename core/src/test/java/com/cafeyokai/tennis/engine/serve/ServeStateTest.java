package com.cafeyokai.tennis.engine.serve;

import com.cafeyokai.tennis.engine.physics.CourtGeometry;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** ServeState tests for T012: FR-020..023 mechanics. */
class ServeStateTest {

    /** Advances through POWER to lock roughly the requested power. */
    private static void lockPower(ServeState s, float targetPower) {
        assertEquals(ServeState.Phase.POWER, s.phase());
        float seconds = targetPower / ServeState.POWER_FILL_RATE;
        for (float t = 0; t < seconds; t += 0.001f) {
            s.update(0.001f);
        }
        s.tap();
        assertEquals(ServeState.Phase.ACCURACY, s.phase());
    }

    @Test
    @DisplayName("Meter phases run in order: AIMING → POWER → ACCURACY → LAUNCHED")
    void phaseOrdering() {
        ServeState s = new ServeState(0, true);
        assertEquals(ServeState.Phase.AIMING, s.phase());
        s.tap();
        assertEquals(ServeState.Phase.POWER, s.phase());
        s.tap();
        assertEquals(ServeState.Phase.ACCURACY, s.phase());
        s.tap();
        assertEquals(ServeState.Phase.LAUNCHED, s.phase());
    }

    @Test
    @DisplayName("Aim window expiry counts as a fault and resets for a second serve")
    void aimWindowExpiryIsFault() {
        ServeState s = new ServeState(0, true);
        s.update(ServeState.AIM_WINDOW_SECONDS + 0.1f);
        assertEquals(1, s.faultCount());
        assertTrue(s.isSecondServe());
        assertEquals(ServeState.Phase.AIMING, s.phase());
        assertEquals(ServeState.AIM_WINDOW_SECONDS, s.aimTimeRemaining(), 1e-4);
    }

    @Test
    @DisplayName("Two consecutive faults are a double fault; the point goes to the receiver")
    void doubleFaultAwardsReceiver() {
        ServeState s = new ServeState(1, false);
        s.update(ServeState.AIM_WINDOW_SECONDS + 1f); // first fault
        s.update(ServeState.AIM_WINDOW_SECONDS + 1f); // second fault
        assertTrue(s.isDoubleFault());
        assertEquals(ServeState.Phase.DOUBLE_FAULT, s.phase());
        // Receiver of server 1 is player 0 — the caller awards them the point.
        assertEquals(0, s.receiver());
    }

    @Test
    @DisplayName("Aim indicator is clamped inside the valid service box")
    void aimClampedToServiceBox() {
        ServeState s = new ServeState(0, true); // receiver 1, deuce box: x>0, 0<y<6.4
        s.setAim(50f, 50f);
        assertTrue(CourtGeometry.isInServiceBox(s.aimX(), s.aimY(), 1, true));
        s.setAim(-50f, -50f);
        assertTrue(CourtGeometry.isInServiceBox(s.aimX(), s.aimY(), 1, true));
    }

    @Test
    @DisplayName("Centered needle lands the serve exactly on the aim point")
    void centeredNeedleLandsOnAim() {
        ServeState s = new ServeState(0, true);
        s.tap(); // confirm default center aim
        lockPower(s, 0.5f);
        // Needle starts at -1 and reaches center after a quarter period.
        s.update(1f / (4f * ServeState.NEEDLE_HZ));
        assertEquals(0f, s.accuracyNeedle(), 1e-3);
        s.tap();
        assertEquals(s.aimX(), s.landingX(), 0.02f);
        assertEquals(s.aimY(), s.landingY(), 1e-4);
        assertFalse(s.isFaultLanding());
    }

    @Test
    @DisplayName("Fault margin grows with power: same needle miss drifts further on a hard serve")
    void higherPowerLargerDeviation() {
        // Full needle deflection (-1, tap immediately) at low power: stays in the box.
        ServeState soft = new ServeState(0, true);
        soft.tap();
        lockPower(soft, 0.1f);
        soft.tap(); // needle at -1
        float softDrift = Math.abs(soft.landingX() - soft.aimX());
        assertFalse(soft.isFaultLanding(), "soft serve with full miss should stay in");

        // Same full deflection at high power: drifts further and faults.
        ServeState hard = new ServeState(0, true);
        hard.tap();
        lockPower(hard, 0.98f);
        hard.tap(); // needle at -1
        float hardDrift = Math.abs(hard.landingX() - hard.aimX());
        assertTrue(hardDrift > softDrift, "high power must amplify the needle miss");
        assertTrue(hard.isFaultLanding(), "hard serve with full miss should fault");
    }

    @Test
    @DisplayName("Serve speed scales with locked power")
    void serveSpeedScalesWithPower() {
        ServeState soft = new ServeState(0, true);
        soft.tap();
        soft.tap(); // lock power immediately at 0
        soft.update(1f / (4f * ServeState.NEEDLE_HZ));
        soft.tap();
        assertEquals(ServeState.MIN_SPEED, soft.serveSpeed(), 0.5f);

        ServeState hard = new ServeState(0, true);
        hard.tap();
        lockPower(hard, 0.99f);
        hard.tap();
        assertTrue(hard.serveSpeed() > ServeState.MIN_SPEED + 0.9f * (ServeState.MAX_SPEED - ServeState.MIN_SPEED));
    }

    @Test
    @DisplayName("Power meter ping-pongs within [0,1] instead of sticking or overflowing")
    void powerMeterOscillates() {
        ServeState s = new ServeState(0, true);
        s.tap();
        float peak = 0f;
        float valley = 1f;
        for (int i = 0; i < 3000; i++) { // 3 s — several full cycles
            s.update(0.001f);
            peak = Math.max(peak, s.powerMeter());
            valley = Math.min(valley, s.powerMeter());
            assertTrue(s.powerMeter() >= 0f && s.powerMeter() <= 1f);
        }
        assertTrue(peak > 0.95f, "meter should reach the top");
        assertTrue(valley < 0.05f, "meter should come back down");
    }
}
