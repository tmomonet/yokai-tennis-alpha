package com.cafeyokai.tennis.gfx;

import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.utils.Disposable;

/**
 * Resolves sprite keys (e.g. {@code char.tess}, {@code court.default}) to
 * texture regions (research R5). All game art goes through keys so the
 * placeholder implementation can be swapped for real assets later without
 * touching screens.
 */
public interface SpriteProvider extends Disposable {

    TextureRegion byKey(String key);
}
