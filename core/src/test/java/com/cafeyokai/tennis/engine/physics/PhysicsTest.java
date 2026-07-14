package com.cafeyokai.tennis.engine.physics;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Physics tests for T010: geometry calls, spin bounces, range radius, determinism. */
class PhysicsTest {

    // ---- Court geometry: in/out ----

    @Test
    @DisplayName("Landings inside the singles lines are in; beyond them are out")
    void inOutCalls() {
        assertTrue(CourtGeometry.isInsideCourt(0f, 0f));
        assertTrue(CourtGeometry.isInsideCourt(4.115f, 11.885f)); // lines are in
        assertTrue(CourtGeometry.isInsideCourt(-4.0f, -11.0f));
        assertFalse(CourtGeometry.isInsideCourt(4.2f, 0f));   // wide
        assertFalse(CourtGeometry.isInsideCourt(0f, 12.0f));  // long
        assertFalse(CourtGeometry.isInsideCourt(-5f, -13f));
    }

    // ---- Court geometry: service boxes ----

    @Test
    @DisplayName("Service box checks respect side, depth, and deuce/ad half")
    void serviceBoxChecks() {
        // Receiver 1 (y>0) faces -y; their right (deuce) court is x<0
        // (real-tennis orientation, fixed 2026-07-14).
        assertTrue(CourtGeometry.isInServiceBox(-2f, 3f, 1, true));
        assertFalse(CourtGeometry.isInServiceBox(2f, 3f, 1, true));
        assertTrue(CourtGeometry.isInServiceBox(2f, 3f, 1, false)); // ad court
        assertFalse(CourtGeometry.isInServiceBox(-2f, 7f, 1, true)); // past service line
        assertFalse(CourtGeometry.isInServiceBox(-2f, -3f, 1, true)); // wrong side of net

        // Receiver 0 (y<0) faces +y; deuce court mirrors to x>0.
        assertTrue(CourtGeometry.isInServiceBox(2f, -3f, 0, true));
        assertFalse(CourtGeometry.isInServiceBox(-2f, -3f, 0, true));
        assertTrue(CourtGeometry.isInServiceBox(-2f, -3f, 0, false));

        // Box centers land inside their own box.
        for (int receiver = 0; receiver <= 1; receiver++) {
            for (boolean deuce : new boolean[] {true, false}) {
                float[] c = CourtGeometry.serviceBoxCenter(receiver, deuce);
                assertTrue(CourtGeometry.isInServiceBox(c[0], c[1], receiver, deuce),
                        "center of box receiver=" + receiver + " deuce=" + deuce);
            }
        }
    }

    // ---- Bounce behavior ----

    private static BallState incomingBall(SpinType spin) {
        BallState b = new BallState();
        b.x = 0f;
        b.y = 3f;
        b.z = 0.5f;
        b.onHit(0, 1f, 8f, -4f, spin);
        return b;
    }

    /** Steps until the first bounce, returns the state right after it. */
    private static BallState bounceOnce(BallState b) {
        BallSimulator sim = new BallSimulator();
        for (int i = 0; i < 1000; i++) {
            if (sim.stepOnce(b)) {
                return b;
            }
        }
        throw new AssertionError("ball never bounced");
    }

    @Test
    @DisplayName("Slice skids low, topspin kicks up relative to a flat bounce")
    void spinChangesBounce() {
        BallState flat = bounceOnce(incomingBall(SpinType.NONE));
        BallState slice = bounceOnce(incomingBall(SpinType.SLICE));
        BallState topspin = bounceOnce(incomingBall(SpinType.TOPSPIN));

        // Vertical: slice stays low, topspin kicks up.
        assertTrue(slice.vz < flat.vz, "slice should bounce lower than flat");
        assertTrue(topspin.vz > flat.vz, "topspin should kick up higher than flat");

        // Planar: slice skids (retains more forward speed).
        assertTrue(slice.vy > flat.vy, "slice should skid through the bounce");
    }

    @Test
    @DisplayName("Bounces accumulate for double-bounce detection and record landing spots")
    void doubleBounceDetection() {
        BallState b = incomingBall(SpinType.NONE);
        BallSimulator sim = new BallSimulator();
        int bounces = 0;
        for (int i = 0; i < 5000 && bounces < 2; i++) {
            if (sim.stepOnce(b)) {
                bounces++;
                assertEquals(b.x, b.lastBounceX);
                assertEquals(b.y, b.lastBounceY);
            }
        }
        assertEquals(2, bounces);
        assertEquals(2, b.bounces);
        // onHit resets the counter for the next rally exchange.
        b.onHit(1, -1f, -8f, 4f, SpinType.NONE);
        assertEquals(0, b.bounces);
    }

    // ---- Range radius + distance-scaled power (FR-033) ----

    @Test
    @DisplayName("Swings outside the range radius miss")
    void rangeRadiusMiss() {
        assertTrue(ShotContact.canReach(0f, 0f, 1.0f, 1.0f));   // dist ~1.41
        assertFalse(ShotContact.canReach(0f, 0f, 2.0f, 1.0f));  // dist ~2.24
        assertEquals(0f, ShotContact.powerScale(0f, 0f, 3f, 0f));
    }

    @Test
    @DisplayName("Shot power scales down with distance: full close in, reduced at the boundary")
    void distanceScaledPower() {
        assertEquals(1f, ShotContact.powerScale(0f, 0f, 0f, 0f), 1e-5);
        float mid = ShotContact.powerScale(0f, 0f, CourtGeometry.RANGE_RADIUS / 2f, 0f);
        float edge = ShotContact.powerScale(0f, 0f, CourtGeometry.RANGE_RADIUS, 0f);
        assertTrue(mid < 1f && mid > edge, "power falls off monotonically");
        assertEquals(ShotContact.MIN_POWER_SCALE, edge, 1e-5);
    }

    // ---- Fixed timestep ----

    @Test
    @DisplayName("Frame pacing does not change the trajectory (fixed timestep)")
    void fixedTimestepDeterminism() {
        BallState a = incomingBall(SpinType.TOPSPIN);
        BallState b = a.copy();

        BallSimulator simA = new BallSimulator();
        BallSimulator simB = new BallSimulator();
        // Same total time, different frame chunking.
        for (int i = 0; i < 120; i++) {
            simA.update(a, 1f / 60f);
        }
        for (int i = 0; i < 60; i++) {
            simB.update(b, 1f / 30f);
        }
        assertEquals(a.x, b.x);
        assertEquals(a.y, b.y);
        assertEquals(a.z, b.z);
        assertEquals(a.vz, b.vz);
        assertEquals(a.bounces, b.bounces);
    }
}
