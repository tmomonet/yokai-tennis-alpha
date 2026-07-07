package com.cafeyokai.tennis.engine.physics;

/**
 * Mutable 2.5D ball state (research R6): planar position (x, y) on the court,
 * height z above it, and matching velocities. Bounce bookkeeping supports
 * in/out and double-bounce point resolution.
 */
public final class BallState {

    public float x;
    public float y;
    public float z;
    public float vx;
    public float vy;
    public float vz;

    public SpinType spin = SpinType.NONE;
    /** Player index (0/1) of the last hitter; -1 before the serve. */
    public int lastHitBy = -1;
    /** Bounces since the last hit. */
    public int bounces;
    /** True once the ball has clipped the net on its current flight. */
    public boolean netHit;

    public float lastBounceX;
    public float lastBounceY;

    /** Resets flight bookkeeping when a player strikes the ball. */
    public void onHit(int player, float vx, float vy, float vz, SpinType spin) {
        this.lastHitBy = player;
        this.vx = vx;
        this.vy = vy;
        this.vz = vz;
        this.spin = spin;
        this.bounces = 0;
        this.netHit = false;
    }

    public BallState copy() {
        BallState c = new BallState();
        c.x = x;
        c.y = y;
        c.z = z;
        c.vx = vx;
        c.vy = vy;
        c.vz = vz;
        c.spin = spin;
        c.lastHitBy = lastHitBy;
        c.bounces = bounces;
        c.netHit = netHit;
        c.lastBounceX = lastBounceX;
        c.lastBounceY = lastBounceY;
        return c;
    }
}
