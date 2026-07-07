package com.cafeyokai.tennis.screens;

import com.badlogic.gdx.graphics.Color;
import com.cafeyokai.tennis.YokaiTennisGame;

import java.util.List;

/**
 * Post-match Score screen (Flow 1 step 6): WIN/LOSS and set-by-set scores
 * (e.g. "6-3, 7-6"), then back to the Main Menu.
 */
public final class ScoreScreen extends BaseScreen {

    private final boolean playerWon;
    private final String setLine;

    public ScoreScreen(YokaiTennisGame game, boolean playerWon, List<int[]> setScores) {
        super(game);
        this.playerWon = playerWon;
        StringBuilder sb = new StringBuilder();
        for (int[] set : setScores) {
            if (sb.length() > 0) {
                sb.append(",  ");
            }
            sb.append(set[0]).append("-").append(set[1]);
        }
        this.setLine = sb.toString();
        float w = 300f;
        addButton("Continue", (YokaiTennisGame.VIRTUAL_WIDTH - w) / 2f, 120f, w, 64f,
                game::showMainMenu);
    }

    @Override
    protected void draw(float delta) {
        float cx = YokaiTennisGame.VIRTUAL_WIDTH / 2f;
        drawTextCentered(titleFont, playerWon ? "YOU WIN!" : "YOU LOSE",
                cx, 500f, playerWon ? Color.GOLD : Color.FIREBRICK);
        drawTextCentered(font, "Match score (you first):", cx, 380f, Color.LIGHT_GRAY);
        drawTextCentered(titleFont, setLine, cx, 310f, Color.WHITE);
    }
}
