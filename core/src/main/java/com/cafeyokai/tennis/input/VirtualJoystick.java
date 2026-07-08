package com.cafeyokai.tennis.input;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.cafeyokai.tennis.gfx.SpriteProvider;

/**
 * Polling-based virtual joystick for touchscreen input (T020, FR-020).
 *
 * All coordinates are in the 1280x720 virtual pixel space. The joystick
 * captures a pointer on touch-down if the touch is within its radius, then
 * tracks the knob clamped to the unit circle times radius until touch-up.
 */
public final class VirtualJoystick {

    private final float centerX;
    private final float centerY;
    private final float radius;

    private float knobX;
    private float knobY;
    private boolean active;
    private int pointerId = -1;

    public VirtualJoystick(float centerX, float centerY, float radius) {
        this.centerX = centerX;
        this.centerY = centerY;
        this.radius = radius;
    }

    /**
     * Called on each touch-down event. Returns true and captures the pointer
     * if the touch is within the joystick's radius.
     */
    public boolean touchDown(int pointer, float virtX, float virtY) {
        if (active) {
            return false; // already captured
        }
        float dx = virtX - centerX;
        float dy = virtY - centerY;
        if (Math.sqrt(dx * dx + dy * dy) <= radius) {
            active = true;
            pointerId = pointer;
            knobX = 0f;
            knobY = 0f;
            return true;
        }
        return false;
    }

    /**
     * Called each frame for each active touch. Updates the knob position,
     * clamped to the unit circle times radius.
     */
    public void touchDragged(int pointer, float virtX, float virtY) {
        if (!active || pointer != pointerId) {
            return;
        }
        float dx = virtX - centerX;
        float dy = virtY - centerY;
        float mag = (float) Math.sqrt(dx * dx + dy * dy);
        if (mag > radius) {
            dx = dx / mag * radius;
            dy = dy / mag * radius;
        }
        knobX = dx;
        knobY = dy;
    }

    /** Called on touch-up; releases capture if the pointer matches. */
    public void touchUp(int pointer) {
        if (active && pointer == pointerId) {
            active = false;
            pointerId = -1;
            knobX = 0f;
            knobY = 0f;
        }
    }

    /** Knob X displacement from center in [-radius, radius], 0 when inactive. */
    public float getKnobX() {
        return active ? knobX : 0f;
    }

    /** Knob Y displacement from center in [-radius, radius], 0 when inactive. */
    public float getKnobY() {
        return active ? knobY : 0f;
    }

    public boolean isActive() {
        return active;
    }

    public float getCenterX() {
        return centerX;
    }

    public float getCenterY() {
        return centerY;
    }

    public float getRadius() {
        return radius;
    }

    public int getPointerId() {
        return pointerId;
    }

    /**
     * Draws the joystick base and knob sprites centered at their positions.
     * Assumes the batch is already begun.
     */
    public void render(SpriteBatch batch, SpriteProvider sprites) {
        TextureRegion base = sprites.byKey("joystick.base");
        TextureRegion knob = sprites.byKey("joystick.knob");

        float baseSize = radius * 2f;
        batch.draw(base,
                centerX - baseSize / 2f,
                centerY - baseSize / 2f,
                baseSize, baseSize);

        float knobSize = radius * 0.9f;
        float kx = centerX + (active ? knobX : 0f);
        float ky = centerY + (active ? knobY : 0f);
        batch.draw(knob,
                kx - knobSize / 2f,
                ky - knobSize / 2f,
                knobSize, knobSize);
    }
}
