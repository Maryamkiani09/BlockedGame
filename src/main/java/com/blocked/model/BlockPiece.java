package com.blocked.model;

import java.awt.Color;

/**
 * Immutable description of a single placeable block: the cells it occupies
 * (each {@code {row, col}} relative to an origin), its colour, the block PNG
 * name used to look up its image asset, and a display name.
 */
public final class BlockPiece {

    private final int[][] cells;
    private final Color color;
    private final String colorName;
    private final String shapeName;

    /**
     * @param cells     each entry is {@code {row, col}} relative to the piece origin
     * @param color     fallback solid colour used when the PNG asset is missing
     * @param colorName block PNG filename without extension (e.g. {@code "red"})
     * @param shapeName human-readable shape identifier (e.g. {@code "L_TOP_RIGHT"})
     */
    public BlockPiece(int[][] cells, Color color, String colorName, String shapeName) {
        // defensive copy so callers cannot mutate the piece's shape afterwards
        this.cells = new int[cells.length][2];
        for (int i = 0; i < cells.length; i++) {
            this.cells[i][0] = cells[i][0];
            this.cells[i][1] = cells[i][1];
        }
        this.color = color;
        this.colorName = colorName;
        this.shapeName = shapeName;
    }

    /**
     * @return a fresh defensive copy of the cell offsets
     */
    public int[][] getCells() {
        int[][] copy = new int[cells.length][2];
        for (int i = 0; i < cells.length; i++) {
            copy[i][0] = cells[i][0];
            copy[i][1] = cells[i][1];
        }
        return copy;
    }

    public Color getColor() {
        return color;
    }

    public String getColorName() {
        return colorName;
    }

    public String getShapeName() {
        return shapeName;
    }

    /**
     * @return bounding-box height (max row index + 1)
     */
    public int getRows() {
        int max = 0;
        for (int[] cell : cells) {
            if (cell[0] > max) {
                max = cell[0];
            }
        }
        return max + 1;
    }

    /**
     * @return bounding-box width (max col index + 1)
     */
    public int getCols() {
        int max = 0;
        for (int[] cell : cells) {
            if (cell[1] > max) {
                max = cell[1];
            }
        }
        return max + 1;
    }
}
