package com.cafeyokai.tennis.screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.ScreenAdapter;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.cafeyokai.tennis.YokaiTennisGame;

import java.util.ArrayList;
import java.util.List;

/**
 * Minimal screen framework: fixed 1280x720 virtual viewport, immediate-mode
 * rectangle/text drawing, and tap-driven buttons — no scene2d skin, so no
 * asset files are needed (research R5).
 */
public abstract class BaseScreen extends ScreenAdapter {

    /** A tappable labeled rectangle. */
    protected static final class Button {
        public final Rectangle bounds;
        public String label;
        public final Runnable action;

        Button(String label, float x, float y, float w, float h, Runnable action) {
            this.label = label;
            this.bounds = new Rectangle(x, y, w, h);
            this.action = action;
        }
    }

    protected final YokaiTennisGame game;
    protected final SpriteBatch batch;
    protected final BitmapFont font;
    protected final BitmapFont titleFont;
    protected final OrthographicCamera camera = new OrthographicCamera();
    protected final FitViewport viewport;
    protected final GlyphLayout layout = new GlyphLayout();
    protected final List<Button> buttons = new ArrayList<>();

    private final Vector2 tap = new Vector2();
    private final Color background = new Color(0.09f, 0.09f, 0.14f, 1f);

    protected BaseScreen(YokaiTennisGame game) {
        this.game = game;
        this.batch = game.batch;
        this.font = game.font;
        this.titleFont = game.titleFont;
        this.viewport = new FitViewport(YokaiTennisGame.VIRTUAL_WIDTH,
                YokaiTennisGame.VIRTUAL_HEIGHT, camera);
        viewport.apply(true);
    }

    protected Button addButton(String label, float x, float y, float w, float h, Runnable action) {
        Button b = new Button(label, x, y, w, h, action);
        buttons.add(b);
        return b;
    }

    protected void setBackground(float r, float g, float b) {
        background.set(r, g, b, 1f);
    }

    @Override
    public void render(float delta) {
        update(delta);
        if (Gdx.input.justTouched()) {
            tap.set(Gdx.input.getX(), Gdx.input.getY());
            viewport.unproject(tap);
            onTap(tap.x, tap.y);
        }
        Gdx.gl.glClearColor(background.r, background.g, background.b, 1f);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
        camera.update();
        batch.setProjectionMatrix(camera.combined);
        batch.begin();
        draw(delta);
        drawButtons();
        batch.end();
    }

    /** Per-frame logic before input/drawing. Default: nothing. */
    protected void update(float delta) {
    }

    /** Screen content drawn inside the shared batch. */
    protected abstract void draw(float delta);

    /** Tap in virtual coordinates. Default: dispatch to buttons. */
    protected void onTap(float x, float y) {
        for (Button b : buttons) {
            if (b.bounds.contains(x, y)) {
                b.action.run();
                return;
            }
        }
    }

    private void drawButtons() {
        for (Button b : buttons) {
            drawRect(b.bounds.x, b.bounds.y, b.bounds.width, b.bounds.height,
                    0.20f, 0.20f, 0.30f, 1f);
            drawRectOutline(b.bounds.x, b.bounds.y, b.bounds.width, b.bounds.height,
                    0.55f, 0.55f, 0.70f, 1f);
            drawTextCentered(font, b.label, b.bounds.x + b.bounds.width / 2f,
                    b.bounds.y + b.bounds.height / 2f, Color.WHITE);
        }
    }

    protected void drawRect(float x, float y, float w, float h,
                            float r, float g, float b, float a) {
        TextureRegion white = game.sprites.byKey("white");
        batch.setColor(r, g, b, a);
        batch.draw(white, x, y, w, h);
        batch.setColor(Color.WHITE);
    }

    protected void drawRectOutline(float x, float y, float w, float h,
                                   float r, float g, float b, float a) {
        float t = 2f;
        drawRect(x, y, w, t, r, g, b, a);
        drawRect(x, y + h - t, w, t, r, g, b, a);
        drawRect(x, y, t, h, r, g, b, a);
        drawRect(x + w - t, y, t, h, r, g, b, a);
    }

    protected void drawTextCentered(BitmapFont f, String text, float cx, float cy, Color color) {
        f.setColor(color);
        layout.setText(f, text);
        f.draw(batch, layout, cx - layout.width / 2f, cy + layout.height / 2f);
        f.setColor(Color.WHITE);
    }

    protected void drawText(BitmapFont f, String text, float x, float y, Color color) {
        f.setColor(color);
        f.draw(batch, text, x, y);
        f.setColor(Color.WHITE);
    }

    @Override
    public void resize(int width, int height) {
        viewport.update(width, height, true);
    }
}
