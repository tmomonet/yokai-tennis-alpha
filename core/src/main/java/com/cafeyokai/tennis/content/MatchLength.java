package com.cafeyokai.tennis.content;

/** Match length option shown in Settings (FR-001a, clarified 2026-07-07). */
public enum MatchLength {
    SINGLE_SET(1, "Single Set"),
    BEST_OF_3(2, "Best of 3");

    public final int setsToWin;
    public final String label;

    MatchLength(int setsToWin, String label) {
        this.setsToWin = setsToWin;
        this.label = label;
    }

    public MatchLength toggled() {
        return this == SINGLE_SET ? BEST_OF_3 : SINGLE_SET;
    }
}
