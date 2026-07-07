package com.cafeyokai.tennis.engine.gesture;

/**
 * Single source of truth for gesture thresholds (contract: retuning must not
 * require classifier code changes). Units: stick displacement is [-1,1] per
 * axis; speeds are displacement units per second.
 *
 * @param smashThreshold   peak radial speed at/above which a flick is a SMASH
 * @param jerkThreshold    perpendicular speed a spike must exceed to read as spin
 * @param apexWindowSeconds half-width of the time window around the apex in
 *                          which a perpendicular spike counts
 * @param maxSpeed         radial speed mapping to power 1.0
 * @param minDisplacement  peak displacement below which the trace is noise
 *                         (no gesture)
 */
public record GestureTuning(
        float smashThreshold,
        float jerkThreshold,
        float apexWindowSeconds,
        float maxSpeed,
        float minDisplacement) {

    /** Starting values; refined during the T026 tuning pass. */
    public static GestureTuning defaults() {
        return new GestureTuning(4.0f, 3.0f, 0.06f, 9.0f, 0.35f);
    }
}
