# Contract — TennisScore engine

Pure-Java state machine in `:core`; no LibGDX imports. This contract is what FR-012's tests verify.

## API surface

```java
TennisScore score = new TennisScore(int setsToWin, int firstServer); // setsToWin: 1|2
score.pointWonBy(int player);          // 0|1; advances all levels; no-op + IllegalStateException if matchOver
score.currentServer();                 // alternates each game; tiebreak rotation: after 1st point, then every 2
score.isTiebreak();                    // true only while games are 6-6 and set undecided
score.isMatchOver(); score.winner();   // winner defined only when over
score.displayPoints(int player);       // "0","15","30","40","Ad" (or tiebreak digits)
score.games(int p); score.sets(int p);
score.completedSetScores();            // e.g. [[6,3],[7,6]] for Score screen
score.pollEvents();                    // drained queue: GAME_WON, SET_WON, TIEBREAK_STARTED, MATCH_OVER
```

## Invariants (test these)

1. 4 straight points from 0-0 win a game (15/30/40/game); scores reset next game.
2. 3-3 points → DEUCE; alternating wins cycle DEUCE↔AD forever; two consecutive from deuce wins the game.
3. Set won at 6 games with a 2-game lead; 7-5 is a valid set; 6-5 is not set-over.
4. At 6-6 a tiebreak starts (TIEBREAK_STARTED); first to 7 win-by-2; set recorded as 7-6.
5. Tiebreak serve rotation: server serves point 1; then alternates every 2 points; next set's first server is the non-starter of the tiebreak.
6. Server alternates every game, across sets, uninterrupted.
7. setsToWin=2: match ends at 2 sets; setsToWin=1: MATCH_OVER after the first SET_WON (single-set mode, FR-001a).
8. After MATCH_OVER, further pointWonBy calls are rejected.
9. Fault handling lives OUTSIDE this contract (ServeState); a double fault simply calls pointWonBy(receiver).
