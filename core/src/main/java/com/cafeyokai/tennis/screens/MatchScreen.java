package com.cafeyokai.tennis.screens;

import com.badlogic.gdx.graphics.Color;
import com.cafeyokai.tennis.YokaiTennisGame;

/** Match screen — gameplay lands in Phase 4 (T021+). */
public final class MatchScreen extends BaseScreen {

    public MatchScreen(YokaiTennisGame game) {
        super(game);
        addButton("Forfeit", 40f, 40f, 200f, 60f, game::showMainMenu);
    }

    @Override
    protected void draw(float delta) {
        drawTextCentered(titleFont, "Match", YokaiTennisGame.VIRTUAL_WIDTH / 2f, 400f,
                Color.WHITE);
    }
}
