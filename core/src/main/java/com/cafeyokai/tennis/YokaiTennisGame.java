package com.cafeyokai.tennis;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.ScreenAdapter;
import com.badlogic.gdx.graphics.GL20;

public class YokaiTennisGame extends Game {
    @Override
    public void create() {
        // Placeholder screen until the screen framework lands (T015).
        setScreen(new ScreenAdapter() {
            @Override
            public void render(float delta) {
                Gdx.gl.glClearColor(0.09f, 0.35f, 0.23f, 1f);
                Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
            }
        });
    }
}
