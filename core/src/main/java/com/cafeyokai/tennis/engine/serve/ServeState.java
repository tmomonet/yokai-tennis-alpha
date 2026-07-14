package com.cafeyokai.tennis.engine.serve;

import com.cafeyokai.tennis.engine.physics.CourtGeometry;

/**
 * Serve state machine for one point (SPEC.md Serve Mechanic, FR-020..023):
 *
 * AIMING (15 s window, aim indicator clamped to the valid service box; tap
 * confirms, window expiry auto-confirms — playtest 2026-07-14 removed the
 * expiry fault) → POWER (meter fills upward and ping-pongs; tap locks) →
 * ACCURACY (swinging needle; tap samples) → LAUNCHED. Needle deviation from
 * center shifts the landing laterally, scaled up by locked power — higher
 * power serves faster but with a larger fault margin (FR-022).
 *
 * Faults: a computed landing outside the service box (the caller checks
 * {@link #isFaultLanding()} and reports via {@link #registerFault()}).
 * Two faults on one point is a DOUBLE_FAULT; the caller awards the point
 * to the receiver (scoring contract rule 9).
 */
public final class ServeState {

    public enum Phase { AIMING, POWER, ACCURACY, LAUNCHED, DOUBLE_FAULT }

    public static final float AIM_WINDOW_SECONDS = 15f;
    public static final float MIN_SPEED = 18f;
    public static final float MAX_SPEED = 36f;
    /** Lateral shift in meters at full needle deflection and full power. */
    public static final float MAX_LATERAL_DEVIATION = 2.2f;
    /** Power meter fill rate per second (ping-pongs between 0 and 1). */
    public static final float POWER_FILL_RATE = 0.9f;
    /** Accuracy needle oscillation frequency in Hz. */
    public static final float NEEDLE_HZ = 0.6f;
    /**
     * Needle deflections within this band count as perfect (playtest 2026-07-14:
     * human reaction time can't hit an exact zero-crossing). Deviation outside
     * the band is remapped continuously so full deflection still means ±1.
     */
    public static final float SWEET_SPOT = 0.18f;
    /** Aim clamp inset from the box lines so the indicator stays visibly inside. */
    private static final float BOX_MARGIN = 0.3f;

    private final int server;
    private final int receiver;
    private final boolean deuceCourt;

    private Phase phase = Phase.AIMING;
    private float aimTimer = AIM_WINDOW_SECONDS;
    private float aimX;
    private float aimY;
    private float powerMeter;
    private int powerDirection = 1;
    private float needleTime;
    private float lockedPower;
    private float lockedAccuracy;
    private int faultCount;
    private float landingX;
    private float landingY;
    private float serveSpeed;

    public ServeState(int server, boolean deuceCourt) {
        this.server = server;
        this.receiver = 1 - server;
        this.deuceCourt = deuceCourt;
        float[] center = CourtGeometry.serviceBoxCenter(receiver, deuceCourt);
        this.aimX = center[0];
        this.aimY = center[1];
    }

    public void update(float dt) {
        switch (phase) {
            case AIMING -> {
                aimTimer -= dt;
                if (aimTimer <= 0f) {
                    tap(); // expiry auto-confirms the current aim (no fault)
                }
            }
            case POWER -> {
                powerMeter += powerDirection * POWER_FILL_RATE * dt;
                if (powerMeter > 1f) {
                    powerMeter = 2f - powerMeter;
                    powerDirection = -1;
                } else if (powerMeter < 0f) {
                    powerMeter = -powerMeter;
                    powerDirection = 1;
                }
            }
            case ACCURACY -> needleTime += dt;
            default -> {
            }
        }
    }

    /** Moves the aim indicator; clamped to the valid service box (FR-021). */
    public void setAim(float x, float y) {
        if (phase != Phase.AIMING) {
            return;
        }
        float sign = CourtGeometry.sideSign(receiver);
        float depth = clamp(y * sign, BOX_MARGIN, CourtGeometry.SERVICE_LINE - BOX_MARGIN);
        aimY = depth * sign;
        // Deuce court is the receiver's right half (x*sign >= 0); ad is the mirror.
        float lateral = x * sign;
        if (deuceCourt) {
            lateral = clamp(lateral, BOX_MARGIN, CourtGeometry.HALF_WIDTH - BOX_MARGIN);
        } else {
            lateral = clamp(lateral, -(CourtGeometry.HALF_WIDTH - BOX_MARGIN), -BOX_MARGIN);
        }
        aimX = lateral * sign;
    }

    /** Meter tap: confirms aim, locks power, then samples the needle (FR-021). */
    public void tap() {
        switch (phase) {
            case AIMING -> {
                phase = Phase.POWER;
                powerMeter = 0f;
                powerDirection = 1;
            }
            case POWER -> {
                lockedPower = powerMeter;
                phase = Phase.ACCURACY;
                needleTime = 0f;
            }
            case ACCURACY -> {
                lockedAccuracy = accuracyNeedle();
                computeLaunch();
                phase = Phase.LAUNCHED;
            }
            default -> {
            }
        }
    }

    private void computeLaunch() {
        // Sweet-spot band: small mistimes count as perfect; beyond it the miss
        // rescales so a full deflection is still ±1.
        float missMag = Math.max(0f, Math.abs(lockedAccuracy) - SWEET_SPOT) / (1f - SWEET_SPOT);
        float miss = Math.copySign(missMag, lockedAccuracy);
        // Fault margin grows with power (FR-022): the same needle miss drifts
        // further at high power.
        float deviation = miss * MAX_LATERAL_DEVIATION * (0.4f + 0.6f * lockedPower);
        landingX = aimX + deviation;
        landingY = aimY;
        serveSpeed = MIN_SPEED + lockedPower * (MAX_SPEED - MIN_SPEED);
    }

    /** True if the launched serve's landing is outside the service box (a fault). */
    public boolean isFaultLanding() {
        if (phase != Phase.LAUNCHED) {
            throw new IllegalStateException("no landing before launch");
        }
        return !CourtGeometry.isInServiceBox(landingX, landingY, receiver, deuceCourt);
    }

    /**
     * Records a fault (out-of-box landing, net, or window expiry). Resets for
     * a second serve, or enters DOUBLE_FAULT on the second consecutive fault.
     */
    public void registerFault() {
        faultCount++;
        if (faultCount >= 2) {
            phase = Phase.DOUBLE_FAULT;
            return;
        }
        phase = Phase.AIMING;
        aimTimer = AIM_WINDOW_SECONDS;
        powerMeter = 0f;
        powerDirection = 1;
        needleTime = 0f;
        float[] center = CourtGeometry.serviceBoxCenter(receiver, deuceCourt);
        aimX = center[0];
        aimY = center[1];
    }

    /** Needle in [-1,1]; starts at -1 when the accuracy phase begins. */
    public float accuracyNeedle() {
        return (float) Math.sin(2.0 * Math.PI * NEEDLE_HZ * needleTime - Math.PI / 2.0);
    }

    public Phase phase() {
        return phase;
    }

    public boolean isDoubleFault() {
        return phase == Phase.DOUBLE_FAULT;
    }

    public boolean isSecondServe() {
        return faultCount == 1 && phase != Phase.DOUBLE_FAULT;
    }

    public int faultCount() {
        return faultCount;
    }

    public float aimTimeRemaining() {
        return Math.max(0f, aimTimer);
    }

    public float aimX() {
        return aimX;
    }

    public float aimY() {
        return aimY;
    }

    public float powerMeter() {
        return powerMeter;
    }

    public float lockedPower() {
        return lockedPower;
    }

    public float landingX() {
        return landingX;
    }

    public float landingY() {
        return landingY;
    }

    public float serveSpeed() {
        return serveSpeed;
    }

    public int server() {
        return server;
    }

    public int receiver() {
        return receiver;
    }

    public boolean deuceCourt() {
        return deuceCourt;
    }

    private static float clamp(float v, float lo, float hi) {
        return Math.max(lo, Math.min(hi, v));
    }
}
