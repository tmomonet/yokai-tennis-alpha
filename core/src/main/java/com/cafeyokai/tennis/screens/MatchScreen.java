package com.cafeyokai.tennis.screens;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.cafeyokai.tennis.YokaiTennisGame;
import com.cafeyokai.tennis.engine.MatchController;
import com.cafeyokai.tennis.engine.gesture.GestureClassifier;
import com.cafeyokai.tennis.engine.gesture.GestureTuning;
import com.cafeyokai.tennis.engine.gesture.ShotGesture;
import com.cafeyokai.tennis.engine.gesture.GestureTrace;
import com.cafeyokai.tennis.engine.physics.BallState;
import com.cafeyokai.tennis.engine.physics.CourtGeometry;
import com.cafeyokai.tennis.engine.score.TennisScore;
import com.cafeyokai.tennis.engine.serve.ServeState;
import com.cafeyokai.tennis.input.MatchInput;

import java.util.Optional;

/**
 * Full-gameplay match screen (T021-T025, Phase 4).
 *
 * Renders a top-down court, ball with height shadow, character sprites, HUD
 * score, and serve-meter UI. Delegates all game logic to MatchController.
 *
 * Coordinate conversion (engine meters → virtual pixels):
 *   screenX = COURT_ORIGIN_X + engineX * SCALE
 *   screenY = COURT_ORIGIN_Y + engineY * SCALE
 * Engine Y points toward AI (positive = AI side); screen Y points up (LibGDX
 * convention). Player 0 (human) is at negative Y = bottom of the screen.
 */
public final class MatchScreen extends BaseScreen {

    // -----------------------------------------------------------------------
    // Layout constants
    // -----------------------------------------------------------------------

    /** Pixels per meter. */
    private static final float SCALE = 26f;

    /** Screen-space court center. */
    private static final float COURT_ORIGIN_X = 640f;
    private static final float COURT_ORIGIN_Y = 370f;

    /** Court half-size in screen pixels. */
    private static final float HALF_W_PX = CourtGeometry.HALF_WIDTH  * SCALE;
    private static final float HALF_L_PX = CourtGeometry.HALF_LENGTH * SCALE;

    /** Net height at center for rendering purposes (visual band). */
    private static final float NET_BAND_HEIGHT = 4f;

    /** Ball sprite size at z = 0 (on the ground). Scales up with height. */
    private static final float BALL_BASE_SIZE = 14f;
    /** Shadow base size (always at z = 0 projection). */
    private static final float SHADOW_SIZE = 10f;

    /** Player sprite size. */
    private static final float PLAYER_SIZE = 48f;

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
    // BaseScreen contract
    // -----------------------------------------------------------------------

    @Override
    protected void update(float delta) {
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

        // 4. Update match simulation
        match.update(delta, input.moveX(), input.moveY());

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

        // 6. Handle match-over transition
        if (match.getPhase() == MatchController.Phase.MATCH_OVER) {
            if (matchOverTimer < 0f) {
                matchOverTimer = MATCH_OVER_LINGER;
            }
            matchOverTimer -= delta;
            if (matchOverTimer <= 0f) {
                game.showScore(match.humanWon(), match.getScore().completedSetScores());
            }
        }
    }

    @Override
    protected void draw(float delta) {
        drawCourt();
        drawBallShadow();
        drawBall();
        drawPlayers();
        drawHUD();
        if (messageTimer > 0f && !transientMessage.isEmpty()) {
            drawTextCentered(font, transientMessage, COURT_ORIGIN_X, 450f, Color.YELLOW);
        }
        // Joystick overlays on touch
        input.render(batch, game.sprites);
    }

    // -----------------------------------------------------------------------
    // Court drawing
    // -----------------------------------------------------------------------

    private void drawCourt() {
        // Court surface (dark green)
        drawRect(
            COURT_ORIGIN_X - HALF_W_PX,
            COURT_ORIGIN_Y - HALF_L_PX,
            HALF_W_PX * 2f,
            HALF_L_PX * 2f,
            0.08f, 0.28f, 0.14f, 1f
        );

        // Court outline (white)
        drawRectOutline(
            COURT_ORIGIN_X - HALF_W_PX,
            COURT_ORIGIN_Y - HALF_L_PX,
            HALF_W_PX * 2f,
            HALF_L_PX * 2f,
            1f, 1f, 1f, 1f
        );

        // Service lines (parallel to net)
        float svcPx = CourtGeometry.SERVICE_LINE * SCALE;
        // Player-0 side service line (y = -SERVICE_LINE)
        drawRect(COURT_ORIGIN_X - HALF_W_PX, COURT_ORIGIN_Y - svcPx - 1f,
                 HALF_W_PX * 2f, 2f,
                 1f, 1f, 1f, 0.8f);
        // AI side service line (y = +SERVICE_LINE)
        drawRect(COURT_ORIGIN_X - HALF_W_PX, COURT_ORIGIN_Y + svcPx - 1f,
                 HALF_W_PX * 2f, 2f,
                 1f, 1f, 1f, 0.8f);

        // Center service line (vertical, between the two service lines)
        drawRect(COURT_ORIGIN_X - 1f, COURT_ORIGIN_Y - svcPx,
                 2f, svcPx * 2f,
                 1f, 1f, 1f, 0.7f);

        // Net — white band across y = 0
        drawRect(COURT_ORIGIN_X - HALF_W_PX,
                 COURT_ORIGIN_Y - NET_BAND_HEIGHT / 2f,
                 HALF_W_PX * 2f, NET_BAND_HEIGHT,
                 1f, 1f, 1f, 1f);
    }

    // -----------------------------------------------------------------------
    // Ball drawing
    // -----------------------------------------------------------------------

    private void drawBallShadow() {
        BallState ball = match.getBall();
        float sx = courtToScreenX(ball.x);
        float sy = courtToScreenY(ball.y);
        // Shadow is always at z = 0 projection (same XY as ball but on the court)
        TextureRegion ballTex = game.sprites.byKey("ball");
        batch.setColor(0f, 0f, 0f, 0.4f);
        batch.draw(ballTex,
                sx - SHADOW_SIZE / 2f,
                sy - SHADOW_SIZE / 2f,
                SHADOW_SIZE, SHADOW_SIZE);
        batch.setColor(Color.WHITE);
    }

    private void drawBall() {
        BallState ball = match.getBall();
        float sx = courtToScreenX(ball.x);
        float sy = courtToScreenY(ball.y);
        // Ball appears above its shadow by its height z
        float heightOffsetPx = ball.z * SCALE;
        float size = BALL_BASE_SIZE + ball.z * 4f; // bigger when higher
        TextureRegion ballTex = game.sprites.byKey("ball");
        batch.setColor(0.95f, 0.93f, 0.25f, 1f);
        batch.draw(ballTex,
                sx - size / 2f,
                sy - size / 2f + heightOffsetPx,
                size, size);
        batch.setColor(Color.WHITE);
    }

    // -----------------------------------------------------------------------
    // Player drawing
    // -----------------------------------------------------------------------

    private void drawPlayers() {
        // Human player (bottom)
        String humanKey = playerSpriteKey(true);
        TextureRegion humanTex = game.sprites.byKey(humanKey);
        float hx = courtToScreenX(match.getPlayerX());
        float hy = courtToScreenY(match.getPlayerY());
        batch.draw(humanTex,
                hx - PLAYER_SIZE / 2f,
                hy - PLAYER_SIZE / 2f,
                PLAYER_SIZE, PLAYER_SIZE);

        // AI player (top)
        String aiKey = playerSpriteKey(false);
        TextureRegion aiTex = game.sprites.byKey(aiKey);
        float ax = courtToScreenX(match.getAiX());
        float ay = courtToScreenY(match.getAiY());
        batch.draw(aiTex,
                ax - PLAYER_SIZE / 2f,
                ay - PLAYER_SIZE / 2f,
                PLAYER_SIZE, PLAYER_SIZE);
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
        drawTextCentered(font, scoreText, COURT_ORIGIN_X, HUD_Y, Color.WHITE);

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

        // Match-over banner
        if (phase == MatchController.Phase.MATCH_OVER) {
            String banner = match.humanWon() ? "YOU WIN!" : "GAME, SET, MATCH";
            drawTextCentered(titleFont, banner, COURT_ORIGIN_X, 400f,
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
        String hint = switch (sp) {
            case AIMING   -> "Move to aim the serve, then tap / click";
            case POWER    -> "Tap / click to lock power";
            case ACCURACY -> "Tap / click when the needle is centered!";
            default       -> null;
        };
        if (hint != null) {
            drawTextCentered(font, hint, COURT_ORIGIN_X, 230f, Color.ORANGE);
        }

        // Always draw aim indicator
        float aimSx = courtToScreenX(ss.aimX());
        float aimSy = courtToScreenY(ss.aimY());
        // Yellow crosshair
        drawRect(aimSx - 8f, aimSy - 1f, 16f, 2f, 1f, 1f, 0f, 0.9f);
        drawRect(aimSx - 1f, aimSy - 8f, 2f, 16f, 1f, 1f, 0f, 0.9f);

        // Aim timer (10s countdown bar)
        float timerFrac = ss.aimTimeRemaining() / ServeState.AIM_WINDOW_SECONDS;
        float barX = COURT_ORIGIN_X - 100f;
        float barY = 80f;
        float barW = 200f;
        float barH = 12f;
        drawRect(barX, barY, barW, barH, 0.2f, 0.2f, 0.2f, 0.8f);
        drawRect(barX, barY, barW * timerFrac, barH, 0.3f, 0.8f, 0.3f, 1f);
        drawText(font, "Aim", barX, barY + 28f, Color.LIGHT_GRAY);

        if (sp == ServeState.Phase.POWER || sp == ServeState.Phase.ACCURACY) {
            // Power bar
            float powerFrac = ss.powerMeter();
            float powerX = barX - 30f;
            float powerY = 100f;
            float powerH = 80f;
            float powerW = 18f;
            drawRect(powerX, powerY, powerW, powerH, 0.1f, 0.1f, 0.1f, 0.9f);
            float fillH = powerH * powerFrac;
            float fillColor = powerFrac < 0.5f ? 0.3f : powerFrac;
            drawRect(powerX, powerY, powerW, fillH, fillColor, 1f - fillColor * 0.5f, 0.1f, 1f);
            drawText(font, "PWR", powerX - 4f, powerY - 6f, Color.LIGHT_GRAY);
        }

        if (sp == ServeState.Phase.ACCURACY) {
            // Accuracy needle bar
            float needle = ss.accuracyNeedle(); // [-1, 1]
            float needleX = barX;
            float needleY = 50f;
            float needleW = barW;
            float needleH = 10f;
            drawRect(needleX, needleY, needleW, needleH, 0.15f, 0.15f, 0.15f, 0.9f);
            // Needle position: map -1..1 to 0..needleW
            float nPos = (needle + 1f) / 2f * needleW;
            drawRect(needleX + nPos - 2f, needleY - 4f, 4f, needleH + 8f, 1f, 0.2f, 0.2f, 1f);
            drawText(font, "ACC", needleX, needleY + 26f, Color.LIGHT_GRAY);
        }
    }

    // -----------------------------------------------------------------------
    // Coordinate conversion helpers
    // -----------------------------------------------------------------------

    /** Engine X (meters, signed) → virtual screen X (pixels). */
    private static float courtToScreenX(float engineX) {
        return COURT_ORIGIN_X + engineX * SCALE;
    }

    /**
     * Engine Y (meters, signed; negative = human/bottom side) → virtual screen Y.
     * Engine Y = -HALF_LENGTH is the human baseline (bottom of view).
     * Engine Y = +HALF_LENGTH is the AI baseline (top of view).
     * No flip needed since LibGDX batch origin is bottom-left and the court is
     * symmetric — positive engine Y should map to higher screen Y.
     */
    private static float courtToScreenY(float engineY) {
        return COURT_ORIGIN_Y + engineY * SCALE;
    }
}
