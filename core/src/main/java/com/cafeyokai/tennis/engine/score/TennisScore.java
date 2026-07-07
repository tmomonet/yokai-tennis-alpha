package com.cafeyokai.tennis.engine.score;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

/**
 * Standard tennis scoring state machine (SPEC.md Resolved Decision 1):
 * points 0/15/30/40 with deuce/advantage, games to 6 win-by-2, 7-point
 * tiebreak at 6-6, best of {@code 2*setsToWin-1} sets.
 *
 * Pure Java, no LibGDX imports. Contract: contracts/scoring-engine.md.
 * Faults are handled by ServeState; a double fault calls
 * {@code pointWonBy(receiver)} like any other point.
 */
public final class TennisScore {

    private final int setsToWin;
    private final Deque<MatchEvent> events = new ArrayDeque<>();
    private final List<int[]> completedSetScores = new ArrayList<>();

    private final int[] points = new int[2];
    private final int[] games = new int[2];
    private final int[] sets = new int[2];

    private int gameServer;
    private TiebreakState tiebreak;
    private boolean matchOver;
    private int winner = -1;

    public TennisScore(int setsToWin, int firstServer) {
        if (setsToWin != 1 && setsToWin != 2) {
            throw new IllegalArgumentException("setsToWin must be 1 or 2: " + setsToWin);
        }
        if (firstServer != 0 && firstServer != 1) {
            throw new IllegalArgumentException("firstServer must be 0 or 1: " + firstServer);
        }
        this.setsToWin = setsToWin;
        this.gameServer = firstServer;
    }

    public void pointWonBy(int player) {
        requirePlayer(player);
        if (matchOver) {
            throw new IllegalStateException("match is over; no further points");
        }
        if (tiebreak != null) {
            tiebreak.pointWonBy(player);
            if (tiebreak.isWon()) {
                games[player]++;
                events.add(MatchEvent.gameWon(player));
                // The player who did not start the tiebreak serves first next set.
                gameServer = 1 - tiebreak.starter();
                tiebreak = null;
                setWonBy(player);
            }
            return;
        }
        points[player]++;
        if (points[player] >= 4 && points[player] - points[1 - player] >= 2) {
            gameWonBy(player);
        }
    }

    private void gameWonBy(int player) {
        games[player]++;
        points[0] = 0;
        points[1] = 0;
        events.add(MatchEvent.gameWon(player));
        gameServer = 1 - gameServer;
        if (games[player] >= 6 && games[player] - games[1 - player] >= 2) {
            setWonBy(player);
        } else if (games[0] == 6 && games[1] == 6) {
            tiebreak = new TiebreakState(gameServer);
            events.add(MatchEvent.tiebreakStarted());
        }
    }

    private void setWonBy(int player) {
        completedSetScores.add(new int[] {games[0], games[1]});
        sets[player]++;
        games[0] = 0;
        games[1] = 0;
        events.add(MatchEvent.setWon(player));
        if (sets[player] >= setsToWin) {
            matchOver = true;
            winner = player;
            events.add(MatchEvent.matchOver(player));
        }
    }

    public int currentServer() {
        return tiebreak != null ? tiebreak.currentServer() : gameServer;
    }

    public boolean isTiebreak() {
        return tiebreak != null;
    }

    public boolean isMatchOver() {
        return matchOver;
    }

    public int winner() {
        if (!matchOver) {
            throw new IllegalStateException("match not over; winner undefined");
        }
        return winner;
    }

    public String displayPoints(int player) {
        requirePlayer(player);
        if (tiebreak != null) {
            return String.valueOf(tiebreak.points(player));
        }
        int own = points[player];
        int opp = points[1 - player];
        if (own >= 3 && opp >= 3) {
            return own > opp ? "Ad" : "40";
        }
        return switch (own) {
            case 0 -> "0";
            case 1 -> "15";
            case 2 -> "30";
            default -> "40";
        };
    }

    public int games(int player) {
        requirePlayer(player);
        return games[player];
    }

    public int sets(int player) {
        requirePlayer(player);
        return sets[player];
    }

    /** Per-set game counts as {@code [gamesPlayer0, gamesPlayer1]}, oldest set first. */
    public List<int[]> completedSetScores() {
        List<int[]> copy = new ArrayList<>(completedSetScores.size());
        for (int[] s : completedSetScores) {
            copy.add(s.clone());
        }
        return copy;
    }

    /** Drains and returns all events queued since the last poll. */
    public List<MatchEvent> pollEvents() {
        List<MatchEvent> drained = new ArrayList<>(events);
        events.clear();
        return drained;
    }

    private static void requirePlayer(int player) {
        if (player != 0 && player != 1) {
            throw new IllegalArgumentException("player must be 0 or 1: " + player);
        }
    }
}
