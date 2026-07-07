package com.cafeyokai.tennis.desktop;

import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration;
import com.cafeyokai.tennis.YokaiTennisGame;

public class DesktopLauncher {
    public static void main(String[] args) {
        Lwjgl3ApplicationConfiguration config = new Lwjgl3ApplicationConfiguration();
        config.setTitle("Yokai Tennis");
        config.setWindowedMode(1280, 720);
        config.setResizable(false);
        config.setForegroundFPS(60);
        new Lwjgl3Application(new YokaiTennisGame(), config);
    }
}
