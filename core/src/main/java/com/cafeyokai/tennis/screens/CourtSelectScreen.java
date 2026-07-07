package com.cafeyokai.tennis.screens;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.math.Rectangle;
import com.cafeyokai.tennis.YokaiTennisGame;
import com.cafeyokai.tennis.content.Court;

import java.util.List;

/**
 * Court Select (Resolved Decision 6: Quick Match court chosen after the
 * character). Thumbnail grid; the single free court is pre-selected.
 */
public final class CourtSelectScreen extends BaseScreen {

    private static final float THUMB_W = 320f;
    private static final float THUMB_H = 180f;

    private final List<Court> courts = Court.all();
    private final Rectangle[] cells;
    private Court selected;

    public CourtSelectScreen(YokaiTennisGame game) {
        super(game);
        cells = new Rectangle[courts.size()];
        float gap = 60f;
        float totalW = courts.size() * THUMB_W + (courts.size() - 1) * gap;
        float startX = (YokaiTennisGame.VIRTUAL_WIDTH - totalW) / 2f;
        for (int i = 0; i < courts.size(); i++) {
            cells[i] = new Rectangle(startX + i * (THUMB_W + gap), 300f, THUMB_W, THUMB_H);
        }
        selected = courts.get(0);
        addButton("Back", 40f, 40f, 200f, 60f, game::showCharacterSelect);
        addButton("Confirm", YokaiTennisGame.VIRTUAL_WIDTH - 240f, 40f, 200f, 60f, () -> {
            game.selectedCourt = selected;
            game.startMatch();
        });
    }

    @Override
    protected void onTap(float x, float y) {
        for (int i = 0; i < cells.length; i++) {
            if (cells[i].contains(x, y)) {
                selected = courts.get(i);
                return;
            }
        }
        super.onTap(x, y);
    }

    @Override
    protected void draw(float delta) {
        drawTextCentered(titleFont, "Choose Court",
                YokaiTennisGame.VIRTUAL_WIDTH / 2f, 630f, Color.WHITE);
        for (int i = 0; i < cells.length; i++) {
            Court c = courts.get(i);
            Rectangle r = cells[i];
            if (c == selected) {
                drawRectOutline(r.x - 6f, r.y - 6f, r.width + 12f, r.height + 12f,
                        1f, 0.85f, 0.2f, 1f);
            }
            batch.draw(game.sprites.byKey(c.backgroundKey()), r.x, r.y, r.width, r.height);
            drawTextCentered(font, c.name(), r.x + r.width / 2f, r.y - 30f, Color.WHITE);
        }
    }
}
