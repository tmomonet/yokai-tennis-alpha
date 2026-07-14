package com.cafeyokai.tennis.engine.ai;

import com.cafeyokai.tennis.engine.physics.CourtGeometry;
import com.cafeyokai.tennis.engine.physics.SpinType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Seeded-determinism AI tests for T014 (FR-040..043). */
class AiControllerTest {

    private static final long SEED = 42L;

    @Test
    @DisplayName("Rally shots target the empty half, away from the opponent (FR-040)")
    void targetsEmptyHalf() {
        AiController ai = new AiController(DifficultyTier.MEDIUM, SEED);
        for (int i = 0; i < 100; i++) {
            float[] left = ai.chooseShotTarget(2.5f, 0); // opponent right → aim left
            assertTrue(left[0] < 0f, "opponent on +x must be answered into -x");
            assertTrue(CourtGeometry.isInsideCourt(left[0], left[1]));
            assertTrue(CourtGeometry.isOnPlayerSide(left[0], left[1], 0));

            float[] right = ai.chooseShotTarget(-2.5f, 1);
            assertTrue(right[0] > 0f, "opponent on -x must be answered into +x");
            assertTrue(CourtGeometry.isOnPlayerSide(right[0], right[1], 1));
        }
    }

    @Test
    @DisplayName("Serve placement: Easy hugs the box center, Hard hunts the corner, all in the box")
    void servePlacementSpreadPerTier() {
        AiController easy = new AiController(DifficultyTier.EASY, SEED);
        AiController hard = new AiController(DifficultyTier.HARD, SEED);
        float[] center = CourtGeometry.serviceBoxCenter(1, true);
        // Outside corner of the deuce box, on the same lateral side as its center
        float corner = Math.copySign(CourtGeometry.HALF_WIDTH - 0.45f, center[0]);

        float easyCenterDist = 0f;
        float hardCornerDist = 0f;
        int n = 200;
        for (int i = 0; i < n; i++) {
            float[] e = easy.chooseServeTarget(1, true);
            float[] h = hard.chooseServeTarget(1, true);
            assertTrue(CourtGeometry.isInServiceBox(e[0], e[1], 1, true), "easy serve in box");
            assertTrue(CourtGeometry.isInServiceBox(h[0], h[1], 1, true), "hard serve in box");
            easyCenterDist += dist(e[0], e[1], center[0], center[1]);
            hardCornerDist += dist(h[0], h[1], corner, CourtGeometry.SERVICE_LINE - 0.45f);
        }
        easyCenterDist /= n;
        hardCornerDist /= n;
        assertTrue(easyCenterDist < 1.2f, "Easy stays near center, got mean " + easyCenterDist);
        assertTrue(hardCornerDist < 1.2f, "Hard stays near corner, got mean " + hardCornerDist);
    }

    @Test
    @DisplayName("Serve speed rises with tier (Easy slow → Hard fast, FR-023)")
    void serveSpeedPerTier() {
        assertTrue(DifficultyTier.EASY.serveSpeed < DifficultyTier.MEDIUM.serveSpeed);
        assertTrue(DifficultyTier.MEDIUM.serveSpeed < DifficultyTier.HARD.serveSpeed);
        assertEquals(DifficultyTier.MEDIUM.serveSpeed,
                new AiController(DifficultyTier.MEDIUM, SEED).serveSpeed());
    }

    @Test
    @DisplayName("Spin reading: Easy ignores (worst), Medium delayed, Hard anticipates (FR-041)")
    void spinReadDifferencesObservable() {
        AiController easy = new AiController(DifficultyTier.EASY, SEED);
        AiController medium = new AiController(DifficultyTier.MEDIUM, SEED);
        AiController hard = new AiController(DifficultyTier.HARD, SEED);

        // Reaction baseline improves with tier.
        assertTrue(easy.effectiveReactionDelay(SpinType.NONE)
                > medium.effectiveReactionDelay(SpinType.NONE));
        assertTrue(medium.effectiveReactionDelay(SpinType.NONE)
                > hard.effectiveReactionDelay(SpinType.NONE));

        // Spin penalty per read style: IGNORE > DELAYED > ANTICIPATE (none).
        float easyPenalty = easy.effectiveReactionDelay(SpinType.SLICE)
                - easy.effectiveReactionDelay(SpinType.NONE);
        float mediumPenalty = medium.effectiveReactionDelay(SpinType.TOPSPIN)
                - medium.effectiveReactionDelay(SpinType.NONE);
        float hardPenalty = hard.effectiveReactionDelay(SpinType.SLICE)
                - hard.effectiveReactionDelay(SpinType.NONE);
        assertEquals(AiController.IGNORED_SPIN_PENALTY, easyPenalty, 1e-5);
        assertEquals(AiController.DELAYED_SPIN_PENALTY, mediumPenalty, 1e-5);
        assertEquals(0f, hardPenalty, 1e-5);
    }

    @Test
    @DisplayName("Same seed → identical decision sequence (deterministic under test)")
    void seededDeterminism() {
        AiController a = new AiController(DifficultyTier.HARD, 1234L);
        AiController b = new AiController(DifficultyTier.HARD, 1234L);
        for (int i = 0; i < 20; i++) {
            assertArrayEquals(a.chooseServeTarget(0, i % 2 == 0), b.chooseServeTarget(0, i % 2 == 0));
            assertArrayEquals(a.chooseShotTarget(1.0f, 1), b.chooseShotTarget(1.0f, 1));
        }
    }

    private static float dist(float x1, float y1, float x2, float y2) {
        float dx = x2 - x1;
        float dy = y2 - y1;
        return (float) Math.sqrt(dx * dx + dy * dy);
    }
}
