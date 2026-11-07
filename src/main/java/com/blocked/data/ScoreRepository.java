package com.blocked.data;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Data-access layer: persists the best score to {@code data/best_score.txt},
 * resolved relative to the current working directory. Every failure is
 * caught and logged; the class never propagates exceptions to callers.
 */
public class ScoreRepository {

    private static final Logger LOGGER = Logger.getLogger(ScoreRepository.class.getName());

    /** Path is relative to CWD so it lines up with the exec:java working dir. */
    private static final String FILE_PATH = "data/best_score.txt";

    /**
     * Loads the best score from disk.
     * <p>Returns {@code 0} when the file is missing, empty, or holds text that
     * cannot be parsed as an integer.
     */
    public int loadBestScore() {
        try {
            Path path = Paths.get(FILE_PATH);
            if (!Files.exists(path)) {
                return 0;
            }
            String content = Files.readString(path).trim();
            if (content.isEmpty()) {
                return 0;
            }
            return Integer.parseInt(content);
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Failed to load best score from " + FILE_PATH, e);
            return 0;
        }
    }

    /**
     * Overwrites the best-score file with the supplied value, creating the
     * parent directory if it does not yet exist.
     */
    public void saveBestScore(int score) {
        try {
            Path path = Paths.get(FILE_PATH);
            Path parent = path.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
            Files.writeString(path, String.valueOf(score));
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Failed to save best score to " + FILE_PATH, e);
        }
    }
}
