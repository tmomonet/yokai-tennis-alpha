package com.cafeyokai.tennis.input;

import com.badlogic.gdx.Application.ApplicationType;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.viewport.Viewport;
import com.cafeyokai.tennis.engine.gesture.GestureTrace;
import com.cafeyokai.tennis.engine.gesture.ShotType;
import com.cafeyokai.tennis.gfx.SpriteProvider;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Platform-aware input aggregator for the match screen (T020).
 *
 * Polled every frame — no InputProcessor. Desktop: WASD for movement, arrow
 * keys are hold-to-charge shot buttons (UP smash, DOWN lob, LEFT slice,
 * RIGHT topspin — hold to charge, release to swing), any non-movement key or
 * click release = meter tap; left-click drag gestures remain as a fallback.
 * Android: left VirtualJoystick for movement, right VirtualJoystick drag for
 * gesture samples, uncaptured touch = meter tap.
 */
public final class MatchInput {

    private static final float GESTURE_SCALE = 150f; // pixels → normalized [-1,1]
    private static final int MAX_POINTERS = 5;

    private final boolean touchMode;

    // Touch-mode sticks (left = move, right = gesture)
    private final VirtualJoystick leftStick;
    private final VirtualJoystick rightStick;

    // Gesture state
    private List<GestureTrace.Sample> gestureAccum;
    private GestureTrace sealedGesture;

    // Shared timing
    private float matchTime;

    // Desktop edge-detection
    private boolean prevMousePressed;
    private Vector2 mousePressPos;

    // Touch edge-detection
    private final boolean[] prevTouched = new boolean[MAX_POINTERS];

    // Events fired this frame
    private boolean tapFired;
    private boolean meterTapFired;
    private ShotType shotKeyFired;
    private float shotChargeFired;

    // Hold-to-charge state (desktop): arrow held = charging, release = swing
    private static final float CHARGE_FULL_SECONDS = 0.8f;
    private ShotType chargingType;
    private float chargeTime;

    // Desktop movement
    private float desktopMoveX;
    private float desktopMoveY;

    private final Vector2 tmpVec = new Vector2();

    public MatchInput() {
        touchMode = Gdx.app.getType() == ApplicationType.Android;
        if (touchMode) {
            leftStick  = new VirtualJoystick(170f, 170f, 110f);
            rightStick = new VirtualJoystick(1110f, 170f, 110f);
        } else {
            leftStick  = null;
            rightStick = null;
        }
    }

    /** Called once per frame before consuming any events. */
    public void update(float delta, Viewport viewport) {
        matchTime += delta;
        tapFired = false;
        meterTapFired = false;
        shotKeyFired = null;
        if (touchMode) {
            updateTouch(viewport);
        } else {
            updateDesktop(delta, viewport);
        }
    }

    // -------------------------------------------------------------------------
    // Desktop input
    // -------------------------------------------------------------------------

    private static int keyFor(ShotType type) {
        return switch (type) {
            case SMASH   -> Input.Keys.UP;
            case LOB     -> Input.Keys.DOWN;
            case SLICE   -> Input.Keys.LEFT;
            case TOPSPIN -> Input.Keys.RIGHT;
        };
    }

    private void updateDesktop(float delta, Viewport viewport) {
        // --- Movement: WASD only (arrows are shot keys) ---
        float mx = 0f, my = 0f;
        if (Gdx.input.isKeyPressed(Input.Keys.A)) mx -= 1f;
        if (Gdx.input.isKeyPressed(Input.Keys.D)) mx += 1f;
        if (Gdx.input.isKeyPressed(Input.Keys.W)) my += 1f;
        if (Gdx.input.isKeyPressed(Input.Keys.S)) my -= 1f;
        // Normalize diagonal
        if (mx != 0f && my != 0f) {
            float inv = 1f / (float) Math.sqrt(2.0);
            mx *= inv;
            my *= inv;
        }
        desktopMoveX = mx;
        desktopMoveY = my;

        // --- Shot keys: hold an arrow to charge, release to swing ---
        if (chargingType == null) {
            if      (Gdx.input.isKeyJustPressed(Input.Keys.UP))    chargingType = ShotType.SMASH;
            else if (Gdx.input.isKeyJustPressed(Input.Keys.DOWN))  chargingType = ShotType.LOB;
            else if (Gdx.input.isKeyJustPressed(Input.Keys.LEFT))  chargingType = ShotType.SLICE;
            else if (Gdx.input.isKeyJustPressed(Input.Keys.RIGHT)) chargingType = ShotType.TOPSPIN;
            if (chargingType != null) {
                chargeTime = 0f;
            }
        } else {
            chargeTime += delta;
            if (!Gdx.input.isKeyPressed(keyFor(chargingType))) {
                // Release = swing with the accumulated charge
                shotKeyFired = chargingType;
                shotChargeFired = Math.min(1f, chargeTime / CHARGE_FULL_SECONDS);
                chargingType = null;
            }
        }

        // --- Meter tap: any key except movement keys ---
        if (Gdx.input.isKeyJustPressed(Input.Keys.ANY_KEY)
                && !Gdx.input.isKeyJustPressed(Input.Keys.W)
                && !Gdx.input.isKeyJustPressed(Input.Keys.A)
                && !Gdx.input.isKeyJustPressed(Input.Keys.S)
                && !Gdx.input.isKeyJustPressed(Input.Keys.D)) {
            meterTapFired = true;
        }

        // --- Gesture / meter from left mouse button ---
        boolean mousePressed = Gdx.input.isButtonPressed(Input.Buttons.LEFT);

        if (mousePressed && !prevMousePressed) {
            // Press edge: start gesture recording
            mousePressPos = unprojectMouse(viewport);
            gestureAccum = new ArrayList<>();
        }

        if (mousePressed && gestureAccum != null) {
            // Sample displacement from press origin
            Vector2 current = unprojectMouse(viewport);
            float dx = (current.x - mousePressPos.x) / GESTURE_SCALE;
            float dy = (current.y - mousePressPos.y) / GESTURE_SCALE;
            float mag = (float) Math.sqrt(dx * dx + dy * dy);
            if (mag > 1f) {
                dx /= mag;
                dy /= mag;
            }
            gestureAccum.add(new GestureTrace.Sample(matchTime, dx, dy));
        }

        if (!mousePressed && prevMousePressed) {
            // Release edge: seal gesture if we collected enough samples
            if (gestureAccum != null && gestureAccum.size() >= 2) {
                sealedGesture = new GestureTrace(gestureAccum);
            }
            gestureAccum = null;
            tapFired = true;
            meterTapFired = true;
        }

        prevMousePressed = mousePressed;
    }

    // -------------------------------------------------------------------------
    // Touch input (Android)
    // -------------------------------------------------------------------------

    private void updateTouch(Viewport viewport) {
        for (int p = 0; p < MAX_POINTERS; p++) {
            boolean touched = Gdx.input.isTouched(p);
            float vx = 0f, vy = 0f;
            if (touched) {
                Vector2 proj = unprojectPointer(p, viewport);
                vx = proj.x;
                vy = proj.y;
            }

            if (touched && !prevTouched[p]) {
                // Touch-down edge: offer to sticks
                boolean capturedLeft  = leftStick.touchDown(p, vx, vy);
                boolean capturedRight = !capturedLeft && rightStick.touchDown(p, vx, vy);
                if (capturedRight) {
                    // Start gesture recording for this pointer
                    gestureAccum = new ArrayList<>();
                }
                // uncaptured touch — will fire meter tap on release
            }

            if (touched) {
                // Drag: update whichever stick owns this pointer
                leftStick.touchDragged(p, vx, vy);
                rightStick.touchDragged(p, vx, vy);

                // If this pointer is the right stick, sample the gesture
                if (rightStick.isActive() && rightStick.getPointerId() == p && gestureAccum != null) {
                    float dx = rightStick.getKnobX() / rightStick.getRadius();
                    float dy = rightStick.getKnobY() / rightStick.getRadius();
                    gestureAccum.add(new GestureTrace.Sample(matchTime, dx, dy));
                }
            }

            if (!touched && prevTouched[p]) {
                // Touch-up edge
                boolean wasRightStick = rightStick.isActive() && rightStick.getPointerId() == p;
                boolean wasEitherStick = (leftStick.isActive() && leftStick.getPointerId() == p) || wasRightStick;

                if (wasRightStick && gestureAccum != null && gestureAccum.size() >= 2) {
                    sealedGesture = new GestureTrace(gestureAccum);
                }
                gestureAccum = null;

                leftStick.touchUp(p);
                rightStick.touchUp(p);

                if (!wasEitherStick) {
                    // Uncaptured touch = meter tap
                    tapFired = true;
                    meterTapFired = true;
                }
            }

            prevTouched[p] = touched;
        }
    }

    // -------------------------------------------------------------------------
    // Public polling API
    // -------------------------------------------------------------------------

    /** Normalized horizontal movement in [-1, 1]. */
    public float moveX() {
        if (touchMode) {
            float r = leftStick.getRadius();
            return r > 0f ? leftStick.getKnobX() / r : 0f;
        }
        return desktopMoveX;
    }

    /** Normalized vertical movement in [-1, 1]. */
    public float moveY() {
        if (touchMode) {
            float r = leftStick.getRadius();
            return r > 0f ? leftStick.getKnobY() / r : 0f;
        }
        return desktopMoveY;
    }

    /**
     * Returns and clears the shot type released this frame (desktop only).
     * Returns Optional.empty() otherwise. {@link #shotCharge()} holds the
     * charge level of the returned shot.
     */
    public Optional<ShotType> pollShotKey() {
        ShotType t = shotKeyFired;
        shotKeyFired = null;
        return Optional.ofNullable(t);
    }

    /** Charge level [0,1] of the shot last returned by {@link #pollShotKey()}. */
    public float shotCharge() {
        return shotChargeFired;
    }

    /** Shot type currently being charged (arrow held), or null. */
    public ShotType chargingShot() {
        return chargingType;
    }

    /** Charge level [0,1] of the in-progress hold; 0 when not charging. */
    public float chargeLevel() {
        return chargingType == null ? 0f : Math.min(1f, chargeTime / CHARGE_FULL_SECONDS);
    }

    /** True when running with touch controls (Android). */
    public boolean isTouchMode() {
        return touchMode;
    }

    /**
     * Returns and clears the sealed gesture trace if one completed this frame.
     * Returns Optional.empty() otherwise.
     */
    public Optional<GestureTrace> pollGesture() {
        GestureTrace g = sealedGesture;
        sealedGesture = null;
        return Optional.ofNullable(g);
    }

    /** Returns true (once) if a general tap event fired this frame. */
    public boolean consumeTap() {
        if (tapFired) {
            tapFired = false;
            return true;
        }
        return false;
    }

    /** Returns true (once) if a meter-tap event fired this frame. */
    public boolean consumeMeterTap() {
        if (meterTapFired) {
            meterTapFired = false;
            return true;
        }
        return false;
    }

    /**
     * Returns true if the virtual coordinates are within either stick's radius.
     * Always false on desktop.
     */
    public boolean isOnJoystick(float virtX, float virtY) {
        if (!touchMode) {
            return false;
        }
        return withinRadius(leftStick, virtX, virtY) || withinRadius(rightStick, virtX, virtY);
    }

    /** Renders touch joysticks when in touch mode. Batch must be begun. */
    public void render(SpriteBatch batch, SpriteProvider sprites) {
        if (touchMode) {
            leftStick.render(batch, sprites);
            rightStick.render(batch, sprites);
        }
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private Vector2 unprojectMouse(Viewport viewport) {
        tmpVec.set(Gdx.input.getX(), Gdx.input.getY());
        viewport.unproject(tmpVec);
        return new Vector2(tmpVec);
    }

    private Vector2 unprojectPointer(int pointer, Viewport viewport) {
        tmpVec.set(Gdx.input.getX(pointer), Gdx.input.getY(pointer));
        viewport.unproject(tmpVec);
        return new Vector2(tmpVec);
    }

    private static boolean withinRadius(VirtualJoystick stick, float vx, float vy) {
        float dx = vx - stick.getCenterX();
        float dy = vy - stick.getCenterY();
        float r = stick.getRadius();
        return dx * dx + dy * dy <= r * r;
    }
}
