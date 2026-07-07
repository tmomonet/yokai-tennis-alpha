package com.cafeyokai.tennis.engine.ai;

import com.cafeyokai.tennis.engine.physics.CourtGeometry;
import com.cafeyokai.tennis.engine.physics.SpinType;

import java.util.Random;

/**
 * Tiered AI decision-making (FR-040..043). Every tier targets empty court
 * space (FR-040); tiers differ in serve speed/placement, reaction time, and
 * spin reading (FR-041). The Random seed is injectable so decisions are
 * reproducible under test.
 */
public final class AiController {

    /** Extra reaction seconds against spin for a tier that reads it DELAYED. */
    public static final float DELAYED_SPIN_PENALTY = 0.25f;
    /** Extra reaction seconds when spin is IGNOREd entirely (never adjusts). */
    public static final float IGNORED_SPIN_PENALTY = 0.5f;

    private final DifficultyTier tier;
    private final Random random;

    public AiController(DifficultyTier tier, long seed) {
        this.tier = tier;
        this.random = new Random(seed);
    }

    public DifficultyTier tier() {
        return tier;
    }

    /** AI serves skip the meter UI; speed comes straight from the tier (FR-023). */
    public float serveSpeed() {
        return tier.serveSpeed;
    }

    /**
     * Serve placement (FR-023): Easy aims at the box center, Hard biases hard
     * toward the outside corner, with tier-scaled jitter. Always inside the box.
     */
    public float[] chooseServeTarget(int receiver, boolean deuceCourt) {
        float[] center = CourtGeometry.serviceBoxCenter(receiver, deuceCourt);
        float cornerX = Math.copySign(CourtGeometry.HALF_WIDTH - 0.45f, center[0]);
        float cornerY = Math.copySign(CourtGeometry.SERVICE_LINE - 0.45f, center[1]);

        float x = lerp(center[0], cornerX, tier.serveCornerBias)
                + (float) random.nextGaussian() * tier.placementJitter * 0.5f;
        float y = lerp(center[1], cornerY, tier.serveCornerBias)
                + (float) random.nextGaussian() * tier.placementJitter * 0.5f;

        // Clamp back into the box interior so jitter can never aim a guaranteed fault.
        float sign = CourtGeometry.sideSign(receiver);
        float lateral = clamp(x * sign * (deuceCourt ? 1f : -1f),
                0.35f, CourtGeometry.HALF_WIDTH - 0.35f);
        x = lateral * (deuceCourt ? 1f : -1f) * sign;
        y = clamp(y * sign, 0.35f, CourtGeometry.SERVICE_LINE - 0.35f) * sign;
        return new float[] {x, y};
    }

    /**
     * Rally shot placement: hit into the empty half of the opponent's court,
     * i.e. away from where the opponent currently stands (FR-040), aimed deep.
     */
    public float[] chooseShotTarget(float opponentX, int opponentSide) {
        float sign = CourtGeometry.sideSign(opponentSide);
        float emptySide = opponentX >= 0f ? -1f : 1f;
        float x = emptySide * CourtGeometry.HALF_WIDTH * 0.6f
                + (float) random.nextGaussian() * tier.placementJitter * 0.5f;
        float y = sign * CourtGeometry.HALF_LENGTH * 0.72f
                + (float) random.nextGaussian() * tier.placementJitter * 0.5f * sign;

        x = clamp(x, -(CourtGeometry.HALF_WIDTH - 0.3f), CourtGeometry.HALF_WIDTH - 0.3f);
        y = clamp(y * sign, 1.5f, CourtGeometry.HALF_LENGTH - 0.5f) * sign;
        return new float[] {x, y};
    }

    /**
     * Seconds before the AI moves to intercept a ball carrying {@code spin}
     * (FR-041): Hard anticipates (no penalty), Medium reacts late, Easy never
     * adjusts and pays the largest effective penalty.
     */
    public float effectiveReactionDelay(SpinType spin) {
        float delay = tier.reactionDelay;
        if (spin != SpinType.NONE) {
            switch (tier.spinRead) {
                case IGNORE -> delay += IGNORED_SPIN_PENALTY;
                case DELAYED -> delay += DELAYED_SPIN_PENALTY;
                case ANTICIPATE -> {
                }
            }
        }
        return delay;
    }

    private static float lerp(float a, float b, float t) {
        return a + (b - a) * t;
    }

    private static float clamp(float v, float lo, float hi) {
        return Math.max(lo, Math.min(hi, v));
    }
}
