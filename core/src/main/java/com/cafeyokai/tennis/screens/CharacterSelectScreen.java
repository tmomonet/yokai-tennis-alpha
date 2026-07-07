package com.cafeyokai.tennis.screens;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.math.Rectangle;
import com.cafeyokai.tennis.YokaiTennisGame;
import com.cafeyokai.tennis.content.Character;
import com.cafeyokai.tennis.content.Roster;

import java.util.List;

/**
 * Character Select (SPEC.md wireframe): 3-portrait grid; padlock badge on
 * the Patreon character; tapping a locked character opens the Unlock screen
 * (Flow 2 step 1); Confirm requires a selected free character.
 */
public final class CharacterSelectScreen extends BaseScreen {

    private static final float PORTRAIT = 180f;

    private final List<Character> roster = Roster.all();
    private final Rectangle[] cells;
    private Character selected;
    private final Button confirm;

    public CharacterSelectScreen(YokaiTennisGame game) {
        super(game);
        cells = new Rectangle[roster.size()];
        float gap = 70f;
        float totalW = roster.size() * PORTRAIT + (roster.size() - 1) * gap;
        float startX = (YokaiTennisGame.VIRTUAL_WIDTH - totalW) / 2f;
        for (int i = 0; i < roster.size(); i++) {
            cells[i] = new Rectangle(startX + i * (PORTRAIT + gap), 300f, PORTRAIT, PORTRAIT);
        }
        addButton("Back", 40f, 40f, 200f, 60f, game::showMainMenu);
        confirm = addButton("Confirm", YokaiTennisGame.VIRTUAL_WIDTH - 240f, 40f, 200f, 60f,
                this::confirmSelection);
        if (game.selectedCharacter != null && !game.selectedCharacter.isLocked()) {
            selected = game.selectedCharacter;
        }
    }

    private void confirmSelection() {
        if (selected != null) {
            game.selectedCharacter = selected;
            game.showCourtSelect();
        }
    }

    @Override
    protected void onTap(float x, float y) {
        for (int i = 0; i < cells.length; i++) {
            if (cells[i].contains(x, y)) {
                Character c = roster.get(i);
                if (c.isLocked()) {
                    game.showUnlock(c); // Flow 2: locked tap opens the Patreon gate
                } else {
                    selected = c;
                }
                return;
            }
        }
        super.onTap(x, y);
    }

    @Override
    protected void draw(float delta) {
        drawTextCentered(titleFont, "Choose Your Player",
                YokaiTennisGame.VIRTUAL_WIDTH / 2f, 630f, Color.WHITE);

        for (int i = 0; i < cells.length; i++) {
            Character c = roster.get(i);
            Rectangle r = cells[i];
            if (c == selected) {
                drawRectOutline(r.x - 6f, r.y - 6f, r.width + 12f, r.height + 12f,
                        1f, 0.85f, 0.2f, 1f);
            }
            batch.draw(game.sprites.byKey(c.spriteKey()), r.x, r.y, r.width, r.height);
            if (c.isLocked()) {
                batch.draw(game.sprites.byKey("badge.padlock"),
                        r.x + r.width - 52f, r.y + r.height - 52f);
            }
            drawTextCentered(font, c.name(), r.x + r.width / 2f, r.y - 30f, Color.WHITE);
        }

        confirm.label = selected == null ? "Select a player" : "Confirm: " + selected.name();
    }
}
