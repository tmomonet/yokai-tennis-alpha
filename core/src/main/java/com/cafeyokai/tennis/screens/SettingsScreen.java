package com.cafeyokai.tennis.screens;

import com.badlogic.gdx.graphics.Color;
import com.cafeyokai.tennis.YokaiTennisGame;

/**
 * Settings (FR-001a): match-length toggle (Single Set / Best of 3, default
 * Best of 3). In-memory only — resets on restart.
 */
public final class SettingsScreen extends BaseScreen {

    private final Button matchLengthButton;

    public SettingsScreen(YokaiTennisGame game) {
        super(game);
        matchLengthButton = addButton(game.settings.matchLength.label,
                660f, 420f, 300f, 60f, this::toggleMatchLength);
        addButton("Back", 40f, 40f, 200f, 60f, game::showMainMenu);
    }

    private void toggleMatchLength() {
        game.settings.matchLength = game.settings.matchLength.toggled();
        matchLengthButton.label = game.settings.matchLength.label;
    }

    @Override
    protected void draw(float delta) {
        drawTextCentered(titleFont, "Settings",
                YokaiTennisGame.VIRTUAL_WIDTH / 2f, 620f, Color.WHITE);
        drawText(font, "Match Length", 320f, 460f, Color.WHITE);
        drawText(font, "(applies to Quick Match; resets on restart)", 320f, 400f, Color.GRAY);
    }
}
