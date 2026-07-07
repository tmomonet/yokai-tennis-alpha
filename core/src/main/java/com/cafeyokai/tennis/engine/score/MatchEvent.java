package com.cafeyokai.tennis.engine.score;

/**
 * Event emitted by {@link TennisScore} as points advance the match.
 * {@code player} is the winner of the game/set/match, or -1 for
 * TIEBREAK_STARTED (which has no winner).
 */
public record MatchEvent(Type type, int player) {

    public enum Type { GAME_WON, SET_WON, TIEBREAK_STARTED, MATCH_OVER }

    public static MatchEvent gameWon(int player) {
        return new MatchEvent(Type.GAME_WON, player);
    }

    public static MatchEvent setWon(int player) {
        return new MatchEvent(Type.SET_WON, player);
    }

    public static MatchEvent tiebreakStarted() {
        return new MatchEvent(Type.TIEBREAK_STARTED, -1);
    }

    public static MatchEvent matchOver(int player) {
        return new MatchEvent(Type.MATCH_OVER, player);
    }
}
