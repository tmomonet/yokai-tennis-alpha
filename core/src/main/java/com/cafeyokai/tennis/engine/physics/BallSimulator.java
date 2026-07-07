package com.cafeyokai.tennis.engine.physics;

/**
 * Fixed-timestep 2.5D ball simulation (research R6). Callers feed variable
 * frame deltas into {@link #update}; internally the ball always advances in
 * {@link #FIXED_DT} steps, so identical inputs give identical trajectories
 * regardless of frame pacing.
 */
public final class BallSimulator {

    public static final float FIXED_DT = 1f / 120f;
    /** Arcade gravity, m/s^2 — snappier than Earth's for GBA-style pacing. */
    public static final float GRAVITY = 18f;

    private static final float RESTITUTION = 0.55f;
    private static final float BOUNCE_FRICTION = 0.80f;
    // Spin modifiers (FR-032): slice stays low and skids, topspin kicks up.
    private static final float SLICE_RESTITUTION = 0.30f;
    private static final float SLICE_FRICTION = 0.92f;
    private static final float TOPSPIN_RESTITUTION = 0.78f;
    private static final float TOPSPIN_FRICTION = 0.85f;
    // Net clip response: ball loses nearly all pace and falls on the hitter's side.
    private static final float NET_DAMP = 0.1f;

    private float accumulator;

    /**
     * Advances the ball by {@code frameDt} seconds in fixed steps.
     *
     * @return the number of ground bounces that occurred during this update
     */
    public int update(BallState ball, float frameDt) {
        accumulator += frameDt;
        int bounces = 0;
        while (accumulator >= FIXED_DT) {
            accumulator -= FIXED_DT;
            if (stepOnce(ball)) {
                bounces++;
            }
        }
        return bounces;
    }

    /** Advances exactly one fixed step; returns true if the ball bounced. */
    public boolean stepOnce(BallState ball) {
        float prevY = ball.y;

        ball.x += ball.vx * FIXED_DT;
        ball.y += ball.vy * FIXED_DT;
        ball.z += ball.vz * FIXED_DT;
        ball.vz -= GRAVITY * FIXED_DT;

        // Net clip: crossing y=0 below net height kills the shot (FR point resolution).
        if (!ball.netHit && prevY != 0f && Math.signum(prevY) != Math.signum(ball.y)
                && ball.z < CourtGeometry.NET_HEIGHT) {
            ball.netHit = true;
            ball.y = prevY; // ball rebounds back onto the hitter's side
            ball.vy = -ball.vy * NET_DAMP;
            ball.vx *= NET_DAMP;
        }

        if (ball.z <= 0f && ball.vz < 0f) {
            ball.z = 0f;
            ball.bounces++;
            ball.lastBounceX = ball.x;
            ball.lastBounceY = ball.y;

            float restitution;
            float friction;
            switch (ball.spin) {
                case SLICE -> {
                    restitution = SLICE_RESTITUTION;
                    friction = SLICE_FRICTION;
                }
                case TOPSPIN -> {
                    restitution = TOPSPIN_RESTITUTION;
                    friction = TOPSPIN_FRICTION;
                }
                default -> {
                    restitution = RESTITUTION;
                    friction = BOUNCE_FRICTION;
                }
            }
            ball.vz = -ball.vz * restitution;
            ball.vx *= friction;
            ball.vy *= friction;
            // Spin is spent on the first bounce; later bounces are neutral.
            ball.spin = SpinType.NONE;
            return true;
        }
        return false;
    }
}
