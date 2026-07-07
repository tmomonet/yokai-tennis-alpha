package com.cafeyokai.tennis.content;

import java.util.List;

/** A selectable court. At least one FREE court exists this chunk (Resolved Decision 5). */
public record Court(String id, String name, String backgroundKey, UnlockType unlockType) {

    private static final List<Court> ALL = List.of(
            new Court("court_default", "Cafe Yokai Court", "court.default", UnlockType.FREE));

    /** All courts in Court Select grid order. */
    public static List<Court> all() {
        return ALL;
    }
}
