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
            case "char.tess" -> pixelPortrait(TESS_GRID, TESS_PALETTE, 4);
            case "char.demi" -> pixelPortrait(DEMI_GRID, DEMI_PALETTE, 4);
            case "char.patreon_test" -> pixelPortrait(PATREON_GRID, PATREON_PALETTE, 4);
            case "char.tess.court" -> pixelPortrait(TESS_COURT_GRID, TESS_PALETTE, 3);
            case "char.demi.court" -> pixelPortrait(DEMI_COURT_GRID, DEMI_PALETTE, 3);
            case "char.patreon_test.court" -> pixelPortrait(PATREON_COURT_GRID, PATREON_PALETTE, 3);
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

    // -----------------------------------------------------------------------
    // Pixel-art character grids
    // -----------------------------------------------------------------------
    // Grid convention: each char maps to a palette entry; '.' = transparent.
    // The grid is top-down (row 0 = top of the sprite).
    // Palette: {r, g, b, a} each 0-255.

    // --- Tess (red hair, red outfit, skin face) ---
    private static final String[] TESS_GRID = {
        "....RRRRRR....",
        "...RRRRRRRR...",
        "...RRffffRR...",
        "...RfffffRR...",
        "...RfffffRR...",
        "....RRRRRR....",
        "..OOOOOOOOOO..",
        ".OOOOOOOOOOOO.",
        ".OOOOOOOOOOOO.",
        ".OOOOOOOOOOOO.",
        "..OOOOOOOOOO..",
        "...OOOO.OOOO..",
        "..OOOOO.OOOOO.",
        ".OOOOOO.OOOOOO",
        "OOOOOOO.OOOOOOO",
        "OOOOOO...OOOOO",
    };
    private static final java.util.Map<Character, int[]> TESS_PALETTE = buildPalette(
        'R', new int[]{210, 40,  40,  255},
        'f', new int[]{240, 195, 150, 255},
        'O', new int[]{200, 50,  50,  255}
    );

    // --- Demi (blue hair, blue outfit) ---
    private static final String[] DEMI_GRID = {
        "....BBBBBB....",
        "...BBBBBBBB...",
        "...BBffffBB...",
        "...BfffffBB...",
        "...BfffffBB...",
        "....BBBBBB....",
        "..UUUUUUUUUU..",
        ".UUUUUUUUUUUU.",
        ".UUUUUUUUUUUU.",
        ".UUUUUUUUUUUU.",
        "..UUUUUUUUUU..",
        "...UUUU.UUUU..",
        "..UUUUU.UUUUU.",
        ".UUUUUU.UUUUUU",
        "UUUUUUU.UUUUUU",
        "UUUUUU...UUUUU",
    };
    private static final java.util.Map<Character, int[]> DEMI_PALETTE = buildPalette(
        'B', new int[]{50,  80,  220, 255},
        'f', new int[]{240, 195, 150, 255},
        'U', new int[]{60,  100, 210, 255}
    );

    // --- Patreon test (dark silhouette with green outline and "?" face) ---
    private static final String[] PATREON_GRID = {
        "....GGGGGG....",
        "...GGGGGGGG...",
        "...GGddddGG...",
        "...GdQdddGG...",
        "...GdddddGG...",
        "....GGGGGG....",
        "..VVVVVVVVVV..",
        ".VVVVVVVVVVVV.",
        ".VVVVVVVVVVVV.",
        ".VVVVVVVVVVVV.",
        "..VVVVVVVVVV..",
        "...VVVV.VVVV..",
        "..VVVVV.VVVVV.",
        ".VVVVVV.VVVVVV",
        "VVVVVVV.VVVVVV",
        "VVVVVV...VVVVV",
    };
    private static final java.util.Map<Character, int[]> PATREON_PALETTE = buildPalette(
        'G', new int[]{40,  180, 70,  255},
        'd', new int[]{30,  25,  40,  255},
        'Q', new int[]{220, 220, 50,  255},
        'V', new int[]{50,  160, 60,  255}
    );

    // --- Court (chibi) sprites: smaller 12x16 grids for in-match rendering ---
    private static final String[] TESS_COURT_GRID = {
        "..RRRR..",
        ".RRRRRR.",
        ".RffRRR.",
        ".RfffffR.",
        "..RRRR..",
        ".OOOOOO.",
        "OOOOOOOO",
        "OOOOOOOO",
        ".OOOOOO.",
        "..OO.OO.",
        "..OO.OO.",
        ".OOO.OOO",
    };
    private static final String[] DEMI_COURT_GRID = {
        "..BBBB..",
        ".BBBBBB.",
        ".BBffBB.",
        ".BfffffB.",
        "..BBBB..",
        ".UUUUUU.",
        "UUUUUUUU",
        "UUUUUUUU",
        ".UUUUUU.",
        "..UU.UU.",
        "..UU.UU.",
        ".UUU.UUU",
    };
    private static final String[] PATREON_COURT_GRID = {
        "..GGGG..",
        ".GGGGGG.",
        ".GGddGG.",
        ".GdQdddG.",
        "..GGGG..",
        ".VVVVVV.",
        "VVVVVVVV",
        "VVVVVVVV",
        ".VVVVVV.",
        "..VV.VV.",
        "..VV.VV.",
        ".VVV.VVV",
    };

    // -----------------------------------------------------------------------
    // Pixel-art renderer
    // -----------------------------------------------------------------------

    /**
     * Renders a pixel-art grid to a Texture.
     * Each character in {@code rows} maps to a palette entry; '.' is transparent.
     * Each grid cell is rendered at {@code scale x scale} pixels.
     */
    private static Texture pixelPortrait(String[] rows, java.util.Map<Character, int[]> palette, int scale) {
        int gridH = rows.length;
        int gridW = 0;
        for (String row : rows) {
            if (row.length() > gridW) gridW = row.length();
        }
        int pw = gridW * scale;
        int ph = gridH * scale;
        Pixmap p = new Pixmap(pw, ph, Pixmap.Format.RGBA8888);
        // Fill transparent
        p.setColor(0f, 0f, 0f, 0f);
        p.fill();
        for (int row = 0; row < gridH; row++) {
            String line = rows[row];
            for (int col = 0; col < line.length(); col++) {
                char ch = line.charAt(col);
                if (ch == '.') continue;
                int[] rgba = palette.get(ch);
                if (rgba == null) continue;
                p.setColor(rgba[0] / 255f, rgba[1] / 255f, rgba[2] / 255f, rgba[3] / 255f);
                p.fillRectangle(col * scale, row * scale, scale, scale);
            }
        }
        return upload(p);
    }

    /** Builds a Character → int[4] palette map from alternating key/value varargs. */
    private static java.util.Map<Character, int[]> buildPalette(Object... entries) {
        java.util.Map<Character, int[]> map = new java.util.HashMap<>();
        for (int i = 0; i + 1 < entries.length; i += 2) {
            map.put((Character) entries[i], (int[]) entries[i + 1]);
        }
        return map;
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
