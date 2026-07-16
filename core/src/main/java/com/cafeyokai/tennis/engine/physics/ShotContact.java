package com.cafeyokai.tennis.engine.physics;

/**
 * FR-033: a swing only connects within {@link CourtGeometry#RANGE_RADIUS} of
 * the ball (no auto-teleport), and shot power scales with distance at
 * contact — full power close in, reduced near the range boundary.
 */
public final class ShotContact {

    /** Power multiplier at the very edge of the range radius. */
    public static final float MIN_POWER_SCALE = 0.4f;
    /** Contact within this distance is "clean" and earns the sweet bonus. */
    public static final float SWEET_RADIUS = 0.8f;
    /** Power multiplier for clean (sweet-spot) contact. */
    public static final float SWEET_BONUS = 1.15f;

    private ShotContact() {
    }

    public static float distance(float playerX, float playerY, float ballX, float ballY) {
        float dx = ballX - playerX;
        float dy = ballY - playerY;
        return (float) Math.sqrt(dx * dx + dy * dy);
    }

    public static boolean canReach(float playerX, float playerY, float ballX, float ballY) {
        return distance(playerX, playerY, ballX, ballY) <= CourtGeometry.RANGE_RADIUS;
    }

    /**
     * Clean contact inside {@link #SWEET_RADIUS} earns {@link #SWEET_BONUS};
     * beyond that, linear falloff to {@link #MIN_POWER_SCALE} at the range
     * boundary; 0 past it (the swing misses).
     */
    public static float powerScale(float playerX, float playerY, float ballX, float ballY) {
        float d = distance(playerX, playerY, ballX, ballY);
        if (d > CourtGeometry.RANGE_RADIUS) {
            return 0f;
        }
        if (d <= SWEET_RADIUS) {
            return SWEET_BONUS;
        }
        float t = (d - SWEET_RADIUS) / (CourtGeometry.RANGE_RADIUS - SWEET_RADIUS);
        return 1f - (1f - MIN_POWER_SCALE) * t;
    }
}
