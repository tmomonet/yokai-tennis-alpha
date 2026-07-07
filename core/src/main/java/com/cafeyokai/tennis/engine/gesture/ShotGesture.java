package com.cafeyokai.tennis.engine.gesture;

/**
 * A classified shot gesture: type, normalized aim direction, and power in [0,1].
 */
public record ShotGesture(ShotType type, float directionX, float directionY, float power) {
}
