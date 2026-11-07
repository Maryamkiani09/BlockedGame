package com.blocked.model;

import java.awt.Color;
import java.awt.Point;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * The 8x8 board model: a grid of placed block cells plus the logic to test
 * placement, detect completed lines, and clear them. Contains no UI and no
 * animation state — purely the board contents and queries over them.
 */
public class Board {

    public static final int SIZE = 8;

    private final Color[][] grid;
    private final String[][] colorNames;

    public Board() {
        grid = new Color[SIZE][SIZE];
        colorNames = new String[SIZE][SIZE];
    }

    /**
     * @return {@code true} when every cell the piece would occupy at the given
     *         origin is in-bounds and currently empty
     */
    public boolean canPlace(BlockPiece piece, int row, int col) {
        for (int[] cell : piece.getCells()) {
            int r = row + cell[0];
            int c = col + cell[1];
            if (r < 0 || r >= SIZE || c < 0 || c >= SIZE) {
                return false;
            }
            if (grid[r][c] != null) {
                return false;
            }
        }
        return true;
    }

    /**
     * Stamps the piece onto the board at the given origin. The caller should
     * verify placement with {@link #canPlace} first; this method fills cells
     * unconditionally.
     */
    public void place(BlockPiece piece, int row, int col) {
        for (int[] cell : piece.getCells()) {
            int r = row + cell[0];
            int c = col + cell[1];
            grid[r][c] = piece.getColor();
            colorNames[r][c] = piece.getColorName();
        }
    }

    /**
     * @return indices of every row that has no empty cells
     */
    public List<Integer> getCompletedRows() {
        List<Integer> rows = new ArrayList<>();
        for (int r = 0; r < SIZE; r++) {
            boolean full = true;
            for (int c = 0; c < SIZE; c++) {
                if (grid[r][c] == null) {
                    full = false;
                    break;
                }
            }
            if (full) {
                rows.add(r);
            }
        }
        return rows;
    }

    /**
     * @return indices of every column that has no empty cells
     */
    public List<Integer> getCompletedCols() {
        List<Integer> cols = new ArrayList<>();
        for (int c = 0; c < SIZE; c++) {
            boolean full = true;
            for (int r = 0; r < SIZE; r++) {
                if (grid[r][c] == null) {
                    full = false;
                    break;
                }
            }
            if (full) {
                cols.add(c);
            }
        }
        return cols;
    }

    /**
     * Clears the union of the supplied completed rows and columns. Cells that
     * lie in both a completed row and a completed column are collected into a
     * {@link Set} of points and therefore removed exactly once.
     *
     * @param rows completed row indices (may be empty or {@code null})
     * @param cols completed column indices (may be empty or {@code null})
     */
    public void clearLines(List<Integer> rows, List<Integer> cols) {
        Set<Point> toClear = new HashSet<>();
        if (rows != null) {
            for (int r : rows) {
                for (int c = 0; c < SIZE; c++) {
                    toClear.add(new Point(c, r)); // x = col, y = row
                }
            }
        }
        if (cols != null) {
            for (int c : cols) {
                for (int r = 0; r < SIZE; r++) {
                    toClear.add(new Point(c, r));
                }
            }
        }
        for (Point p : toClear) {
            grid[p.y][p.x] = null;
            colorNames[p.y][p.x] = null;
        }
    }

    /**
     * @return {@code true} if the cell is within bounds and currently filled
     */
    public boolean isCellFilled(int row, int col) {
        if (row < 0 || row >= SIZE || col < 0 || col >= SIZE) {
            return false;
        }
        return grid[row][col] != null;
    }

    /**
     * @return the colour of the cell, or {@code null} if empty or out of bounds
     */
    public Color getCellColor(int row, int col) {
        if (row < 0 || row >= SIZE || col < 0 || col >= SIZE) {
            return null;
        }
        return grid[row][col];
    }

    /**
     * @return the colour-name of the cell, or {@code null} if empty or out of bounds
     */
    public String getCellColorName(int row, int col) {
        if (row < 0 || row >= SIZE || col < 0 || col >= SIZE) {
            return null;
        }
        return colorNames[row][col];
    }

    /**
     * @return {@code true} when at least one non-null piece in the tray can be
     *         legally placed somewhere on the board (used as the game-over check)
     */
    public boolean canAnyPieceFit(BlockPiece[] tray) {
        if (tray == null) {
            return false;
        }
        for (BlockPiece piece : tray) {
            if (piece == null) {
                continue;
            }
            for (int r = 0; r < SIZE; r++) {
                for (int c = 0; c < SIZE; c++) {
                    if (canPlace(piece, r, c)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    /**
     * @return a defensive copy of the current grid (callers may mutate freely)
     */
    public Color[][] getGridSnapshot() {
        Color[][] copy = new Color[SIZE][SIZE];
        for (int r = 0; r < SIZE; r++) {
            System.arraycopy(grid[r], 0, copy[r], 0, SIZE);
        }
        return copy;
    }

    /**
     * Empties the entire board.
     */
    public void reset() {
        for (int r = 0; r < SIZE; r++) {
            for (int c = 0; c < SIZE; c++) {
                grid[r][c] = null;
                colorNames[r][c] = null;
            }
        }
    }
}
