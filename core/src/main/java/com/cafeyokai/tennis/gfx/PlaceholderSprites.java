package com.cafeyokai.tennis.gfx;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureRegion;

import java.util.HashMap;
import java.util.Map;

/**
 * Runtime-generated placeholder art (research R5, FR-054): solid-color
 * geometric Pixmap textures, no binary assets in the repo. Swapping in real
 * art later means replacing this provider; the keys stay the same.
 */
public final class PlaceholderSprites implements SpriteProvider {

    private final Map<String, Texture> textures = new HashMap<>();

    @Override
    public TextureRegion byKey(String key) {
        Texture t = textures.get(key);
        if (t == null) {
            t = create(key);
            textures.put(key, t);
        }
        return new TextureRegion(t);
    }

    private Texture create(String key) {
        return switch (key) {
            case "white" -> solid(1, 1, Color.WHITE);
            case "char.tess" -> portrait(new Color(0.85f, 0.20f, 0.20f, 1f));
            case "char.demi" -> portrait(new Color(0.22f, 0.42f, 0.90f, 1f));
            case "char.patreon_test" -> portrait(new Color(0.20f, 0.72f, 0.30f, 1f));
            case "ball" -> ball();
            case "badge.padlock" -> padlock();
            case "joystick.base" -> circle(160, new Color(1f, 1f, 1f, 0.25f));
            case "joystick.knob" -> circle(72, new Color(1f, 1f, 1f, 0.6f));
            case "court.default" -> courtThumbnail();
            default -> throw new IllegalArgumentException("unknown sprite key: " + key);
        };
    }

    private static Texture solid(int w, int h, Color c) {
        Pixmap p = new Pixmap(w, h, Pixmap.Format.RGBA8888);
        p.setColor(c);
        p.fill();
        return upload(p);
    }

    /** Character portrait: colored round head + shoulders on a dark card. */
    private static Texture portrait(Color c) {
        int s = 128;
        Pixmap p = new Pixmap(s, s, Pixmap.Format.RGBA8888);
        p.setColor(0.13f, 0.12f, 0.18f, 1f);
        p.fill();
        p.setColor(c);
        p.fillCircle(s / 2, s / 2 + 14, 30);            // head
        p.fillRectangle(s / 2 - 42, 0, 84, 34);          // shoulders (y-down pixmap)
        p.setColor(c.r * 0.6f, c.g * 0.6f, c.b * 0.6f, 1f);
        p.drawRectangle(0, 0, s, s);
        p.drawRectangle(1, 1, s - 2, s - 2);
        return upload(p);
    }

    private static Texture ball() {
        int s = 32;
        Pixmap p = new Pixmap(s, s, Pixmap.Format.RGBA8888);
        p.setColor(0.95f, 0.93f, 0.25f, 1f);
        p.fillCircle(s / 2, s / 2, s / 2 - 1);
        return upload(p);
    }

    private static Texture padlock() {
        int s = 48;
        Pixmap p = new Pixmap(s, s, Pixmap.Format.RGBA8888);
        p.setColor(0.85f, 0.75f, 0.25f, 1f);
        p.fillRectangle(8, 22, 32, 20);                 // body
        p.setColor(0.7f, 0.62f, 0.2f, 1f);
        for (int r = 10; r <= 13; r++) {
            p.drawCircle(24, 18, r);                    // shackle
        }
        return upload(p);
    }

    private static Texture circle(int size, Color c) {
        Pixmap p = new Pixmap(size, size, Pixmap.Format.RGBA8888);
        p.setColor(c);
        p.fillCircle(size / 2, size / 2, size / 2 - 2);
        return upload(p);
    }

    /** Top-down court card: green surface, white lines, net band across the middle. */
    private static Texture courtThumbnail() {
        int w = 320;
        int h = 180;
        Pixmap p = new Pixmap(w, h, Pixmap.Format.RGBA8888);
        p.setColor(0.10f, 0.35f, 0.22f, 1f);
        p.fill();
        p.setColor(0.16f, 0.48f, 0.30f, 1f);
        p.fillRectangle(30, 20, w - 60, h - 40);
        p.setColor(Color.WHITE);
        p.drawRectangle(30, 20, w - 60, h - 40);
        p.drawLine(30, h / 2, w - 31, h / 2);           // net line
        p.drawLine(w / 2, 45, w / 2, h - 46);           // center service line
        p.drawLine(55, 45, w - 56, 45);                 // service lines
        p.drawLine(55, h - 46, w - 56, h - 46);
        return upload(p);
    }

    private static Texture upload(Pixmap p) {
        Texture t = new Texture(p);
        p.dispose();
        return t;
    }

    @Override
    public void dispose() {
        for (Texture t : textures.values()) {
            t.dispose();
        }
        textures.clear();
    }
}
