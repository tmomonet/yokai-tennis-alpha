package com.cafeyokai.tennis.engine.physics;

/**
 * Court constants and geometry checks, in meters, real singles-court
 * proportions. Coordinates: x across the court, y along it; the net is the
 * line y = 0. Player 0 occupies y &lt; 0, player 1 occupies y &gt; 0.
 *
 * Used by physics (in/out), serve fault checks (service boxes), AI targeting,
 * and the FR-033 range-radius rule.
 */
public final class CourtGeometry {

    /** Half of the 8.23 m singles-court width. */
    public static final float HALF_WIDTH = 4.115f;
    /** Half of the 23.77 m court length. */
    public static final float HALF_LENGTH = 11.885f;
    /** Service line distance from the net. */
    public static final float SERVICE_LINE = 6.40f;
    /** Net height at center. */
    public static final float NET_HEIGHT = 0.91f;
    /** FR-033: max distance from the ball at which a swing connects. */
    public static final float RANGE_RADIUS = 1.8f;

    private CourtGeometry() {
    }

    /** True if (x, y) is on or inside the singles lines (lines are in). */
    public static boolean isInsideCourt(float x, float y) {
        return Math.abs(x) <= HALF_WIDTH && Math.abs(y) <= HALF_LENGTH;
    }

    /** True if (x, y) is on the given player's half (net line belongs to neither). */
    public static boolean isOnPlayerSide(float x, float y, int player) {
        return player == 0 ? y < 0f : y > 0f;
    }

    /** Sign of a player's half of the court: -1 for player 0, +1 for player 1. */
    public static float sideSign(int player) {
        return player == 0 ? -1f : 1f;
    }

    /**
     * True if (x, y) lands in the receiver's deuce or ad service box.
     * The deuce court is the receiver's right-hand box as they face the net.
     */
    public static boolean isInServiceBox(float x, float y, int receiver, boolean deuceCourt) {
        float sign = sideSign(receiver);
        float depth = y * sign; // distance past the net into the receiver's half
        if (depth < 0f || depth > SERVICE_LINE) {
            return false;
        }
        // Receiver at +y faces -y: their right is -x. Receiver at -y faces +y:
        // their right is +x. (Fixed 2026-07-14 — was mirrored vs real tennis.)
        boolean rightHalf = x * sign <= 0f;
        return deuceCourt == rightHalf && Math.abs(x) <= HALF_WIDTH;
    }

    /** Center point of a service box, for aim defaults and AI serve targeting. */
    public static float[] serviceBoxCenter(int receiver, boolean deuceCourt) {
        float sign = sideSign(receiver);
        float x = (deuceCourt ? -1f : 1f) * sign * HALF_WIDTH / 2f;
        float y = sign * SERVICE_LINE / 2f;
        return new float[] {x, y};
    }
}
