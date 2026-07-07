package com.cafeyokai.tennis.content;

/** A playable character (SPEC.md Character Roster). */
public record Character(String id, String name, String spriteKey, UnlockType unlockType,
                        String loreBlurb) {

    public boolean isLocked() {
        return unlockType != UnlockType.FREE;
    }
}
