package com.cafeyokai.tennis.screens;

import com.badlogic.gdx.graphics.Color;
import com.cafeyokai.tennis.YokaiTennisGame;

/** Main Menu (SPEC.md wireframe): Play, Circuit, Tournament, Settings. */
public final class MainMenuScreen extends BaseScreen {

    public MainMenuScreen(YokaiTennisGame game) {
        super(game);
        float w = 360f;
        float x = (YokaiTennisGame.VIRTUAL_WIDTH - w) / 2f;
        addButton("Play", x, 400f, w, 64f, game::showCharacterSelect);
        addButton("Circuit", x, 320f, w, 64f, () -> game.showStub("Circuit"));
        addButton("Tournament", x, 240f, w, 64f, () -> game.showStub("Tournament"));
        addButton("Settings", x, 160f, w, 64f, game::showSettings);
    }

    @Override
    protected void draw(float delta) {
        drawTextCentered(titleFont, "YOKAI TENNIS",
                YokaiTennisGame.VIRTUAL_WIDTH / 2f, 600f, Color.WHITE);
        drawTextCentered(font, "an arcade tennis game for Cafe Yokai fans",
                YokaiTennisGame.VIRTUAL_WIDTH / 2f, 530f, Color.LIGHT_GRAY);
    }
}
