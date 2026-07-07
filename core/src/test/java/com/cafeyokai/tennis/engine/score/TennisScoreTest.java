package com.cafeyokai.tennis.engine.score;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Verifies every invariant in contracts/scoring-engine.md (FR-012). */
class TennisScoreTest {

    private static void winPoints(TennisScore s, int player, int n) {
        for (int i = 0; i < n; i++) {
            s.pointWonBy(player);
        }
    }

    /** Wins one game for {@code player} from 0-0 (4 straight points). */
    private static void winGame(TennisScore s, int player) {
        winPoints(s, player, 4);
    }

    private static void winGames(TennisScore s, int player, int n) {
        for (int i = 0; i < n; i++) {
            winGame(s, player);
        }
    }

    private static List<MatchEvent> eventTypes(TennisScore s, MatchEvent.Type type) {
        return s.pollEvents().stream().filter(e -> e.type() == type).toList();
    }

    // Invariant 1

    @Test
    @DisplayName("4 straight points win a game with 0/15/30/40 progression; points reset")
    void fourStraightPointsWinGame() {
        TennisScore s = new TennisScore(2, 0);
        assertEquals("0", s.displayPoints(0));
        s.pointWonBy(0);
        assertEquals("15", s.displayPoints(0));
        s.pointWonBy(0);
        assertEquals("30", s.displayPoints(0));
        s.pointWonBy(0);
        assertEquals("40", s.displayPoints(0));
        s.pointWonBy(0);

        assertEquals(1, s.games(0));
        assertEquals(0, s.games(1));
        assertEquals("0", s.displayPoints(0));
        assertEquals("0", s.displayPoints(1));
        List<MatchEvent> gameWon = eventTypes(s, MatchEvent.Type.GAME_WON);
        assertEquals(1, gameWon.size());
        assertEquals(0, gameWon.get(0).player());
    }

    // Invariant 2

    @Test
    @DisplayName("3-3 is deuce; wins cycle deuce/ad indefinitely; two consecutive from deuce win")
    void deuceAdvantageCycle() {
        TennisScore s = new TennisScore(2, 0);
        winPoints(s, 0, 3);
        winPoints(s, 1, 3);
        assertEquals("40", s.displayPoints(0));
        assertEquals("40", s.displayPoints(1));

        for (int cycle = 0; cycle < 5; cycle++) {
            s.pointWonBy(0);
            assertEquals("Ad", s.displayPoints(0));
            assertEquals("40", s.displayPoints(1));
            s.pointWonBy(1);
            assertEquals("40", s.displayPoints(0));
            assertEquals("40", s.displayPoints(1));
            assertEquals(0, s.games(0));
            assertEquals(0, s.games(1));
        }

        s.pointWonBy(1);
        s.pointWonBy(1);
        assertEquals(1, s.games(1));
        assertEquals("0", s.displayPoints(0));
    }

    // Invariant 3

    @Test
    @DisplayName("Set won at 6 with 2-game lead; 6-5 is not set-over; 7-5 is a valid set")
    void setRequiresTwoGameLead() {
        TennisScore s = new TennisScore(2, 0);
        // Reach 5-5.
        for (int i = 0; i < 5; i++) {
            winGame(s, 0);
            winGame(s, 1);
        }
        assertEquals(5, s.games(0));
        assertEquals(5, s.games(1));

        winGame(s, 0); // 6-5: not a set
        assertEquals(0, s.sets(0));
        assertFalse(s.isTiebreak());

        winGame(s, 0); // 7-5: set over
        assertEquals(1, s.sets(0));
        assertArrayEquals(new int[] {7, 5}, s.completedSetScores().get(0));
        assertEquals(0, s.games(0));
        assertEquals(0, s.games(1));
    }

    @Test
    @DisplayName("6-0 sweep wins the set")
    void straightSetWin() {
        TennisScore s = new TennisScore(2, 0);
        winGames(s, 0, 6);
        assertEquals(1, s.sets(0));
        assertArrayEquals(new int[] {6, 0}, s.completedSetScores().get(0));
        assertEquals(1, eventTypes(s, MatchEvent.Type.SET_WON).size());
    }

    // Invariant 4

    @Test
    @DisplayName("6-6 starts a tiebreak; first to 7 win-by-2; set recorded 7-6")
    void tiebreakAtSixAll() {
        TennisScore s = new TennisScore(2, 0);
        for (int i = 0; i < 6; i++) {
            winGame(s, 0);
            winGame(s, 1);
        }
        assertTrue(s.isTiebreak());
        assertEquals(1, eventTypes(s, MatchEvent.Type.TIEBREAK_STARTED).size());
        assertEquals("0", s.displayPoints(0));

        winPoints(s, 0, 6);
        assertEquals("6", s.displayPoints(0));
        assertTrue(s.isTiebreak());
        winPoints(s, 0, 1); // 7-0
        assertFalse(s.isTiebreak());
        assertEquals(1, s.sets(0));
        assertArrayEquals(new int[] {7, 6}, s.completedSetScores().get(0));
    }

    @Test
    @DisplayName("Tiebreak needs win-by-2: 7-6 continues, 8-6 ends it")
    void tiebreakWinByTwo() {
        TennisScore s = new TennisScore(2, 0);
        for (int i = 0; i < 6; i++) {
            winGame(s, 0);
            winGame(s, 1);
        }
        winPoints(s, 0, 6);
        winPoints(s, 1, 6); // 6-6 in tiebreak
        s.pointWonBy(0);    // 7-6: not over
        assertTrue(s.isTiebreak());
        s.pointWonBy(0);    // 8-6: over
        assertFalse(s.isTiebreak());
        assertEquals(1, s.sets(0));
    }

    // Invariant 5

    @Test
    @DisplayName("Tiebreak rotation: starter serves point 1, then alternates every 2; next set starts with non-starter")
    void tiebreakServeRotation() {
        TennisScore s = new TennisScore(2, 0);
        for (int i = 0; i < 6; i++) {
            winGame(s, 0);
            winGame(s, 1);
        }
        assertTrue(s.isTiebreak());
        int starter = s.currentServer();
        int other = 1 - starter;

        int[] expected = {starter, other, other, starter, starter, other, other, starter, starter};
        for (int i = 0; i < expected.length; i++) {
            assertEquals(expected[i], s.currentServer(), "server before tiebreak point " + (i + 1));
            // Alternate winners to keep the tiebreak alive through the rotation check.
            s.pointWonBy(i % 2);
        }

        // Finish the tiebreak (currently 5-4 for player 0 after 9 alternating points... make player 0 win).
        while (s.isTiebreak()) {
            s.pointWonBy(0);
        }
        assertEquals(other, s.currentServer(), "non-starter of tiebreak serves first next set");
    }

    // Invariant 6

    @Test
    @DisplayName("Server alternates every game, across set boundaries")
    void serverAlternatesEveryGame() {
        TennisScore s = new TennisScore(2, 1);
        assertEquals(1, s.currentServer());
        int expected = 1;
        // 6-0 set then 3 more games; server must flip each game including across the set boundary.
        for (int g = 0; g < 9; g++) {
            assertEquals(expected, s.currentServer(), "server for game " + (g + 1));
            winGame(s, 0);
            expected = 1 - expected;
        }
    }

    // Invariant 7

    @Test
    @DisplayName("setsToWin=1: match ends after the first set (single-set mode, FR-001a)")
    void singleSetMode() {
        TennisScore s = new TennisScore(1, 0);
        winGames(s, 1, 6);
        assertTrue(s.isMatchOver());
        assertEquals(1, s.winner());
        assertEquals(1, eventTypes(s, MatchEvent.Type.MATCH_OVER).size());
    }

    @Test
    @DisplayName("setsToWin=2: match ends at 2 sets, not 1")
    void bestOfThreeMode() {
        TennisScore s = new TennisScore(2, 0);
        winGames(s, 0, 6);
        assertFalse(s.isMatchOver());
        winGames(s, 1, 6);
        assertFalse(s.isMatchOver());
        winGames(s, 0, 6);
        assertTrue(s.isMatchOver());
        assertEquals(0, s.winner());
        assertEquals(2, s.sets(0));
        assertEquals(1, s.sets(1));
        List<int[]> setScores = s.completedSetScores();
        assertEquals(3, setScores.size());
        assertArrayEquals(new int[] {6, 0}, setScores.get(0));
        assertArrayEquals(new int[] {0, 6}, setScores.get(1));
        assertArrayEquals(new int[] {6, 0}, setScores.get(2));
    }

    // Invariant 8

    @Test
    @DisplayName("After MATCH_OVER, further points are rejected")
    void pointsRejectedAfterMatchOver() {
        TennisScore s = new TennisScore(1, 0);
        winGames(s, 0, 6);
        assertTrue(s.isMatchOver());
        assertThrows(IllegalStateException.class, () -> s.pointWonBy(0));
        assertThrows(IllegalStateException.class, () -> s.pointWonBy(1));
    }

    @Test
    @DisplayName("winner() undefined before match over")
    void winnerUndefinedBeforeOver() {
        TennisScore s = new TennisScore(2, 0);
        assertThrows(IllegalStateException.class, s::winner);
    }
}
