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
    // Reactions/pace/jitter tightened across all tiers in T026 round 7
    // (playtest: "still needs to be harder").
    EASY(17f, 0.0f, 0.9f, 0.32f, 0.78f, 2.2f, SpinRead.IGNORE),
    MEDIUM(22f, 0.5f, 0.5f, 0.15f, 1.00f, 1.4f, SpinRead.DELAYED),
    HARD(28f, 0.9f, 0.20f, 0.08f, 1.18f, 0.45f, SpinRead.ANTICIPATE);

    public enum SpinRead { IGNORE, DELAYED, ANTICIPATE }

    /** Serve launch speed, m/s. */
    public final float serveSpeed;
    /** 0 = aim at the box center, 1 = aim at the box corner. */
    public final float serveCornerBias;
    /** Std-dev of placement jitter in meters (serves and rally targets). */
    public final float placementJitter;
    /** Seconds before the AI starts moving to a hit ball. */
    public final float reactionDelay;
    /** Rally shot speed as a fraction of the base pace (T026 round 6). */
    public final float rallyPace;
    /** Std-dev (m) of the AI's initial landing-spot misread (T026 round 8):
     *  it commits to the wrong spot until the ball crosses the net, so
     *  wrong-footing it wins points. Hard barely misreads. */
    public final float anticipationError;
    public final SpinRead spinRead;

    DifficultyTier(float serveSpeed, float serveCornerBias, float placementJitter,
                   float reactionDelay, float rallyPace, float anticipationError,
                   SpinRead spinRead) {
        this.serveSpeed = serveSpeed;
        this.serveCornerBias = serveCornerBias;
        this.placementJitter = placementJitter;
        this.reactionDelay = reactionDelay;
        this.rallyPace = rallyPace;
        this.anticipationError = anticipationError;
        this.spinRead = spinRead;
    }
}
