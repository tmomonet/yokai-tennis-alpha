package com.cafeyokai.tennis.content;

import java.util.List;

/**
 * Fixed launch roster of 3 (SPEC.md Character Roster / Resolved Decision 8):
 * Tess (red, FREE), Demi (blue, FREE), and the unnamed Patreon test
 * character (green, PATREON — name/lore TBD per spec).
 */
public final class Roster {

    private static final List<Character> ALL = List.of(
            new Character("tess", "Tess", "char.tess", UnlockType.FREE,
                    "Cafe Yokai's ace barista — serves hot."),
            new Character("demi", "Demi", "char.demi", UnlockType.FREE,
                    "Cool head, cooler backhand."),
            new Character("patreon_test", "???", "char.patreon_test", UnlockType.PATREON,
                    "A mysterious patron of the cafe."));

    private Roster() {
    }

    /** All characters in Character Select grid order. */
    public static List<Character> all() {
        return ALL;
    }
}
