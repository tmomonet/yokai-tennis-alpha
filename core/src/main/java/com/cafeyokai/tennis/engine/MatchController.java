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
    private static final int HUMAN  = 0;
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
    private static final float AI_SPEED      = 7f;
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

        // Reset ball to serve position (above server's racket)
        ball.x = 0f;
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

        // Reset positions so the receiver starts centered for the serve
        playerX = 0f;
        playerY = HUMAN_BASE_Y;
        aiX = 0f;

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

        // Position AI ball at AI side
        ball.x = 0f;
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
                aiReactionStarted = false;
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
            // Check if the ball is now out
            if (!CourtGeometry.isInsideCourt(ball.lastBounceX, ball.lastBounceY)) {
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

            // First bounce in-court on AI side: check if AI can auto-return
            if (ball.lastBounceY > 0f && ball.bounces == 1) {
                // AI will attempt return after reaction delay
                if (!aiReactionStarted) {
                    aiReactionStarted = true;
                }
            }

            // First bounce in-court on human side — human must hit
        }

        // AI auto-return logic
        if (ball.y > 0f && ball.lastHitBy != AI && ball.bounces <= 1) {
            if (aiReactionStarted) {
                aiReactionTimer -= dt;
            }
            if (aiReactionTimer <= 0f && aiReactionStarted && ShotContact.canReach(aiX, aiY, ball.x, ball.y)) {
                executeAiReturn();
            }
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
        if (ball.y > 0f) {
            // Ball on AI side: move toward ball X
            float targetX = ball.x;
            float diff = targetX - aiX;
            float step  = AI_SPEED * dt;
            if (Math.abs(diff) <= step) {
                aiX = targetX;
            } else {
                aiX += Math.signum(diff) * step;
            }
        } else {
            // Drift back to center
            float step = AI_SPEED * 0.5f * dt;
            if (Math.abs(aiX) <= step) {
                aiX = 0f;
            } else {
                aiX -= Math.signum(aiX) * step;
            }
        }
        aiX = clamp(aiX, -CourtGeometry.HALF_WIDTH, CourtGeometry.HALF_WIDTH);
    }

    private void executeAiReturn() {
        float[] target = ai.chooseShotTarget(playerX, HUMAN);
        SpinType spin  = randomAiSpin();
        float speed    = RALLY_SPEED_BASE * (0.7f + 0.3f * random.nextFloat());
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

        // Aim at AI's empty side (in the direction the gesture points, toward AI side)
        float targetX = aiX + gesture.directionX() * CourtGeometry.HALF_WIDTH * 0.5f;
        targetX = clamp(targetX, -(CourtGeometry.HALF_WIDTH - 0.3f), CourtGeometry.HALF_WIDTH - 0.3f);
        float targetY;
        if (gesture.type() == ShotType.LOB) {
            targetY = AI_BASE_Y; // deep
        } else {
            targetY = CourtGeometry.HALF_LENGTH * 0.65f;
        }

        launchBallistic(HUMAN, targetX, targetY, speed, spin);

        // After human hits, start AI reaction timer
        aiReactionTimer = ai.effectiveReactionDelay(spin);
        aiReactionStarted = false;
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
        // Add a minimum arc so the ball clears the net
        float minVz = BallSimulator.GRAVITY * t * 0.3f;
        if (vz < minVz) vz = minVz;

        float vx = dx / dist * speed;
        float vy = dy / dist * speed;

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
