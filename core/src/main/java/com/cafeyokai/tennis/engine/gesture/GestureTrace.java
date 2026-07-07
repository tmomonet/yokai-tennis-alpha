package com.cafeyokai.tennis.engine.gesture;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * An ordered series of timestamped right-stick displacement samples in
 * [-1,1] per axis, from gesture start to stick release.
 */
public final class GestureTrace {

    /** One stick sample: time in seconds (monotonic within the trace) and displacement. */
    public record Sample(float tSeconds, float x, float y) {
        public float magnitude() {
            return (float) Math.sqrt(x * x + y * y);
        }
    }

    private final List<Sample> samples;

    public GestureTrace(List<Sample> samples) {
        this.samples = Collections.unmodifiableList(new ArrayList<>(samples));
    }

    public List<Sample> samples() {
        return samples;
    }
}
