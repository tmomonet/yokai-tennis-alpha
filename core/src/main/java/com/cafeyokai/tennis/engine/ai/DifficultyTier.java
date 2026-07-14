package com.cafeyokai.tennis.engine.ai;

/**
 * AI difficulty tiers (FR-040..041). Tiers modulate serve speed/placement
 * (Easy: center + slow → Hard: corners + fast, FR-023), reaction time, and
 * how the AI reads spin: Easy ignores it, Medium reacts with delay, Hard
 * anticipates it.
 */
public enum DifficultyTier {

    // Serve speeds lowered from 20/27/34 after playtest 2026-07-14: the human
    // receiver could not physically reach a 27 m/s serve.
    EASY(17f, 0.0f, 1.0f, 0.35f, SpinRead.IGNORE),
    MEDIUM(22f, 0.5f, 0.6f, 0.22f, SpinRead.DELAYED),
    HARD(28f, 0.9f, 0.25f, 0.12f, SpinRead.ANTICIPATE);

    public enum SpinRead { IGNORE, DELAYED, ANTICIPATE }

    /** Serve launch speed, m/s. */
    public final float serveSpeed;
    /** 0 = aim at the box center, 1 = aim at the box corner. */
    public final float serveCornerBias;
    /** Std-dev of placement jitter in meters (serves and rally targets). */
    public final float placementJitter;
    /** Seconds before the AI starts moving to a hit ball. */
    public final float reactionDelay;
    public final SpinRead spinRead;

    DifficultyTier(float serveSpeed, float serveCornerBias, float placementJitter,
                   float reactionDelay, SpinRead spinRead) {
        this.serveSpeed = serveSpeed;
        this.serveCornerBias = serveCornerBias;
        this.placementJitter = placementJitter;
        this.reactionDelay = reactionDelay;
        this.spinRead = spinRead;
    }
}
