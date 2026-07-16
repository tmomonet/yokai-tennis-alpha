package com.cafeyokai.tennis.engine;

import com.cafeyokai.tennis.engine.ai.AiController;
import com.cafeyokai.tennis.engine.ai.DifficultyTier;
import com.cafeyokai.tennis.engine.gesture.ShotGesture;
import com.cafeyokai.tennis.engine.gesture.ShotType;
import com.cafeyokai.tennis.engine.physics.BallSimulator;
import com.cafeyokai.tennis.engine.physics.BallState;
import com.cafeyokai.tennis.engine.physics.CourtGeometry;
import com.cafeyokai.tennis.engine.physics.ShotContact;
import com.cafeyokai.tennis.engine.physics.SpinType;
import com.cafeyokai.tennis.engine.score.MatchEvent;
import com.cafeyokai.tennis.engine.score.TennisScore;
import com.cafeyokai.tennis.engine.serve.ServeState;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;
import java.util.Random;

/**
 * Orchestrates a full tennis match from serve to match-over (T022-T025).
 *
 * Pure Java — no LibGDX imports. Driven by frame-delta updates from MatchScreen.
 * Exposes state for rendering and navigates through: serve meters, serve flight,
 * rally, point-over cooldown, and match-over terminal state.
 */
public final class MatchController {

    // -----------------------------------------------------------------------
    // Phase enum
    // -----------------------------------------------------------------------

    public enum Phase {
        SERVE_METERS,
        SERVE_FLIGHT,
        RALLY,
        POINT_OVER,
        MATCH_OVER
    }

    // -----------------------------------------------------------------------
    // Constants
    // -----------------------------------------------------------------------

    /** Human is always player 0 (y < 0 side). */
    public static final int HUMAN  = 0;
    /** AI is always player 1 (y > 0 side). */
    private static final int AI     = 1;

    /** Human baseline Y — near the back of the court. */
    private static final float HUMAN_BASE_Y = -(CourtGeometry.HALF_LENGTH * 0.7f);
    /** AI baseline Y — near the back of the opponent side. */
    private static final float AI_BASE_Y    =  (CourtGeometry.HALF_LENGTH * 0.7f);

    /** Cooldown between point-over display and next serve. */
    private static final float POINT_OVER_DURATION = 2f;
    /** How long match-over lingers before MatchScreen transitions out. */
    private static final float MATCH_OVER_LINGER = 2f; // informational only

    /** Player movement speed (meters/second). */
    private static final float PLAYER_SPEED  = 8f;
    /** AI movement speed (meters/second). */
    private static final float AI_SPEED      = 7.5f;
    /** Shot launch speed for human rally shots (meters/second). */
    private static final float RALLY_SPEED_BASE = 20f;

    // -----------------------------------------------------------------------
    // Core components
    // -----------------------------------------------------------------------

    private final TennisScore score;
    private final Random random;
    private final AiController ai;
    private final BallSimulator simulator = new BallSimulator();
    private final BallState ball = new BallState();

    // -----------------------------------------------------------------------
    // Match state
    // -----------------------------------------------------------------------

    private Phase phase;
    private int currentServer;
    private boolean deuceCourt;
    private int receiver;
    private ServeState serveState;

    private int pointsThisGame; // used to determine deuce/ad court rotation

    /** Server's lateral spot for the current point (deuce/ad side of center). */
    private float serveSideX;

    // Player positions (human moves freely on own half; AI X-tracks the ball)
    private float playerX = 0f;
    private float playerY = HUMAN_BASE_Y;
    private float aiX     = 0f;
    private float aiY     = AI_BASE_Y;

    // Point-over cooldown
    private float pointOverTimer;

    /** AI serve windup (ball-toss telegraph) duration in seconds. */
    private static final float AI_WINDUP_DURATION = 0.9f;

    // AI serve delay, then a visible windup so the serve is telegraphed
    private float aiServeTimer;
    private float aiWindupTimer;
    private boolean aiWindingUp;

    // AI reaction delay after ball crosses net
    private float aiReactionTimer = 0f;
    private boolean aiReactionStarted = false;

    // AI's misread of the incoming ball's landing X (T026 round 8): applied
    // until the ball crosses the net, then the read snaps to the true spot.
    private float aiGuessOffset = 0f;
    private boolean aiGuessValid = false;

    // Messages for MatchScreen to display
    private final Deque<String> messages = new ArrayDeque<>();

    // -----------------------------------------------------------------------
    // Constructor
    // -----------------------------------------------------------------------

    public MatchController(int setsToWin, DifficultyTier tier, long seed) {
        this.random = new Random(seed);
        // Coin toss for first server (FR-020)
        this.currentServer = random.nextBoolean() ? HUMAN : AI;
        this.score = new TennisScore(setsToWin, currentServer);
        this.ai = new AiController(tier, seed ^ 0xdeadbeefL);
        startNewPoint();
    }

    // -----------------------------------------------------------------------
    // Point lifecycle
    // -----------------------------------------------------------------------

    private void startNewPoint() {
        phase = Phase.SERVE_METERS;
        deuceCourt = (pointsThisGame % 2 == 0);
        receiver = 1 - currentServer;

        // Tennis-rules positions: the server stands to the side of their center
        // mark matching the point (deuce = server's right facing the net) and
        // serves diagonally; the receiver shades toward the box being served to.
        float[] boxCenter = CourtGeometry.serviceBoxCenter(receiver, deuceCourt);
        serveSideX = -Math.signum(boxCenter[0]) * 1.2f;
        float serverX = serveSideX;
        float receiverX = boxCenter[0] * 0.75f;

        // Reset ball to serve position (above server's racket)
        ball.x = serverX;
        ball.y = currentServer == HUMAN ? HUMAN_BASE_Y * 0.8f : AI_BASE_Y * 0.8f;
        ball.z = 1.5f;
        ball.vx = 0f;
        ball.vy = 0f;
        ball.vz = 0f;
        ball.spin = SpinType.NONE;
        ball.bounces = 0;
        ball.netHit = false;
        ball.lastHitBy = -1;

        serveState = new ServeState(currentServer, deuceCourt);
        messages.clear();

        // Place server and receiver on their tennis-correct sides
        if (currentServer == HUMAN) {
            playerX = serverX;
            aiX = receiverX;
        } else {
            aiX = serverX;
            playerX = receiverX;
        }
        playerY = HUMAN_BASE_Y;

        aiReactionTimer = 0f;
        aiReactionStarted = false;
        aiWindingUp = false;

        if (currentServer == AI) {
            // AI waits before serving so the receiver can read the point start
            aiServeTimer = 1.4f + random.nextFloat() * 0.8f;
        }
    }

    // -----------------------------------------------------------------------
    // Main update — called every frame by MatchScreen
    // -----------------------------------------------------------------------

    public void update(float dt, float moveX, float moveY) {
        switch (phase) {
            case SERVE_METERS  -> updateServeMeters(dt, moveX, moveY);
            case SERVE_FLIGHT  -> updateServeFlight(dt, moveX, moveY);
            case RALLY         -> updateRally(dt, moveX, moveY);
            case POINT_OVER    -> updatePointOver(dt);
            case MATCH_OVER    -> { /* terminal */ }
        }
    }

    // -----------------------------------------------------------------------
    // SERVE_METERS
    // -----------------------------------------------------------------------

    private void updateServeMeters(float dt, float moveX, float moveY) {
        if (currentServer == HUMAN) {
            // Movement input steers the aim indicator (both axes) while serving
            float newAimX = serveState.aimX() + moveX * dt * 5f;
            float newAimY = serveState.aimY() + moveY * dt * 5f;
            serveState.setAim(newAimX, newAimY);
            serveState.update(dt);

            if (serveState.phase() == ServeState.Phase.LAUNCHED) {
                launchServe();
            } else if (serveState.phase() == ServeState.Phase.DOUBLE_FAULT) {
                awardPoint(receiver, "Double Fault!");
            }
        } else {
            // Receiver may position freely while waiting for the AI serve
            movePlayer(dt, moveX, moveY);
            if (!aiWindingUp) {
                aiServeTimer -= dt;
                if (aiServeTimer <= 0f) {
                    // Telegraph the serve: announce it and toss the ball
                    aiWindingUp = true;
                    aiWindupTimer = AI_WINDUP_DURATION;
                    messages.add("CPU serving...");
                }
            } else {
                aiWindupTimer -= dt;
                // Ball-toss animation: rises and falls over the windup
                float progress = 1f - Math.max(0f, aiWindupTimer) / AI_WINDUP_DURATION;
                ball.z = 1.5f + 2.2f * (float) Math.sin(Math.PI * progress);
                if (aiWindupTimer <= 0f) {
                    aiWindingUp = false;
                    executeAiServe();
                }
            }
        }
    }

    /** Human meter tap: advance the serve state machine by one step. */
    public void meterTap() {
        if (phase == Phase.SERVE_METERS && currentServer == HUMAN) {
            serveState.tap();
            if (serveState.phase() == ServeState.Phase.LAUNCHED) {
                launchServe();
            } else if (serveState.phase() == ServeState.Phase.DOUBLE_FAULT) {
                awardPoint(receiver, "Double Fault!");
            }
        }
    }

    /**
     * Launches the serve after the accuracy meter is locked. Computes
     * ballistic trajectory from server position to the aim landing spot.
     */
    private void launchServe() {
        float targetX = serveState.landingX();
        float targetY = serveState.landingY();
        float speed   = serveState.serveSpeed();

        // Check fault landing immediately
        if (serveState.isFaultLanding()) {
            fault();
            return;
        }

        launchBallistic(currentServer, targetX, targetY, speed, SpinType.NONE);
        phase = Phase.SERVE_FLIGHT;
    }

    private void executeAiServe() {
        float[] target = ai.chooseServeTarget(receiver, deuceCourt);
        float speed    = ai.serveSpeed();

        // Serve launches from where the AI is standing (its serve-side spot)
        ball.x = aiX;
        ball.y = AI_BASE_Y * 0.8f;
        ball.z = 1.5f;
        ball.bounces = 0;
        ball.netHit  = false;
        ball.lastHitBy = -1;

        launchBallistic(AI, target[0], target[1], speed, SpinType.NONE);
        phase = Phase.SERVE_FLIGHT;
    }

    // -----------------------------------------------------------------------
    // SERVE_FLIGHT
    // -----------------------------------------------------------------------

    private void updateServeFlight(float dt, float moveX, float moveY) {
        // Both server and receiver may reposition while the serve is in the air
        movePlayer(dt, moveX, moveY);

        int bounces = simulator.update(ball, dt);

        // Net hit: always a fault
        if (ball.netHit) {
            fault();
            return;
        }

        // First bounce: check service box
        if (bounces > 0) {
            if (!CourtGeometry.isInServiceBox(ball.lastBounceX, ball.lastBounceY, receiver, deuceCourt)) {
                fault();
            } else {
                phase = Phase.RALLY;
                messages.clear();
                aiReactionTimer = ai.effectiveReactionDelay(ball.spin);
                // The serve has already bounced — the receiver reacts now.
                // (Without this the AI could never return a serve: the RALLY
                // bounce handler never sees the serve's bounce.)
                aiReactionStarted = true;
            }
        }
    }

    // -----------------------------------------------------------------------
    // RALLY
    // -----------------------------------------------------------------------

    private void updateRally(float dt, float moveX, float moveY) {
        // Move human player
        movePlayer(dt, moveX, moveY);

        // Move AI toward ball if reaction delay has elapsed
        updateAiMovement(dt);

        // Advance physics
        int newBounces = simulator.update(ball, dt);

        // Net hit: ball stays on hitter's side, point to opponent of lastHitBy
        if (ball.netHit) {
            int loser = ball.lastHitBy;
            int winner = 1 - loser;
            if (loser == HUMAN) {
                awardPoint(winner, "Net!");
            } else {
                awardPoint(winner, "Let point!");
            }
            return;
        }

        // Ball bounced
        if (newBounces > 0) {
            // Only the FIRST bounce after a hit is judged in/out — once a ball
            // has bounced in legally, where the second bounce lands is moot.
            if (ball.bounces == 1
                    && !CourtGeometry.isInsideCourt(ball.lastBounceX, ball.lastBounceY)) {
                // lastHitBy sent it out — opponent wins
                int winner = 1 - ball.lastHitBy;
                awardPoint(winner, winner == HUMAN ? "Out! Point to you." : "Out!");
                return;
            }

            // Second bounce: the side the ball landed on loses
            if (ball.bounces >= 2) {
                // Whose side? y < 0 is human, y > 0 is AI
                int loser = (ball.lastBounceY < 0f) ? HUMAN : AI;
                int winner = 1 - loser;
                String msg = loser == HUMAN ? "Bounce!" : "Point!";
                awardPoint(winner, msg);
                return;
            }

            // First bounce in-court on human side — human must hit
        }

        // Reaction clock: runs from the moment the ball is struck toward the
        // AI (set in submitGesture / serve handling), gating both movement
        // (updateAiMovement) and the swing below.
        if (aiReactionStarted && aiReactionTimer > 0f) {
            aiReactionTimer -= dt;
        }

        // AI return: plays the ball off the bounce (first bounce only, no
        // air volleys) once reacted and within swing range.
        if (ball.y > 0f && ball.lastHitBy != AI && ball.bounces == 1
                && aiReactionStarted && aiReactionTimer <= 0f
                && ShotContact.canReach(aiX, aiY, ball.x, ball.y)) {
            executeAiReturn();
        }
    }

    /** Moves the human player in both axes, clamped to their half of the court. */
    private void movePlayer(float dt, float moveX, float moveY) {
        playerX = clamp(playerX + moveX * PLAYER_SPEED * dt,
                -CourtGeometry.HALF_WIDTH, CourtGeometry.HALF_WIDTH);
        playerY = clamp(playerY + moveY * PLAYER_SPEED * dt,
                -CourtGeometry.HALF_LENGTH, -1.5f);
    }

    private void updateAiMovement(float dt) {
        // The AI runs (in both axes) to meet an incoming ball once its
        // reaction delay has elapsed; otherwise it drifts back to base.
        // Pre-bounce it heads for the predicted landing spot, post-bounce it
        // chases the ball itself.
        boolean incoming = ball.lastHitBy != AI && ball.lastHitBy != -1
                && (ball.y > 0f || ball.vy > 0f);
        float targetX;
        float targetY;
        float step;
        if (incoming && aiReactionStarted && aiReactionTimer <= 0f) {
            if (ball.bounces == 0) {
                float[] spot = predictBounce();
                if (aiGuessValid && ball.y <= 0f) {
                    // Ball still on the human side: the AI commits to its
                    // (possibly wrong) first read; the true spot only once
                    // the ball crosses the net.
                    targetX = clamp(spot[0] + aiGuessOffset,
                            -CourtGeometry.HALF_WIDTH, CourtGeometry.HALF_WIDTH);
                    targetY = spot[1];
                } else {
                    targetX = spot[0];
                    targetY = spot[1];
                }
            } else {
                targetX = ball.x;
                targetY = ball.y;
            }
            step = AI_SPEED * dt;
        } else {
            targetX = 0f;
            targetY = AI_BASE_Y;
            step = AI_SPEED * 0.5f * dt;
        }
        targetX = clamp(targetX, -CourtGeometry.HALF_WIDTH, CourtGeometry.HALF_WIDTH);
        targetY = clamp(targetY, 1.0f, CourtGeometry.HALF_LENGTH);

        float dx = targetX - aiX;
        float dy = targetY - aiY;
        float dist = (float) Math.sqrt(dx * dx + dy * dy);
        if (dist <= step) {
            aiX = targetX;
            aiY = targetY;
        } else if (dist > 0f) {
            aiX += dx / dist * step;
            aiY += dy / dist * step;
        }
    }

    /** Predicted (x, y) of the ball's first bounce; falls back to its position. */
    private float[] predictBounce() {
        BallState sim = ball.copy();
        for (int i = 0; i < 720; i++) { // 6 s at 120 Hz
            if (simulator.stepOnce(sim)) {
                return new float[] {sim.lastBounceX, sim.lastBounceY};
            }
        }
        return new float[] {ball.x, ball.y};
    }

    private void executeAiReturn() {
        aiGuessValid = false;
        float[] target = ai.chooseShotTarget(playerX, HUMAN);
        SpinType spin  = randomAiSpin();
        float speed    = RALLY_SPEED_BASE * ai.tier().rallyPace
                * (0.85f + 0.3f * random.nextFloat());
        launchBallistic(AI, target[0], target[1], speed, spin);
        aiReactionTimer = ai.effectiveReactionDelay(spin);
        aiReactionStarted = false;
    }

    private SpinType randomAiSpin() {
        int r = random.nextInt(3);
        return switch (r) {
            case 1  -> SpinType.TOPSPIN;
            case 2  -> SpinType.SLICE;
            default -> SpinType.NONE;
        };
    }

    // -----------------------------------------------------------------------
    // Human shot submission
    // -----------------------------------------------------------------------

    /**
     * Called from MatchScreen when a classified gesture is available.
     * Attempts to hit the ball if in RALLY phase and within range.
     */
    public void submitGesture(ShotGesture gesture) {
        if (phase != Phase.RALLY) {
            return;
        }
        if (ball.lastHitBy == HUMAN) {
            // Cannot hit twice in a row
            messages.add("Whiff!");
            return;
        }
        if (!ShotContact.canReach(playerX, playerY, ball.x, ball.y)) {
            messages.add("Out of range!");
            return;
        }

        float powerScale = ShotContact.powerScale(playerX, playerY, ball.x, ball.y);
        float speed = RALLY_SPEED_BASE * gesture.power() * powerScale;
        if (speed < 5f) speed = 5f;

        SpinType spin = shotTypeToSpin(gesture.type());

        // Aim in court coordinates: directionX (-1..1, held A/D on desktop)
        // picks a side of the court — full deflection lands near the sideline,
        // neutral lands center — and the shot type picks the depth. Jitter
        // keeps repeated shots from landing on the identical spot.
        float targetX = gesture.directionX() * CourtGeometry.HALF_WIDTH * 0.75f
                + (random.nextFloat() - 0.5f) * 0.6f;
        targetX = clamp(targetX, -(CourtGeometry.HALF_WIDTH - 0.3f), CourtGeometry.HALF_WIDTH - 0.3f);

        float depthFrac = switch (gesture.type()) {
            case LOB     -> 0.88f;  // over the AI, near the baseline
            case TOPSPIN -> 0.75f;  // deep drive
            case SMASH   -> 0.62f;  // fast mid-court put-away
            case SLICE   -> 0.50f;  // short skidding ball
        };
        float targetY = CourtGeometry.HALF_LENGTH * depthFrac
                + (random.nextFloat() - 0.5f) * 0.8f;
        targetY = clamp(targetY, 1.5f, CourtGeometry.HALF_LENGTH - 0.5f);

        launchBallistic(HUMAN, targetX, targetY, speed, spin);

        // After human hits, the AI's reaction clock starts immediately —
        // but its first read of the landing spot carries tier-scaled error
        // (wrong-footing window until the ball crosses the net).
        aiReactionTimer = ai.effectiveReactionDelay(spin);
        aiReactionStarted = true;
        aiGuessOffset = ai.anticipationOffset();
        aiGuessValid = true;
    }

    private static SpinType shotTypeToSpin(ShotType type) {
        return switch (type) {
            case SLICE   -> SpinType.SLICE;
            case TOPSPIN -> SpinType.TOPSPIN;
            default      -> SpinType.NONE;
        };
    }

    // -----------------------------------------------------------------------
    // POINT_OVER
    // -----------------------------------------------------------------------

    private void updatePointOver(float dt) {
        pointOverTimer -= dt;
        if (pointOverTimer <= 0f) {
            nextPoint();
        }
    }

    // -----------------------------------------------------------------------
    // Internal helpers
    // -----------------------------------------------------------------------

    private void fault() {
        if (serveState == null) return;
        serveState.registerFault();
        if (serveState.isDoubleFault()) {
            awardPoint(receiver, "Double Fault!");
        } else {
            phase = Phase.SERVE_METERS;
            messages.add("Fault!");
            // Return the ball to the server's hand for the second serve
            ball.x = serveSideX;
            ball.y = currentServer == HUMAN ? HUMAN_BASE_Y * 0.8f : AI_BASE_Y * 0.8f;
            ball.z = 1.5f;
            ball.vx = 0f;
            ball.vy = 0f;
            ball.vz = 0f;
            ball.spin = SpinType.NONE;
            ball.bounces = 0;
            ball.netHit = false;
            ball.lastHitBy = -1;
            if (currentServer == AI) {
                // Pause before the AI's second serve instead of firing next frame
                aiServeTimer = 1.0f + random.nextFloat() * 0.6f;
            }
        }
    }

    private void awardPoint(int winner, String message) {
        if (!message.isEmpty()) {
            messages.add(message);
        }
        score.pointWonBy(winner);
        pointsThisGame++;
        processEvents();
        if (phase != Phase.MATCH_OVER) {
            phase = Phase.POINT_OVER;
            pointOverTimer = POINT_OVER_DURATION;
        }
    }

    private void processEvents() {
        List<MatchEvent> events = score.pollEvents();
        for (MatchEvent ev : events) {
            switch (ev.type()) {
                case GAME_WON -> {
                    currentServer = score.currentServer();
                    pointsThisGame = 0;
                    messages.add(ev.player() == HUMAN ? "Game!" : "Game, CPU.");
                }
                case SET_WON  -> messages.add(ev.player() == HUMAN ? "Set!" : "Set, CPU.");
                case TIEBREAK_STARTED -> messages.add("Tiebreak!");
                case MATCH_OVER -> {
                    phase = Phase.MATCH_OVER;
                    messages.add(ev.player() == HUMAN ? "You Win!" : "Game, Set, Match.");
                }
            }
        }
    }

    private void nextPoint() {
        if (score.isMatchOver()) {
            phase = Phase.MATCH_OVER;
        } else {
            startNewPoint();
        }
    }

    /**
     * Launches the ball on a ballistic arc from its current position toward
     * (targetX, targetY) at the given speed, arriving at z≈0 when it crosses
     * the target's ground plane.
     */
    private void launchBallistic(int hitter, float targetX, float targetY, float speed, SpinType spin) {
        float dx = targetX - ball.x;
        float dy = targetY - ball.y;
        float dist = (float) Math.sqrt(dx * dx + dy * dy);
        if (dist < 0.001f) dist = 0.001f;

        float t = dist / speed;
        // z(t) = z0 + vz*t - 0.5*g*t^2 = 0  =>  vz = (0.5*g*t^2 - z0) / t
        float vz = (0.5f * BallSimulator.GRAVITY * t * t - ball.z) / t;

        // If that flat arc would clip the net, float the shot instead: extend
        // flight time (slower, higher arc) and re-solve so the ball still lands
        // exactly on the target. (The old code raised vz without recomputing
        // the landing, so clamped shots sailed meters long — phantom faults.)
        if (Math.signum(ball.y) != Math.signum(targetY) && dy != 0f) {
            float netFrac = Math.abs(ball.y) / Math.abs(dy); // path fraction at y=0
            for (int i = 0; i < 12; i++) {
                float tNet = t * netFrac;
                float zNet = ball.z + vz * tNet - 0.5f * BallSimulator.GRAVITY * tNet * tNet;
                if (zNet >= CourtGeometry.NET_HEIGHT + 0.25f) {
                    break;
                }
                t *= 1.12f;
                vz = (0.5f * BallSimulator.GRAVITY * t * t - ball.z) / t;
            }
        }

        float vx = dx / t;
        float vy = dy / t;

        ball.onHit(hitter, vx, vy, vz, spin);
    }

    // -----------------------------------------------------------------------
    // Public accessors for MatchScreen
    // -----------------------------------------------------------------------

    public Phase getPhase() {
        return phase;
    }

    public BallState getBall() {
        return ball;
    }

    public TennisScore getScore() {
        return score;
    }

    /** Returns the current ServeState, or null if not in a serve phase. */
    public ServeState getServeState() {
        return (phase == Phase.SERVE_METERS || phase == Phase.SERVE_FLIGHT) ? serveState : null;
    }

    public boolean isHumanServing() {
        return currentServer == HUMAN;
    }

    public float getPlayerX() {
        return playerX;
    }

    public float getPlayerY() {
        return playerY;
    }

    public float getAiX() {
        return aiX;
    }

    public float getAiY() {
        return aiY;
    }

    /** Returns and removes the oldest message in the queue, or null if empty. */
    public String pollMessage() {
        return messages.isEmpty() ? null : messages.poll();
    }

    /** Valid only when phase == MATCH_OVER. */
    public boolean humanWon() {
        return score.winner() == HUMAN;
    }

    // -----------------------------------------------------------------------
    // Utility
    // -----------------------------------------------------------------------

    private static float clamp(float v, float lo, float hi) {
        return Math.max(lo, Math.min(hi, v));
    }
}
