package com.cafeyokai.tennis.screens;

import com.badlogic.gdx.graphics.Color;
import com.cafeyokai.tennis.YokaiTennisGame;

/** Circuit/Tournament placeholder — modes are post-MVP (Resolved Decision 10). */
public final class StubScreen extends BaseScreen {

    private final String title;

    public StubScreen(YokaiTennisGame game, String title) {
        super(game);
        this.title = title;
        addButton("Back", 40f, 40f, 200f, 60f, game::showMainMenu);
    }

    @Override
    protected void draw(float delta) {
        drawTextCentered(titleFont, title,
                YokaiTennisGame.VIRTUAL_WIDTH / 2f, 440f, Color.WHITE);
        drawTextCentered(font, "Coming soon!",
                YokaiTennisGame.VIRTUAL_WIDTH / 2f, 340f, Color.LIGHT_GRAY);
    }
}
