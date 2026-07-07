package com.cafeyokai.tennis.screens;

import com.badlogic.gdx.graphics.Color;
import com.cafeyokai.tennis.YokaiTennisGame;
import com.cafeyokai.tennis.content.Character;

/**
 * Patreon gate placeholder (Flow 2 steps 1-2). Shows the exact SPEC copy and
 * a visible Connect Patreon button. Real OAuth is out of scope this chunk;
 * pressing the button says so explicitly — no silent failure (Constitution P3),
 * and no login is ever required to keep playing (Constitution P2).
 */
public final class UnlockScreen extends BaseScreen {

    /** Exact copy from SPEC.md Flow 2 step 2. */
    static final String GATE_COPY =
            "This character is a Patreon exclusive. Connect your Patreon account to unlock.";

    private final Character character;
    private String notice = "";

    public UnlockScreen(YokaiTennisGame game, Character character) {
        super(game);
        this.character = character;
        float w = 340f;
        float x = (YokaiTennisGame.VIRTUAL_WIDTH - w) / 2f;
        addButton("Connect Patreon", x, 170f, w, 64f,
                () -> notice = "Patreon connection is coming in a future update.");
        addButton("Back", 40f, 40f, 200f, 60f, game::showCharacterSelect);
    }

    @Override
    protected void draw(float delta) {
        float cx = YokaiTennisGame.VIRTUAL_WIDTH / 2f;
        batch.draw(game.sprites.byKey(character.spriteKey()), cx - 90f, 430f, 180f, 180f);
        batch.draw(game.sprites.byKey("badge.padlock"), cx + 42f, 558f);
        drawTextCentered(titleFont, character.name(), cx, 400f, Color.WHITE);
        drawTextCentered(font, character.loreBlurb(), cx, 340f, Color.LIGHT_GRAY);
        drawTextCentered(font, GATE_COPY, cx, 290f, Color.WHITE);
        if (!notice.isEmpty()) {
            drawTextCentered(font, notice, cx, 130f, Color.ORANGE);
        }
    }
}
