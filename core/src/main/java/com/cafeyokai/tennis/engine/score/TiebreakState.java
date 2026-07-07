package com.cafeyokai.tennis.engine.score;

/**
 * 7-point tiebreak, win by 2, played at 6-6 (spec clarification 2026-07-07).
 *
 * Serve rotation: the starter serves point 1, then service alternates every
 * two points (standard tiebreak rotation, scoring contract invariant 5).
 */
public final class TiebreakState {

    private final int starter;
    private final int[] points = new int[2];

    public TiebreakState(int starter) {
        this.starter = starter;
    }

    public int starter() {
        return starter;
    }

    public int points(int player) {
        return points[player];
    }

    public int currentServer() {
        int played = points[0] + points[1];
        if (played == 0) {
            return starter;
        }
        // After point 1, pairs of points alternate: O,O,S,S,O,O,...
        boolean otherServes = ((played - 1) / 2) % 2 == 0;
        return otherServes ? 1 - starter : starter;
    }

    public void pointWonBy(int player) {
        points[player]++;
    }

    public boolean isWon() {
        int hi = Math.max(points[0], points[1]);
        int diff = Math.abs(points[0] - points[1]);
        return hi >= 7 && diff >= 2;
    }

    public int winner() {
        if (!isWon()) {
            throw new IllegalStateException("tiebreak not decided");
        }
        return points[0] > points[1] ? 0 : 1;
    }
}
