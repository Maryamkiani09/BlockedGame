package com.blocked.model;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/**
 * Catalogue of every block shape used in the game plus the logic that builds
 * a fresh, randomly shuffled tray of three distinct pieces.
 * <p>All twenty shapes are constructed once in a static initializer and
 * shared immutably thereafter.
 */
public final class PieceGenerator {

    /** Canonical purple (no {@link Color} constant exists for it). */
    private static final Color PURPLE = new Color(128, 0, 128);

    /** All twenty block shapes, in the fixed order defined by the plan. */
    public static final BlockPiece[] ALL_PIECES;

    private static final Random RANDOM = new Random();

    static {
        ALL_PIECES = new BlockPiece[20];

        // 1. single cell
        ALL_PIECES[0] = new BlockPiece(new int[][]{{0, 0}}, Color.RED, "red", "SINGLE");
        // 2. horizontal 1x2
        ALL_PIECES[1] = new BlockPiece(new int[][]{{0, 0}, {0, 1}}, Color.BLUE, "blue", "LINE_1x2_H");
        // 3. vertical 2x1
        ALL_PIECES[2] = new BlockPiece(new int[][]{{0, 0}, {1, 0}}, Color.GREEN, "green", "LINE_2x1_V");
        // 4. horizontal 1x3
        ALL_PIECES[3] = new BlockPiece(new int[][]{{0, 0}, {0, 1}, {0, 2}}, Color.YELLOW, "yellow", "LINE_1x3_H");
        // 5. vertical 3x1
        ALL_PIECES[4] = new BlockPiece(new int[][]{{0, 0}, {1, 0}, {2, 0}}, Color.ORANGE, "orange", "LINE_3x1_V");
        // 6. horizontal 1x4
        ALL_PIECES[5] = new BlockPiece(new int[][]{{0, 0}, {0, 1}, {0, 2}, {0, 3}}, Color.ORANGE, "orange", "LINE_1x4_H");
        // 7. vertical 4x1
        ALL_PIECES[6] = new BlockPiece(new int[][]{{0, 0}, {1, 0}, {2, 0}, {3, 0}}, Color.CYAN, "cyan", "LINE_4x1_V");
        // 8. 2x2 square
        ALL_PIECES[7] = new BlockPiece(new int[][]{{0, 0}, {0, 1}, {1, 0}, {1, 1}}, Color.RED, "red", "SQUARE_2x2");
        // 9. horizontal 3x2 rectangle
        ALL_PIECES[8] = new BlockPiece(new int[][]{{0, 0}, {0, 1}, {0, 2}, {1, 0}, {1, 1}, {1, 2}}, Color.PINK, "pink", "RECT_3x2_H");
        // 10. vertical 2x3 rectangle
        ALL_PIECES[9] = new BlockPiece(new int[][]{{0, 0}, {0, 1}, {1, 0}, {1, 1}, {2, 0}, {2, 1}}, PURPLE, "purple", "RECT_2x3_V");
        // 11. L opening to the right at the bottom
        ALL_PIECES[10] = new BlockPiece(new int[][]{{0, 0}, {1, 0}, {2, 0}, {2, 1}}, Color.YELLOW, "yellow", "L_TOP_RIGHT");
        // 12. L opening to the left at the bottom
        ALL_PIECES[11] = new BlockPiece(new int[][]{{0, 1}, {1, 1}, {2, 1}, {2, 0}}, Color.BLUE, "blue", "L_TOP_LEFT");
        // 13. L opening to the right at the top
        ALL_PIECES[12] = new BlockPiece(new int[][]{{0, 0}, {0, 1}, {1, 0}, {2, 0}}, Color.GREEN, "green", "L_BOTTOM_RIGHT");
        // 14. L opening to the left at the top
        ALL_PIECES[13] = new BlockPiece(new int[][]{{0, 0}, {0, 1}, {1, 1}, {2, 1}}, Color.RED, "red", "L_BOTTOM_LEFT");
        // 15. T pointing up (stem on top)
        ALL_PIECES[14] = new BlockPiece(new int[][]{{1, 0}, {1, 1}, {1, 2}, {0, 1}}, Color.CYAN, "cyan", "T_TOP");
        // 16. T pointing down (stem below)
        ALL_PIECES[15] = new BlockPiece(new int[][]{{0, 0}, {0, 1}, {0, 2}, {1, 1}}, Color.ORANGE, "orange", "T_DOWN");
        // 17. T pointing left
        ALL_PIECES[16] = new BlockPiece(new int[][]{{0, 1}, {1, 0}, {1, 1}, {2, 1}}, PURPLE, "purple", "T_LEFT");
        // 18. T pointing right
        ALL_PIECES[17] = new BlockPiece(new int[][]{{0, 0}, {1, 0}, {1, 1}, {2, 0}}, Color.PINK, "pink", "T_RIGHT");
        // 19. S lying horizontal
        ALL_PIECES[18] = new BlockPiece(new int[][]{{0, 0}, {0, 1}, {1, 1}, {1, 2}}, Color.GREEN, "green", "S_HORIZONTAL");
        // 20. S standing vertical
        ALL_PIECES[19] = new BlockPiece(new int[][]{{0, 1}, {1, 0}, {1, 1}, {2, 0}}, Color.BLUE, "blue", "S_VERTICAL");
    }

    private PieceGenerator() {
        // utility class — no instances
    }

    /**
     * @return an array of exactly three distinct pieces chosen at random
     *         from {@link #ALL_PIECES} (the pool of indices is shuffled with
     *         {@link Random} and the first three are taken)
     */
    public static BlockPiece[] generateTray() {
        List<Integer> indices = new ArrayList<>(ALL_PIECES.length);
        for (int i = 0; i < ALL_PIECES.length; i++) {
            indices.add(i);
        }
        Collections.shuffle(indices, RANDOM);
        return new BlockPiece[]{
                ALL_PIECES[indices.get(0)],
                ALL_PIECES[indices.get(1)],
                ALL_PIECES[indices.get(2)]
        };
    }
}
