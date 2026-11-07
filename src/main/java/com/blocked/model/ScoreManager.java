package com.blocked.model;

import com.blocked.data.ScoreRepository;

/**
 * Tracks the score for the current run and the persisted best score. The
 * best score is loaded from the {@link ScoreRepository} at construction and
 * written back whenever the current run beats it.
 */
public class ScoreManager {

    private int currentScore;
    private int bestScore;

    public ScoreManager(ScoreRepository repository) {
        this.bestScore = repository.loadBestScore();
        this.currentScore = 0;
    }

    /**
     * Adds points for placing cells (one point per cell).
     */
    public void addCellPoints(int n) {
        currentScore += n;
    }

    /**
     * Awards a clear bonus and returns the bonus amount added.
     * <p>Formula: {@code bonus = 10 * (rows + cols) * max(1, rows + cols)}.
     * For example, clearing 2 rows + 1 col yields {@code 10 * 3 * 3 = 90}.
     *
     * @return the bonus amount that was added to the current score
     */
    public int addClearBonus(int rows, int cols) {
        int lines = rows + cols;
        int bonus = 10 * lines * Math.max(1, lines);
        currentScore += bonus;
        return bonus;
    }

    public int getCurrentScore() {
        return currentScore;
    }

    public int getBestScore() {
        return bestScore;
    }

    /**
     * Persists a new best score when the current run beats the stored one.
     */
    public void updateBestScore(ScoreRepository repository) {
        if (currentScore > bestScore) {
            bestScore = currentScore;
            repository.saveBestScore(bestScore);
        }
    }

    /**
     * Resets the current run's score to zero. The best score is preserved.
     */
    public void reset() {
        currentScore = 0;
    }
}
