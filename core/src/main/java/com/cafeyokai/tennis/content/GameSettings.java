package com.cafeyokai.tennis.content;

import com.cafeyokai.tennis.engine.ai.DifficultyTier;

/**
 * Session-scoped settings — in-memory only this chunk, nothing survives
 * restart (persistence-free per plan). Quick Match AI is fixed to MEDIUM
 * (FR-042); the field exists for future chunks.
 */
public final class GameSettings {

    public MatchLength matchLength = MatchLength.BEST_OF_3;
    public DifficultyTier aiDifficulty = DifficultyTier.MEDIUM;
}
