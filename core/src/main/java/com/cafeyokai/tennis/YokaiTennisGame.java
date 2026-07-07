package com.cafeyokai.tennis;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.cafeyokai.tennis.content.Character;
import com.cafeyokai.tennis.content.Court;
import com.cafeyokai.tennis.content.GameSettings;
import com.cafeyokai.tennis.gfx.PlaceholderSprites;
import com.cafeyokai.tennis.gfx.SpriteProvider;
import com.cafeyokai.tennis.screens.CharacterSelectScreen;
import com.cafeyokai.tennis.screens.CourtSelectScreen;
import com.cafeyokai.tennis.screens.MainMenuScreen;
import com.cafeyokai.tennis.screens.MatchScreen;
import com.cafeyokai.tennis.screens.ScoreScreen;
import com.cafeyokai.tennis.screens.SettingsScreen;
import com.cafeyokai.tennis.screens.StubScreen;
import com.cafeyokai.tennis.screens.UnlockScreen;

import java.util.List;

/**
 * Game shell: shared rendering objects, session state, and screen
 * navigation for Flow 1 (Main Menu → Character Select → Court Select →
 * Match → Score) plus the Flow 2 Patreon-gate placeholder.
 */
public class YokaiTennisGame extends Game {

    public static final float VIRTUAL_WIDTH = 1280f;
    public static final float VIRTUAL_HEIGHT = 720f;

    public SpriteBatch batch;
    public BitmapFont font;
    public BitmapFont titleFont;
    public SpriteProvider sprites;
    public final GameSettings settings = new GameSettings();

    public Character selectedCharacter;
    public Court selectedCourt;

    @Override
    public void create() {
        batch = new SpriteBatch();
        font = new BitmapFont();
        font.getData().setScale(1.6f);
        titleFont = new BitmapFont();
        titleFont.getData().setScale(3.2f);
        sprites = new PlaceholderSprites();
        showMainMenu();
    }

    private void switchTo(Screen next) {
        Screen old = getScreen();
        setScreen(next);
        if (old != null) {
            old.dispose();
        }
    }

    public void showMainMenu() {
        switchTo(new MainMenuScreen(this));
    }

    public void showSettings() {
        switchTo(new SettingsScreen(this));
    }

    public void showStub(String title) {
        switchTo(new StubScreen(this, title));
    }

    public void showCharacterSelect() {
        switchTo(new CharacterSelectScreen(this));
    }

    public void showUnlock(Character character) {
        switchTo(new UnlockScreen(this, character));
    }

    public void showCourtSelect() {
        switchTo(new CourtSelectScreen(this));
    }

    public void startMatch() {
        switchTo(new MatchScreen(this));
    }

    public void showScore(boolean playerWon, List<int[]> setScores) {
        switchTo(new ScoreScreen(this, playerWon, setScores));
    }

    @Override
    public void dispose() {
        super.dispose();
        if (getScreen() != null) {
            getScreen().dispose();
        }
        batch.dispose();
        font.dispose();
        titleFont.dispose();
        sprites.dispose();
    }
}
