package com.blocked.engine;

import com.blocked.data.ScoreRepository;
import com.blocked.model.AnimationState;
import com.blocked.model.AnimationState.ScorePopupEntry;
import com.blocked.model.BlockPiece;
import com.blocked.model.Board;
import com.blocked.model.GameMode;
import com.blocked.model.GameState;
import com.blocked.model.PieceGenerator;
import com.blocked.model.ScoreManager;

import java.awt.Point;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;

/**
 * Authoritative state machine for the BLOCKED game.
 * <p>
 * Invariants:
 * <ul>
 *   <li>Zero {@code javax.swing} imports — engine is pure model/logic.</li>
 *   <li>{@link #tick(long)} never calls any drawing method.</li>
 *   <li>Placement is rejected (piece stays in tray) when
 *       {@link Board#canPlace} returns {@code false}.</li>
 *   <li>Flash animation defers {@link Board#clearLines} until the flash
 *       expires inside {@link #tick(long)}.</li>
 *   <li>Pause suspends elapsed-time accumulation; resume resumes it without
 *       a time skip.</li>
 *   <li>TIME_ATTACK triggers GAME_OVER when {@code timeAttackRemaining==0}
 *       and {@code score < 500}.</li>
 * </ul>
 */
public class GameEngine {

    private static final Logger LOGGER = Logger.getLogger(GameEngine.class.getName());

    // -------------------------------------------------------------------------
    // Injected collaborators
    // -------------------------------------------------------------------------

    private final Board           board;
    private final ScoreManager    scoreManager;
    private final ScoreRepository scoreRepository;
    private final SoundManager    soundManager;
    private final AnimationState  animationState;

    // -------------------------------------------------------------------------
    // Tray slot layout — used by mouse-press hit-test (set by UI callers)
    // -------------------------------------------------------------------------

    /** Pixel X of each tray slot's top-left corner (set by UI on each repaint). */
    private int[] traySlotX = new int[3];
    /** Pixel Y of each tray slot's top-left corner (set by UI on each repaint). */
    private int[] traySlotY = new int[3];
    /** Pixel width and height of each tray slot. */
    private int traySlotSize = 100;

    // -------------------------------------------------------------------------
    // State machine
    // -------------------------------------------------------------------------

    private GameState gameState = GameState.MENU;
    private GameMode  gameMode;

    // -------------------------------------------------------------------------
    // Tray and selection
    // -------------------------------------------------------------------------

    private BlockPiece[] tray = new BlockPiece[3];
    private int selectedPieceIndex = -1;

    // -------------------------------------------------------------------------
    // Drag state
    // -------------------------------------------------------------------------

    private boolean isDragging   = false;
    private int     dragScreenX  = 0;
    private int     dragScreenY  = 0;

    // -------------------------------------------------------------------------
    // Keyboard-driven preview position (board grid coordinates)
    // -------------------------------------------------------------------------

    private int previewRow = 0;
    private int previewCol = 0;

    // -------------------------------------------------------------------------
    // Timing
    // -------------------------------------------------------------------------

    private long gameStartMs    = 0;
    private long pausedAtMs     = 0;
    private long totalPausedMs  = 0;
    private long elapsedMs      = 0;

    // -------------------------------------------------------------------------
    // Time Attack
    // -------------------------------------------------------------------------

    private int timeAttackRemaining = GameMode.TIME_ATTACK_SECONDS;

    // -------------------------------------------------------------------------
    // Deferred line-clear (for flash animation)
    // -------------------------------------------------------------------------

    /** Row indices to clear once the flash expires. */
    private List<Integer> pendingClearRows = new ArrayList<>();
    /** Column indices to clear once the flash expires. */
    private List<Integer> pendingClearCols = new ArrayList<>();

    // -------------------------------------------------------------------------
    // Board layout cache (filled by UI each paint cycle via setLayoutHints)
    // -------------------------------------------------------------------------

    /** Pixel X of the board's top-left corner. */
    private int boardOffX  = 0;
    /** Pixel Y of the board's top-left corner. */
    private int boardOffY  = 0;
    /** Pixel size of one board cell. */
    private int cellSize   = 50;

    // =========================================================================
    // Constructor
    // =========================================================================

    /**
     * @param board           the 8×8 board model
     * @param scoreManager    score tracker for the current run
     * @param pieceGenerator  not stored — only used via static methods
     * @param scoreRepository persistence for best score
     * @param soundManager    sound effects
     * @param animationState  shared animation bag read by the renderer
     */
    public GameEngine(Board board,
                      ScoreManager scoreManager,
                      PieceGenerator pieceGenerator,   // kept for API symmetry; static methods used directly
                      ScoreRepository scoreRepository,
                      SoundManager soundManager,
                      AnimationState animationState) {
        this.board           = board;
        this.scoreManager    = scoreManager;
        this.scoreRepository = scoreRepository;
        this.soundManager    = soundManager;
        this.animationState  = animationState;
    }

    // =========================================================================
    // Layout hints (called by UI before processing events)
    // =========================================================================

    /**
     * Tells the engine where the board is drawn so mouse-to-grid conversion
     * works correctly. The UI calls this once per repaint.
     */
    public void setLayoutHints(int boardOffX, int boardOffY, int cellSize,
                               int[] traySlotX, int[] traySlotY, int traySlotSize) {
        this.boardOffX     = boardOffX;
        this.boardOffY     = boardOffY;
        this.cellSize      = cellSize;
        this.traySlotX     = traySlotX;
        this.traySlotY     = traySlotY;
        this.traySlotSize  = traySlotSize;
    }

    // =========================================================================
    // Game lifecycle
    // =========================================================================

    /**
     * Starts or restarts a game in the supplied mode.
     * Resets the board, score, tray, and all timing state.
     */
    public void startGame(GameMode mode) {
        this.gameMode = mode;
        board.reset();
        scoreManager.reset();
        tray = PieceGenerator.generateTray();
        gameState              = GameState.PLAYING;
        gameStartMs            = System.currentTimeMillis();
        totalPausedMs          = 0;
        elapsedMs              = 0;
        selectedPieceIndex     = -1;
        isDragging             = false;
        previewRow             = 0;
        previewCol             = 0;
        pendingClearRows       = new ArrayList<>();
        pendingClearCols       = new ArrayList<>();
        animationState.flashActive = false;
        animationState.popActive   = false;
        animationState.scorePopups.clear();

        if (mode == GameMode.TIME_ATTACK) {
            timeAttackRemaining = GameMode.TIME_ATTACK_SECONDS;
        }

        LOGGER.info("Game started: mode=" + mode);
    }

    // =========================================================================
    // Tick (called by Swing Timer at ~60 Hz — NO drawing here)
    // =========================================================================

    /**
     * Advances all time-dependent engine state.
     * Must never call any rendering method.
     *
     * @param nowMs current time from {@code System.currentTimeMillis()}
     */
    public void tick(long nowMs) {
        if (gameState != GameState.PLAYING) {
            return;
        }

        // --- Elapsed time --------------------------------------------------
        elapsedMs = nowMs - gameStartMs - totalPausedMs;

        // --- Time Attack countdown -----------------------------------------
        if (gameMode == GameMode.TIME_ATTACK) {
            timeAttackRemaining = Math.max(0,
                    GameMode.TIME_ATTACK_SECONDS - (int) (elapsedMs / 1000));

            if (timeAttackRemaining == 0
                    && scoreManager.getCurrentScore() < GameMode.TIME_ATTACK_TARGET) {
                triggerGameOver();
                return;
            }
        }

        // --- Flash animation expiry ---------------------------------------
        if (animationState.flashActive
                && (nowMs - animationState.flashStartMs) >= AnimationState.FLASH_DURATION_MS) {
            // Deferred line clear — happens here, not in attemptPlacement
            board.clearLines(pendingClearRows, pendingClearCols);
            pendingClearRows.clear();
            pendingClearCols.clear();
            animationState.flashActive = false;
            animationState.flashCells.clear();

            // After clearing, check whether the tray is exhausted / no fit
            checkTrayAndFit();
        }

        // --- Pop animation expiry -----------------------------------------
        if (animationState.popActive
                && (nowMs - animationState.popStartMs) >= AnimationState.POP_DURATION_MS) {
            animationState.popActive = false;
            animationState.popCells.clear();
        }

        // --- Score popup expiry -------------------------------------------
        Iterator<ScorePopupEntry> it = animationState.scorePopups.iterator();
        while (it.hasNext()) {
            ScorePopupEntry entry = it.next();
            if ((nowMs - entry.startMs) >= ScorePopupEntry.POPUP_DURATION_MS) {
                it.remove();
            }
        }
    }

    // =========================================================================
    // Placement logic
    // =========================================================================

    /**
     * Attempts to stamp the currently selected piece at the given board cell.
     * Rejects silently with an invalid sound if {@code canPlace} returns false.
     */
    private void attemptPlacement(int row, int col) {
        if (selectedPieceIndex < 0 || selectedPieceIndex >= tray.length) {
            return;
        }
        BlockPiece piece = tray[selectedPieceIndex];
        if (piece == null) {
            return;
        }

        // Bounds-check the origin so partial pieces don't wrap
        if (row < 0 || row >= Board.SIZE || col < 0 || col >= Board.SIZE) {
            soundManager.playInvalid();
            LOGGER.fine("Placement rejected (out of bounds): row=" + row + " col=" + col);
            return;
        }

        if (!board.canPlace(piece, row, col)) {
            soundManager.playInvalid();
            LOGGER.fine("Placement rejected: piece=" + piece.getShapeName()
                    + " row=" + row + " col=" + col);
            return;
        }

        // --- Stamp the piece ----------------------------------------------
        board.place(piece, row, col);

        // Pop animation — highlight placed cells
        long now = System.currentTimeMillis();
        Set<Point> placed = new HashSet<>();
        for (int[] cell : piece.getCells()) {
            placed.add(new Point(col + cell[1], row + cell[0]));   // x=col, y=row
        }
        animationState.popCells    = placed;
        animationState.popStartMs  = now;
        animationState.popActive   = true;

        // Score: one point per cell
        scoreManager.addCellPoints(piece.getCells().length);

        // Detect completed lines
        List<Integer> completedRows = board.getCompletedRows();
        List<Integer> completedCols = board.getCompletedCols();

        if (!completedRows.isEmpty() || !completedCols.isEmpty()) {
            // Bonus score
            int bonus = scoreManager.addClearBonus(completedRows.size(), completedCols.size());

            // Build the set of cells to flash
            Set<Point> flashCells = new HashSet<>();
            for (int r : completedRows) {
                for (int c = 0; c < Board.SIZE; c++) {
                    flashCells.add(new Point(c, r));
                }
            }
            for (int c : completedCols) {
                for (int r = 0; r < Board.SIZE; r++) {
                    flashCells.add(new Point(c, r));
                }
            }
            animationState.flashCells   = flashCells;
            animationState.flashStartMs = now;
            animationState.flashActive  = true;

            // Store rows/cols for deferred clearLines inside tick()
            pendingClearRows = new ArrayList<>(completedRows);
            pendingClearCols = new ArrayList<>(completedCols);

            // Score popup — anchor to board-pixel coords of the placed origin
            float popX = boardOffX + col * cellSize + cellSize / 2f;
            float popY = boardOffY + row * cellSize - cellSize / 2f;
            ScorePopupEntry popup = new ScorePopupEntry();
            popup.text    = "+" + bonus;
            popup.x       = popX;
            popup.y       = popY;
            popup.startMs = now;
            animationState.scorePopups.add(popup);

            soundManager.playClear();
        } else {
            soundManager.playPlace();
        }

        // Remove piece from tray
        tray[selectedPieceIndex] = null;
        selectedPieceIndex = -1;

        LOGGER.fine("Placed: piece=" + piece.getShapeName()
                + " row=" + row + " col=" + col
                + " completedRows=" + completedRows
                + " completedCols=" + completedCols);

        // If flash is not active (no lines cleared), check tray/fit immediately
        if (!animationState.flashActive) {
            checkTrayAndFit();
        }
        // If flash IS active, checkTrayAndFit() will be called in tick() after clearLines
    }

    /**
     * Refills the tray when all slots are empty, then checks whether any
     * remaining piece can be placed. If not, determines win or game-over.
     */
    private void checkTrayAndFit() {
        if (gameState != GameState.PLAYING) {
            return;
        }

        // Refill tray when all three slots are consumed
        boolean allNull = true;
        for (BlockPiece bp : tray) {
            if (bp != null) {
                allNull = false;
                break;
            }
        }
        if (allNull) {
            tray = PieceGenerator.generateTray();
            LOGGER.fine("Tray refilled.");
        }

        // Collect non-null pieces for fit check
        List<BlockPiece> nonNull = new ArrayList<>();
        for (BlockPiece bp : tray) {
            if (bp != null) {
                nonNull.add(bp);
            }
        }

        if (!board.canAnyPieceFit(nonNull.toArray(new BlockPiece[0]))) {
            // Classic: no fit → game over;  Time Attack: reach target → win
            if (gameMode == GameMode.TIME_ATTACK
                    && scoreManager.getCurrentScore() >= GameMode.TIME_ATTACK_TARGET) {
                triggerWin();
            } else if (gameMode == GameMode.CLASSIC) {
                triggerGameOver();
            } else {
                // TIME_ATTACK and score < target and no fit → game over
                triggerGameOver();
            }
        }
    }

    // =========================================================================
    // Mouse input
    // =========================================================================

    /**
     * Handles a mouse-button-pressed event from the playing screen.
     */
    public void handleMousePressed(int sx, int sy,
                                   int boardOffX, int boardOffY, int cellSize) {
        if (gameState != GameState.PLAYING) {
            return;
        }
        for (int i = 0; i < tray.length; i++) {
            if (tray[i] == null) {
                continue;
            }
            int slotX = traySlotX[i];
            int slotY = traySlotY[i];
            int slotW = traySlotSize;
            int slotH = traySlotSize;
            if (sx >= slotX && sx < slotX + slotW
                    && sy >= slotY && sy < slotY + slotH) {
                selectedPieceIndex = i;
                isDragging         = true;
                dragScreenX        = sx;
                dragScreenY        = sy;
                // Seed preview position from current drag coords
                previewRow = computeDragOrigin(false);
                previewCol = computeDragOrigin(true);
                soundManager.playSelect();
                LOGGER.fine("Piece selected: index=" + i);
                return;
            }
        }
    }

    /**
     * Handles mouse-drag events; updates drag position and preview cell.
     */
    public void handleMouseDragged(int sx, int sy) {
        if (!isDragging) {
            return;
        }
        dragScreenX = sx;
        dragScreenY = sy;
        previewRow  = computeDragOrigin(false);
        previewCol  = computeDragOrigin(true);
    }

    /**
     * Grid cell where the dragged piece's top-left cell would land. The piece
     * is held by its centre, snapped to the nearest cell, and clamped so the
     * whole piece stays on the board. The renderer and the drop logic both use
     * this, so what you see is exactly where the piece is placed.
     */
    public int getDragOriginRow() { return computeDragOrigin(false); }
    public int getDragOriginCol() { return computeDragOrigin(true); }

    private int computeDragOrigin(boolean horizontal) {
        BlockPiece p = (selectedPieceIndex >= 0 && selectedPieceIndex < tray.length)
                ? tray[selectedPieceIndex] : null;
        int cells = (p == null) ? 1 : (horizontal ? p.getCols() : p.getRows());
        int cs = Math.max(1, cellSize);
        double pos = horizontal ? (dragScreenX - boardOffX) : (dragScreenY - boardOffY);
        int origin = (int) Math.floor((pos - cells * cs / 2.0) / cs + 0.5);
        return clamp(origin, 0, Board.SIZE - cells);
    }

    /**
     * Handles mouse-button-released events; attempts placement if the drop
     * lands inside the board.
     */
    public void handleMouseReleased(int sx, int sy,
                                    int boardOffX, int boardOffY, int cellSize) {
        if (!isDragging) {
            return;
        }
        dragScreenX = sx;
        dragScreenY = sy;
        isDragging = false;

        int boardPx = Board.SIZE * Math.max(1, cellSize);
        boolean insideBoard = sx >= boardOffX && sx < boardOffX + boardPx
                && sy >= boardOffY && sy < boardOffY + boardPx;

        // Drop where the snapped preview was showing
        if (insideBoard) {
            attemptPlacement(computeDragOrigin(false), computeDragOrigin(true));
        } else {
            LOGGER.fine("Drop outside board — piece returned to tray.");
        }
    }

    // =========================================================================
    // Keyboard input
    // =========================================================================

    /**
     * Handles key-press events forwarded from the UI.
     *
     * @param keyCode {@code java.awt.event.KeyEvent} VK_ constant
     */
    public void handleKeyPress(int keyCode) {
        // KeyEvent constants (numeric literals keep this class Swing-free)
        final int VK_1     = 49;
        final int VK_2     = 50;
        final int VK_3     = 51;
        final int VK_UP    = 38;
        final int VK_DOWN  = 40;
        final int VK_LEFT  = 37;
        final int VK_RIGHT = 39;
        final int VK_ENTER = 10;
        final int VK_SPACE = 32;
        final int VK_P     = 80;
        final int VK_R     = 82;
        final int VK_M     = 77;

        if (keyCode == VK_1 && gameState == GameState.PLAYING) {
            selectTrayPiece(0);
        } else if (keyCode == VK_2 && gameState == GameState.PLAYING) {
            selectTrayPiece(1);
        } else if (keyCode == VK_3 && gameState == GameState.PLAYING) {
            selectTrayPiece(2);
        } else if (keyCode == VK_UP && gameState == GameState.PLAYING) {
            previewRow = Math.max(0, previewRow - 1);
        } else if (keyCode == VK_DOWN && gameState == GameState.PLAYING) {
            previewRow = Math.min(Board.SIZE - 1, previewRow + 1);
        } else if (keyCode == VK_LEFT && gameState == GameState.PLAYING) {
            previewCol = Math.max(0, previewCol - 1);
        } else if (keyCode == VK_RIGHT && gameState == GameState.PLAYING) {
            previewCol = Math.min(Board.SIZE - 1, previewCol + 1);
        } else if ((keyCode == VK_ENTER || keyCode == VK_SPACE)
                && gameState == GameState.PLAYING) {
            if (selectedPieceIndex >= 0) {
                attemptPlacement(previewRow, previewCol);
            }
        } else if (keyCode == VK_P) {
            if (gameState == GameState.PLAYING) {
                pause();
            } else if (gameState == GameState.PAUSED) {
                resume();
            }
        } else if (keyCode == VK_R) {
            if (gameState == GameState.PLAYING
                    || gameState == GameState.PAUSED
                    || gameState == GameState.GAME_OVER) {
                restart();
            }
        } else if (keyCode == VK_M) {
            soundManager.toggleMute();
        }
    }

    // =========================================================================
    // Pause / Resume / Restart / Menu navigation
    // =========================================================================

    /** Flips the mute state (used by the on-screen speaker button). */
    public void toggleMute() {
        soundManager.toggleMute();
    }

    /** Suspends elapsed-time accumulation. */
    public void pause() {
        if (gameState == GameState.PLAYING) {
            pausedAtMs = System.currentTimeMillis();
            gameState  = GameState.PAUSED;
            LOGGER.info("Game paused.");
        }
    }

    /** Resumes elapsed-time accumulation without a time skip. */
    public void resume() {
        if (gameState == GameState.PAUSED) {
            long now       = System.currentTimeMillis();
            totalPausedMs += (now - pausedAtMs);
            gameState      = GameState.PLAYING;
            LOGGER.info("Game resumed. totalPausedMs=" + totalPausedMs);
        }
    }

    /** Restarts the current mode from scratch. */
    public void restart() {
        if (gameMode != null) {
            startGame(gameMode);
        }
    }

    /** Returns to the main menu. */
    public void goToMenu() {
        gameState = GameState.MENU;
        LOGGER.info("Returned to menu.");
    }

    /** Navigates from menu to mode-select screen. */
    public void setModeSelectState() {
        gameState = GameState.MODE_SELECT;
        LOGGER.info("Navigated to mode select.");
    }

    /** Navigates to the instructions screen. */
    public void setInstructionsState() {
        gameState = GameState.INSTRUCTIONS;
        LOGGER.info("Navigated to instructions.");
    }

    // =========================================================================
    // Terminal states
    // =========================================================================

    private void triggerGameOver() {
        gameState = GameState.GAME_OVER;
        scoreManager.updateBestScore(scoreRepository);
        LOGGER.info("Game over. score=" + scoreManager.getCurrentScore()
                + " best=" + scoreManager.getBestScore());
    }

    private void triggerWin() {
        gameState = GameState.WIN;
        scoreManager.updateBestScore(scoreRepository);
        LOGGER.info("Win! score=" + scoreManager.getCurrentScore()
                + " best=" + scoreManager.getBestScore());
    }

    // =========================================================================
    // Getters required by GamePanel
    // =========================================================================

    public GameState getGameState()               { return gameState; }
    public GameMode  getGameMode()                { return gameMode; }
    public Board     getBoard()                   { return board; }
    public BlockPiece[] getTray()                 { return tray; }
    public int       getSelectedPieceIndex()      { return selectedPieceIndex; }
    public int       getPreviewRow()              { return previewRow; }
    public int       getPreviewCol()              { return previewCol; }
    public boolean   isDragging()                 { return isDragging; }
    public int       getDragScreenX()             { return dragScreenX; }
    public int       getDragScreenY()             { return dragScreenY; }
    public int       getCurrentScore()            { return scoreManager.getCurrentScore(); }
    public int       getBestScore()               { return scoreManager.getBestScore(); }
    public long      getElapsedMs()               { return elapsedMs; }
    public int       getTimeAttackRemainingSeconds() { return timeAttackRemaining; }
    public AnimationState getAnimationState()     { return animationState; }
    public boolean   isMuted()                    { return soundManager.isMuted(); }

    // =========================================================================
    // Helpers
    // =========================================================================

    private void selectTrayPiece(int index) {
        if (index >= 0 && index < tray.length && tray[index] != null) {
            selectedPieceIndex = index;
            soundManager.playSelect();
            LOGGER.fine("Piece selected by key: index=" + index);
        }
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }
}