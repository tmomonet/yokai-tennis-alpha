package com.cafeyokai.tennis.screens;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.cafeyokai.tennis.YokaiTennisGame;
import com.cafeyokai.tennis.engine.MatchController;
import com.cafeyokai.tennis.engine.gesture.GestureClassifier;
import com.cafeyokai.tennis.engine.gesture.GestureTuning;
import com.cafeyokai.tennis.engine.gesture.ShotGesture;
import com.cafeyokai.tennis.engine.gesture.GestureTrace;
import com.cafeyokai.tennis.engine.gesture.ShotType;
import com.cafeyokai.tennis.engine.physics.BallSimulator;
import com.cafeyokai.tennis.engine.physics.BallState;
import com.cafeyokai.tennis.engine.physics.ShotContact;
import com.cafeyokai.tennis.engine.physics.CourtGeometry;
import com.cafeyokai.tennis.engine.score.TennisScore;
import com.cafeyokai.tennis.engine.serve.ServeState;
import com.cafeyokai.tennis.input.MatchInput;

import java.util.Optional;

/**
 * Full-gameplay match screen (T021-T025, Phase 4; perspective view added in
 * the T026 tuning pass, reference: Mario Tennis GBC).
 *
 * Renders a behind-the-player pseudo-3D court: the near (human) baseline is
 * wide, the far (AI) baseline is ~72% of its width, the near half of the
 * court takes more vertical screen space than the far half, and sprite sizes
 * shrink with depth. Delegates all game logic to MatchController.
 *
 * Projection model — a virtual camera CAM_BACK meters behind the near
 * baseline. For an engine point (x, y) (meters, y positive toward the AI):
 *   Z(y)        = y + HALF_LENGTH + CAM_BACK          (distance from camera)
 *   screenX     = CENTER_X + x * K_W / Z(y)
 *   groundRow   = HORIZON_Y - K_Y / Z(y)              (perspective-correct)
 *   pixelsPerM  = K_W / Z(y)                          (sprite/height scale)
 * K_Y and HORIZON_Y are derived so the near baseline lands on NEAR_ROW and
 * the far baseline on FAR_ROW.
 */
public final class MatchScreen extends BaseScreen {

    // -----------------------------------------------------------------------
    // Perspective constants
    // -----------------------------------------------------------------------

    private static final float CENTER_X = 640f;

    /** Camera distance behind the near baseline (meters). Larger = milder perspective. */
    private static final float CAM_BACK = 61f;
    /** Width focal constant (px·m): near half-width ≈ 340 px, far ≈ 245 px. */
    private static final float K_W = 5040f;

    /** Screen row of the near (human) baseline. */
    private static final float NEAR_ROW = 70f;
    /** Screen row of the far (AI) baseline. */
    private static final float FAR_ROW = 555f;

    private static final float Z_NEAR = CAM_BACK;
    private static final float Z_FAR  = CAM_BACK + 2f * CourtGeometry.HALF_LENGTH;
    /** Ground-row focal constant, derived from the two anchor rows. */
    private static final float K_Y = (FAR_ROW - NEAR_ROW) / (1f / Z_NEAR - 1f / Z_FAR);
    /** Vanishing row for the ground plane (off-screen above the viewport). */
    private static final float HORIZON_Y = NEAR_ROW + K_Y / Z_NEAR;

    /** Cafe floor apron drawn around the court lines (meters). */
    private static final float APRON_M = 2.2f;
    /** Court strip height in engine meters (drawing resolution). */
    private static final float STRIP_M = 0.15f;

    /** Net height for rendering (meters). */
    private static final float NET_M = 0.95f;

    /** Sprite sizes in meters (converted per-depth to pixels). */
    private static final float PLAYER_M = 0.9f;
    private static final float BALL_M = 0.24f;
    private static final float SHADOW_M = 0.22f;

    /** HUD Y positions. */
    private static final float HUD_Y   = 700f;
    private static final float SERVE_Y = 670f;

    // -----------------------------------------------------------------------
    // Components
    // -----------------------------------------------------------------------

    private final MatchController match;
    private final MatchInput input;

    // Transient message overlay
    private String transientMessage = "";
    private float messageTimer = 0f;

    // Match-over auto-transition timer
    private float matchOverTimer = -1f;
    private static final float MATCH_OVER_LINGER = 2.5f;

    // Gesture classifier (reused each frame)
    private final GestureClassifier gestureClassifier =
            new GestureClassifier(GestureTuning.defaults());

    // Ball motion trail (screen-space ghost positions, oldest first)
    private static final int TRAIL_LEN = 7;
    private final float[] trailX = new float[TRAIL_LEN];
    private final float[] trailY = new float[TRAIL_LEN];
    private final float[] trailSize = new float[TRAIL_LEN];
    private int trailCount = 0;

    // Landing-spot indicator (Madden catch-spot style): predicted first bounce
    // of the ball in flight, recomputed each frame on a throwaway copy.
    private static final int PREDICT_MAX_STEPS = 720; // 6 s at 120 Hz
    private final BallSimulator predictSim = new BallSimulator();
    private boolean landingValid;
    private float landingX;
    private float landingY;
    private boolean landingIncoming;

    // Screen-effect clock (indicator pulse)
    private float stateTime;

    /** Power for a keyed shot: quick tap ~0.55, full 0.8 s charge = 1.0. */
    private static float chargedPower(float charge) {
        return 0.55f + 0.45f * charge;
    }

    // -----------------------------------------------------------------------
    // Constructor
    // -----------------------------------------------------------------------

    public MatchScreen(YokaiTennisGame game) {
        super(game);
        int setsToWin = game.settings.matchLength.setsToWin;
        match = new MatchController(setsToWin, game.settings.aiDifficulty,
                System.currentTimeMillis());
        input = new MatchInput();
        // Forfeit button — bottom left
        addButton("Forfeit", 20f, 20f, 120f, 40f, game::showMainMenu);
    }

    // -----------------------------------------------------------------------
    // Projection helpers
    // -----------------------------------------------------------------------

    /** Camera distance for an engine Y (meters). */
    private static float depth(float engineY) {
        return engineY + CourtGeometry.HALF_LENGTH + CAM_BACK;
    }

    /** Screen row where the ground plane at engine Y is drawn. */
    private static float groundRow(float engineY) {
        return HORIZON_Y - K_Y / depth(engineY);
    }

    /** Screen X for an engine (x, y) ground point. */
    private static float screenX(float engineX, float engineY) {
        return CENTER_X + engineX * K_W / depth(engineY);
    }

    /** Pixels per engine meter at the given depth (sprite/height scale). */
    private static float ppm(float engineY) {
        return K_W / depth(engineY);
    }

    // -----------------------------------------------------------------------
    // BaseScreen contract
    // -----------------------------------------------------------------------

    @Override
    protected void update(float delta) {
        stateTime += delta;

        // 1. Update input polling
        input.update(delta, viewport);

        // 2. Feed meter tap
        if (input.consumeMeterTap()) {
            match.meterTap();
        }

        // 3. Feed gesture
        Optional<GestureTrace> gestureOpt = input.pollGesture();
        if (gestureOpt.isPresent()) {
            Optional<ShotGesture> shot = gestureClassifier.classify(gestureOpt.get());
            shot.ifPresent(match::submitGesture);
        }

        // 3b. Feed keyed shot (desktop arrows): hold-to-charge power,
        // steered by held A/D
        input.pollShotKey().ifPresent(type ->
                match.submitGesture(new ShotGesture(
                        type, input.moveX(), 1f, chargedPower(input.shotCharge()))));

        // 4. Update match simulation
        match.update(delta, input.moveX(), input.moveY());

        // 4b. Predict the ball's first bounce for the landing indicator
        updateLandingPrediction();

        // 5. Drain messages
        String msg = match.pollMessage();
        while (msg != null) {
            if (!msg.isEmpty()) {
                transientMessage = msg;
                messageTimer = 2f;
            }
            msg = match.pollMessage();
        }
        if (messageTimer > 0f) {
            messageTimer -= delta;
        } else {
            transientMessage = "";
        }

        // 6. Ball trail sampling — only while the ball is actually in flight
        MatchController.Phase phase = match.getPhase();
        if (phase == MatchController.Phase.SERVE_FLIGHT
                || phase == MatchController.Phase.RALLY) {
            pushTrailSample();
        } else {
            trailCount = 0;
        }

        // 7. Handle match-over transition
        if (phase == MatchController.Phase.MATCH_OVER) {
            if (matchOverTimer < 0f) {
                matchOverTimer = MATCH_OVER_LINGER;
            }
            matchOverTimer -= delta;
            if (matchOverTimer <= 0f) {
                game.showScore(match.humanWon(), match.getScore().completedSetScores());
            }
        }
    }

    /**
     * Runs the deterministic ball sim forward on a copy until the first bounce
     * and records the spot. Only meaningful while the ball is in flight and has
     * not bounced yet (once it bounces, the real mark is where it landed).
     */
    private void updateLandingPrediction() {
        landingValid = false;
        MatchController.Phase phase = match.getPhase();
        if (phase != MatchController.Phase.SERVE_FLIGHT
                && phase != MatchController.Phase.RALLY) {
            return;
        }
        BallState ball = match.getBall();
        if (ball.bounces > 0) {
            return;
        }
        BallState sim = ball.copy();
        for (int i = 0; i < PREDICT_MAX_STEPS; i++) {
            if (predictSim.stepOnce(sim)) {
                landingValid = true;
                landingX = sim.lastBounceX;
                landingY = sim.lastBounceY;
                landingIncoming = ball.lastHitBy != MatchController.HUMAN;
                return;
            }
        }
    }

    @Override
    protected void draw(float delta) {
        BallState ball = match.getBall();

        drawCourt();
        if (landingValid) {
            drawLandingMarker();
        }

        // Far side of the net (drawn first so the net occludes it)
        drawPlayer(false);
        if (ball.y > 0f) {
            drawBallShadow(ball);
            drawBallTrail();
            drawBall(ball);
        }

        drawNet();

        // Near side of the net
        drawSwingRangeGlow();
        drawPlayer(true);
        if (ball.y <= 0f) {
            drawBallShadow(ball);
            drawBallTrail();
            drawBall(ball);
        }

        drawHUD();
        if (messageTimer > 0f && !transientMessage.isEmpty()) {
            drawTextCentered(font, transientMessage, CENTER_X, 450f, Color.YELLOW);
        }
        // Joystick overlays on touch
        input.render(batch, game.sprites);
    }

    // -----------------------------------------------------------------------
    // Court drawing (perspective trapezoid, strip by strip)
    // -----------------------------------------------------------------------

    private void drawCourt() {
        float extent = CourtGeometry.HALF_LENGTH + APRON_M;

        // Surface strips: apron + court, shaded slightly darker with distance
        for (float y = -extent; y < extent; y += STRIP_M) {
            float rowBottom = groundRow(y);
            float rowTop = groundRow(Math.min(y + STRIP_M, extent));
            float h = rowTop - rowBottom + 1f;
            float scale = ppm(y);
            float t = (y + extent) / (2f * extent); // 0 near → 1 far
            float shade = 1f - 0.22f * t;

            // Cafe Yokai interior: dark espresso wood floor around a
            // latte-toned play area (was grass green pre-coffee-shop theme)
            float apronHalf = (CourtGeometry.HALF_WIDTH + APRON_M) * scale;
            drawRect(CENTER_X - apronHalf, rowBottom, apronHalf * 2f, h,
                    0.26f * shade, 0.16f * shade, 0.10f * shade, 1f);

            if (Math.abs(y) <= CourtGeometry.HALF_LENGTH) {
                float courtHalf = CourtGeometry.HALF_WIDTH * scale;
                drawRect(CENTER_X - courtHalf, rowBottom, courtHalf * 2f, h,
                        0.58f * shade, 0.42f * shade, 0.26f * shade, 1f);
            }
        }

        // Sidelines (converging trapezoid edges)
        for (float y = -CourtGeometry.HALF_LENGTH; y < CourtGeometry.HALF_LENGTH; y += STRIP_M) {
            float rowBottom = groundRow(y);
            float rowTop = groundRow(Math.min(y + STRIP_M, CourtGeometry.HALF_LENGTH));
            float h = rowTop - rowBottom + 1f;
            float half = CourtGeometry.HALF_WIDTH * ppm(y);
            drawRect(CENTER_X - half - 1.5f, rowBottom, 3f, h, 1f, 1f, 1f, 0.9f);
            drawRect(CENTER_X + half - 1.5f, rowBottom, 3f, h, 1f, 1f, 1f, 0.9f);
        }

        // Baselines and service lines (horizontal, width matched to depth)
        drawCourtLine(-CourtGeometry.HALF_LENGTH, 3f, 1f);
        drawCourtLine(CourtGeometry.HALF_LENGTH, 2f, 1f);
        drawCourtLine(-CourtGeometry.SERVICE_LINE, 2f, 0.8f);
        drawCourtLine(CourtGeometry.SERVICE_LINE, 2f, 0.8f);

        // Center service line: x = 0 projects to CENTER_X at every depth
        float centerBottom = groundRow(-CourtGeometry.SERVICE_LINE);
        float centerTop = groundRow(CourtGeometry.SERVICE_LINE);
        drawRect(CENTER_X - 1f, centerBottom, 2f, centerTop - centerBottom, 1f, 1f, 1f, 0.7f);
    }

    /** Horizontal court line at the given engine Y, spanning the court width there. */
    private void drawCourtLine(float engineY, float thickness, float alpha) {
        float half = CourtGeometry.HALF_WIDTH * ppm(engineY);
        drawRect(CENTER_X - half, groundRow(engineY) - thickness / 2f,
                half * 2f, thickness, 1f, 1f, 1f, alpha);
    }

    private void drawNet() {
        float row = groundRow(0f);
        float scale = ppm(0f);
        float half = CourtGeometry.HALF_WIDTH * scale;
        float netH = NET_M * scale;

        // Wooden posts just outside the sidelines (cafe counter styling)
        drawRect(CENTER_X - half - 5f, row, 6f, netH + 6f, 0.48f, 0.32f, 0.19f, 1f);
        drawRect(CENTER_X + half - 1f, row, 6f, netH + 6f, 0.48f, 0.32f, 0.19f, 1f);

        // Espresso-dark net band with cream tape on top
        drawRect(CENTER_X - half, row, half * 2f, netH, 0.24f, 0.14f, 0.08f, 0.95f);
        drawRect(CENTER_X - half, row + netH - 4f, half * 2f, 4f, 1f, 0.96f, 0.88f, 1f);
    }

    // -----------------------------------------------------------------------
    // Ball drawing
    // -----------------------------------------------------------------------

    /**
     * Pulsing ground marker at the predicted first bounce ("catch spot").
     * Bright orange when the ball is incoming (get there!), faint white for
     * the player's own shot (aim feedback).
     */
    private void drawLandingMarker() {
        float scale = ppm(landingY);
        float pulse = 1f + 0.15f * (float) Math.sin(stateTime * 6f);
        float w = 0.8f * scale * pulse;
        float sx = screenX(landingX, landingY);
        float sy = groundRow(landingY);
        TextureRegion ballTex = game.sprites.byKey("ball");

        if (landingIncoming) {
            // Saturated red-orange so it pops against the latte-toned floor
            batch.setColor(1f, 0.32f, 0.12f, 0.7f);
        } else {
            batch.setColor(1f, 1f, 1f, 0.3f);
        }
        // Ground ellipse (perspective-squashed) + a solid center dot
        batch.draw(ballTex, sx - w / 2f, sy - w * 0.2f, w, w * 0.4f);
        float dot = w * 0.25f;
        batch.draw(ballTex, sx - dot / 2f, sy - dot * 0.2f, dot, dot * 0.4f);
        batch.setColor(Color.WHITE);
    }

    private void drawBallShadow(BallState ball) {
        float scale = ppm(ball.y);
        float w = SHADOW_M * scale;
        float sx = screenX(ball.x, ball.y);
        float sy = groundRow(ball.y);
        TextureRegion ballTex = game.sprites.byKey("ball");
        batch.setColor(0f, 0f, 0f, 0.4f);
        batch.draw(ballTex, sx - w / 2f, sy - w * 0.2f, w, w * 0.4f);
        batch.setColor(Color.WHITE);
    }

    private void pushTrailSample() {
        BallState ball = match.getBall();
        float scale = ppm(ball.y);
        float sx = screenX(ball.x, ball.y);
        float sy = groundRow(ball.y) + ball.z * scale;
        if (trailCount == TRAIL_LEN) {
            System.arraycopy(trailX, 1, trailX, 0, TRAIL_LEN - 1);
            System.arraycopy(trailY, 1, trailY, 0, TRAIL_LEN - 1);
            System.arraycopy(trailSize, 1, trailSize, 0, TRAIL_LEN - 1);
            trailCount--;
        }
        trailX[trailCount] = sx;
        trailY[trailCount] = sy;
        trailSize[trailCount] = BALL_M * scale;
        trailCount++;
    }

    /** Comet trail behind the ball (reference image) — helps track fast serves. */
    private void drawBallTrail() {
        TextureRegion ballTex = game.sprites.byKey("ball");
        for (int i = 0; i < trailCount - 1; i++) {
            float f = (i + 1f) / TRAIL_LEN; // older = smaller & fainter
            float size = trailSize[i] * (0.4f + 0.4f * f);
            batch.setColor(1f, 0.75f, 0.2f, 0.28f * f);
            batch.draw(ballTex,
                    trailX[i] - size / 2f,
                    trailY[i] - size / 2f,
                    size, size);
        }
        batch.setColor(Color.WHITE);
    }

    private void drawBall(BallState ball) {
        float scale = ppm(ball.y);
        float size = BALL_M * scale;
        float sx = screenX(ball.x, ball.y);
        float sy = groundRow(ball.y) + ball.z * scale;
        TextureRegion ballTex = game.sprites.byKey("ball");
        batch.setColor(0.95f, 0.93f, 0.25f, 1f);
        batch.draw(ballTex, sx - size / 2f, sy - size / 2f, size, size);
        batch.setColor(Color.WHITE);
    }

    // -----------------------------------------------------------------------
    // Player drawing (bottom-anchored so feet stand on the court)
    // -----------------------------------------------------------------------

    /**
     * Glow under the human player while a swing would connect (FR-033 range):
     * green in range, gold when contact would be clean (sweet-spot bonus).
     * A charge bar rises over the player while an arrow key is held.
     */
    private void drawSwingRangeGlow() {
        if (match.getPhase() != MatchController.Phase.RALLY) {
            return;
        }
        float px = match.getPlayerX();
        float py = match.getPlayerY();
        float scale = ppm(py);
        float sx = screenX(px, py);
        float sy = groundRow(py);

        BallState ball = match.getBall();
        if (ball.lastHitBy != MatchController.HUMAN
                && ShotContact.canReach(px, py, ball.x, ball.y)) {
            boolean sweet = ShotContact.distance(px, py, ball.x, ball.y)
                    <= ShotContact.SWEET_RADIUS;
            float w = 1.3f * scale;
            TextureRegion ballTex = game.sprites.byKey("ball");
            if (sweet) {
                batch.setColor(1f, 0.85f, 0.2f, 0.55f);
            } else {
                batch.setColor(0.35f, 1f, 0.45f, 0.4f);
            }
            batch.draw(ballTex, sx - w / 2f, sy - w * 0.2f, w, w * 0.4f);
            batch.setColor(Color.WHITE);
        }

        // Charge bar above the player while holding a shot key
        if (input.chargingShot() != null) {
            float charge = input.chargeLevel();
            float barW = 0.9f * scale;
            float barH = 7f;
            float bx = sx - barW / 2f;
            float by = sy + (PLAYER_M + 0.15f) * scale;
            drawRect(bx, by, barW, barH, 0.1f, 0.1f, 0.1f, 0.8f);
            drawRect(bx, by, barW * charge, barH,
                    0.4f + 0.6f * charge, 1f - 0.7f * charge, 0.15f, 0.95f);
        }
    }

    private void drawPlayer(boolean human) {
        float ex = human ? match.getPlayerX() : match.getAiX();
        float ey = human ? match.getPlayerY() : match.getAiY();
        float size = PLAYER_M * ppm(ey);
        float sx = screenX(ex, ey);
        float sy = groundRow(ey);
        TextureRegion tex = game.sprites.byKey(playerSpriteKey(human));
        batch.draw(tex, sx - size / 2f, sy, size, size);
    }

    /**
     * Returns the court-sprite key for the human or AI player.
     * Tries the ".court" variant first and falls back to the base key.
     */
    private String playerSpriteKey(boolean human) {
        if (human && game.selectedCharacter != null) {
            return game.selectedCharacter.spriteKey() + ".court";
        }
        // AI uses a different character from the available free ones
        // Default to "char.demi.court" for AI when human is Tess, etc.
        if (!human) {
            if (game.selectedCharacter != null
                    && "char.tess".equals(game.selectedCharacter.spriteKey())) {
                return "char.demi.court";
            }
            return "char.tess.court";
        }
        return "char.tess.court";
    }

    // -----------------------------------------------------------------------
    // HUD
    // -----------------------------------------------------------------------

    private void drawHUD() {
        TennisScore score = match.getScore();

        // Score line: "Sets P0-P1  |  Games P0-P1  |  Points P0-P1"
        String scoreText = String.format(
            "Sets %d-%d  |  Games %d-%d  |  %s - %s",
            score.sets(0), score.sets(1),
            score.games(0), score.games(1),
            score.displayPoints(0), score.displayPoints(1)
        );
        drawTextCentered(font, scoreText, CENTER_X, HUD_Y, Color.WHITE);

        // Serve indicator
        MatchController.Phase phase = match.getPhase();
        if (phase == MatchController.Phase.SERVE_METERS
                || phase == MatchController.Phase.SERVE_FLIGHT) {
            ServeState ss = match.getServeState();
            if (ss != null) {
                String serveTxt = match.isHumanServing() ? "You Serve" : "CPU Serves";
                if (ss.isSecondServe()) serveTxt += "  (2nd Serve)";
                drawText(font, serveTxt, 20f, SERVE_Y, Color.CYAN);
            }
        }

        // Serve meter UI (human serving in SERVE_METERS only)
        if (phase == MatchController.Phase.SERVE_METERS
                && match.isHumanServing()
                && match.getServeState() != null) {
            drawServeMeters();
        }

        // Desktop shot-key reference during rallies
        if (phase == MatchController.Phase.RALLY && !input.isTouchMode()) {
            drawTextCentered(font,
                    "UP smash   DOWN lob   LEFT slice   RIGHT topspin   |   HOLD to charge, release to swing   |   A/D aims",
                    CENTER_X, 25f, Color.LIGHT_GRAY);
        }

        // Match-over banner
        if (phase == MatchController.Phase.MATCH_OVER) {
            String banner = match.humanWon() ? "YOU WIN!" : "GAME, SET, MATCH";
            drawTextCentered(titleFont, banner, CENTER_X, 400f,
                    match.humanWon() ? Color.YELLOW : Color.RED);
        }
    }

    // -----------------------------------------------------------------------
    // Serve meter UI
    // -----------------------------------------------------------------------

    private void drawServeMeters() {
        ServeState ss = match.getServeState();
        if (ss == null) return;

        ServeState.Phase sp = ss.phase();

        // Phase instruction — playtest feedback: the meter flow needs guidance
        boolean touch = input.isTouchMode();
        String hint = switch (sp) {
            case AIMING   -> touch ? "Move to aim the serve, then tap"
                                   : "WASD to aim the serve, then press any key";
            case POWER    -> touch ? "Tap to lock power"
                                   : "Press any key to lock power";
            case ACCURACY -> touch ? "Tap when the needle is centered!"
                                   : "Press any key when the needle is centered!";
            default       -> null;
        };
        if (hint != null) {
            drawTextCentered(font, hint, CENTER_X, 250f, Color.ORANGE);
        }

        // Serve button (tap anywhere / any key works; this is the visible affordance)
        String btnLabel = switch (sp) {
            case AIMING   -> touch ? "TAP TO TOSS"      : "ANY KEY: TOSS";
            case POWER    -> touch ? "TAP: LOCK POWER"  : "ANY KEY: LOCK POWER";
            case ACCURACY -> touch ? "TAP: HIT!"        : "ANY KEY: HIT!";
            default       -> null;
        };
        if (btnLabel != null) {
            drawRect(540f, 18f, 200f, 50f, 0.80f, 0.50f, 0.10f, 0.92f);
            drawRectOutline(540f, 18f, 200f, 50f, 1f, 1f, 1f, 1f);
            drawTextCentered(font, btnLabel, CENTER_X, 43f, Color.WHITE);
        }

        // Aim indicator (yellow crosshair, projected onto the far court)
        float aimSx = screenX(ss.aimX(), ss.aimY());
        float aimSy = groundRow(ss.aimY());
        drawRect(aimSx - 8f, aimSy - 1f, 16f, 2f, 1f, 1f, 0f, 0.9f);
        drawRect(aimSx - 1f, aimSy - 8f, 2f, 16f, 1f, 1f, 0f, 0.9f);

        // Auto-serve notice only when the (generous) window is nearly up —
        // no countdown bar (playtest 2026-07-14: the timer read as pressure)
        if (sp == ServeState.Phase.AIMING && ss.aimTimeRemaining() < 5f) {
            int secs = (int) Math.ceil(ss.aimTimeRemaining());
            drawTextCentered(font, "Auto-serve in " + secs + "s",
                    CENTER_X, 280f, Color.LIGHT_GRAY);
        }

        if (sp == ServeState.Phase.POWER || sp == ServeState.Phase.ACCURACY) {
            // Power bar (vertical, left of the serve button)
            float powerFrac = ss.powerMeter();
            float powerX = 470f;
            float powerY = 80f;
            float powerH = 100f;
            float powerW = 20f;
            drawRect(powerX, powerY, powerW, powerH, 0.1f, 0.1f, 0.1f, 0.9f);
            float fillH = powerH * powerFrac;
            float fillColor = powerFrac < 0.5f ? 0.3f : powerFrac;
            drawRect(powerX, powerY, powerW, fillH, fillColor, 1f - fillColor * 0.5f, 0.1f, 1f);
            drawText(font, "PWR", powerX - 4f, powerY - 6f, Color.LIGHT_GRAY);
        }

        if (sp == ServeState.Phase.ACCURACY) {
            // Accuracy needle bar (horizontal, above the serve button)
            float needle = ss.accuracyNeedle(); // [-1, 1]
            float needleX = 540f;
            float needleY = 90f;
            float needleW = 200f;
            float needleH = 12f;
            drawRect(needleX, needleY, needleW, needleH, 0.15f, 0.15f, 0.15f, 0.9f);
            // Sweet-spot band in the middle
            float bandW = needleW * ServeState.SWEET_SPOT;
            drawRect(needleX + needleW / 2f - bandW / 2f, needleY, bandW, needleH,
                    0.2f, 0.7f, 0.25f, 0.9f);
            // Needle position: map -1..1 to 0..needleW
            float nPos = (needle + 1f) / 2f * needleW;
            drawRect(needleX + nPos - 2f, needleY - 4f, 4f, needleH + 8f, 1f, 0.2f, 0.2f, 1f);
            drawText(font, "ACC", needleX - 40f, needleY + 10f, Color.LIGHT_GRAY);
        }
    }
}
