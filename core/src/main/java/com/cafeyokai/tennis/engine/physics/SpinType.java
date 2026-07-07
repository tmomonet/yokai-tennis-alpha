package com.cafeyokai.tennis.engine.physics;

/** Ball spin state affecting bounce behavior (FR-032). */
public enum SpinType {
    NONE,
    /** Stays low and skids after the bounce. */
    SLICE,
    /** Kicks up after the bounce. */
    TOPSPIN
}
