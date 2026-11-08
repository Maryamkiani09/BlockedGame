package com.blocked.ui;

import com.blocked.data.ResourceLoader;
import com.blocked.engine.GameEngine;
import com.blocked.model.AnimationState;
import com.blocked.model.AnimationState.ScorePopupEntry;
import com.blocked.model.BlockPiece;
import com.blocked.model.Board;
import com.blocked.model.GameMode;
import com.blocked.model.GameState;

import javax.swing.JPanel;
import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Composite;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Paint;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.util.HashMap;
import java.util.Map;

/**
 * Single JPanel that renders every screen of the BLOCKED game.
 * <p>
 * Layering contract:
 * <ul>
 *   <li>{@link #paintComponent} ONLY draws. It never mutates the engine, the
 *       board, or any model object — it reads state through engine getters.</li>
 *   <li>All game logic lives in {@link GameEngine}; this class merely projects
 *       that state onto the screen.</li>
 *   <li>Board/tray geometry is fixed (compile-time constants) so the engine's
 *       mouse hit-test matches what is drawn.</li>
 *   <li>Every PNG load is null-checked and falls back to programmatic drawing
 *       so a missing asset never crashes the game.</li>
 * </ul>
 */
public class GamePanel extends JPanel {

    private static final long serialVersionUID = 1L;

    // ------------------------------------------------------------------
    // Window / layout constants
    // ------------------------------------------------------------------

    public static final int WINDOW_WIDTH  = 900;
    public static final int WINDOW_HEIGHT = 700;

    /** Board top-left and cell size: 8 * 60 = 480px board. */
    public static final int BOARD_OFFSET_X = 50;
    public static final int BOARD_OFFSET_Y = 80;
    public static final int CELL_SIZE       = 60;

    /** Tray sits below the board. */
    public static final int TRAY_Y         = 580;
    public static final int TRAY_SLOT_SIZE  = 110;
    public static final int TRAY_GAP       = 10;

    /** Score panel sits to the right of the board. */
    public static final int SCORE_PANEL_X = 570;
    public static final int SCORE_PANEL_W = 280;
    public static final int SCORE_PANEL_H = 270;

    /** Size of a cell when a piece is drawn "in hand" at the cursor. */
    private static final int DRAG_GHOST_CELL = 28;

    // ------------------------------------------------------------------
    // Colours
    // ------------------------------------------------------------------

    private static final Color BG          = new Color(0x1a1a2e);
    private static final Color CELL_BG      = new Color(0x16213e);
    private static final Color GRID         = new Color(0x0f3460);
    private static final Color PANEL_BORDER = new Color(0x39486b);
    private static final Color LABEL_FG     = new Color(0x8aa0c8);
    private static final Color TEXT_FG      = new Color(0xcdd6f4);

    // ------------------------------------------------------------------
    // Dependencies
    // ------------------------------------------------------------------

    private final GameEngine engine;

    // ------------------------------------------------------------------
    // Loaded assets (any field may be null — callers fall back to drawing)
    // ------------------------------------------------------------------

    private final Map<String, BufferedImage> blockImages = new HashMap<>();

    private BufferedImage imgScoreBg;
    private BufferedImage imgSlotBg;
    private BufferedImage imgBack;
    private BufferedImage imgBest;
    private BufferedImage imgClassic;
    private BufferedImage imgExit;
    private BufferedImage imgGameOver;
    private BufferedImage imgInstructions;
    private BufferedImage imgMenu;
    private BufferedImage imgMute;
    private BufferedImage imgPause;
    private BufferedImage imgPlay;
    private BufferedImage imgRestart;
    private BufferedImage imgRestartIcon;
    private BufferedImage imgResume;
    private BufferedImage imgTimeAttack;
    private BufferedImage imgWin;

    // ------------------------------------------------------------------
    // Fonts
    // ------------------------------------------------------------------

    private Font titleFont;       // 72pt
    private Font headingFont;     // 40pt
    private Font subHeadingFont;  // 30pt
    private Font labelFont;       // 22pt
    private Font scoreValueFont;  // 44pt
    private Font scoreValue2Font; // 28pt
    private Font smallFont;       // 16pt

    // ------------------------------------------------------------------
    // Hit rectangles (written while drawing, read by handleClick)
    // ------------------------------------------------------------------

    private Rectangle playRect;
    private Rectangle instructRect;
    private Rectangle exitRect;
    private Rectangle classicRect;
    private Rectangle timeAttackRect;
    private Rectangle backRect;
    private Rectangle resumeRect;
    private Rectangle restartRect;
    private Rectangle menuRect;

    // =========================================================================
    // Construction
    // =========================================================================

    public GamePanel(GameEngine engine) {
        this.engine = engine;

        loadImages();
        loadFonts();

        setPreferredSize(new java.awt.Dimension(WINDOW_WIDTH, WINDOW_HEIGHT));
        setBackground(BG);
        setDoubleBuffered(true);
        setFocusable(true);

        // Tell the engine where the board and tray slots are so its mouse
        // hit-testing matches what is drawn. Done once here (fixed geometry),
        // never inside paintComponent — that would violate the draw-only rule.
        int[] traySlotX = new int[3];
        int[] traySlotY = new int[3];
        for (int i = 0; i < 3; i++) {
            traySlotX[i] = BOARD_OFFSET_X + i * (TRAY_SLOT_SIZE + TRAY_GAP);
            traySlotY[i] = TRAY_Y;
        }
        engine.setLayoutHints(BOARD_OFFSET_X, BOARD_OFFSET_Y, CELL_SIZE,
                traySlotX, traySlotY, TRAY_SLOT_SIZE);
    }

    private void loadImages() {
        String[] colors = {"blue", "green", "orange", "pink", "purple", "red", "yellow"};
        for (String c : colors) {
            BufferedImage img = ResourceLoader.loadImage("/images/blocks/" + c + ".png");
            if (img != null) {
                blockImages.put(c, img);
            }
        }

        imgScoreBg      = ResourceLoader.loadImage("/images/score/background.png");
        imgSlotBg       = ResourceLoader.loadImage("/images/slot/background.png");
        imgMenu         = ResourceLoader.loadImage("/images/menu/button.png");
        imgPause        = ResourceLoader.loadImage("/images/pause/button.png");
        imgResume       = ResourceLoader.loadImage("/images/resume/button.png");
        imgRestart      = ResourceLoader.loadImage("/images/restart/button.png");
        imgRestartIcon  = ResourceLoader.loadImage("/images/restart/icon.png");
        imgGameOver     = ResourceLoader.loadImage("/images/game-over/cross.png");
        imgWin          = ResourceLoader.loadImage("/images/win/check.png");
        imgBest         = ResourceLoader.loadImage("/images/best/star.png");
        imgClassic      = ResourceLoader.loadImage("/images/classic/button.png");
        imgTimeAttack   = ResourceLoader.loadImage("/images/time-attack/button.png");
        imgInstructions = ResourceLoader.loadImage("/images/instructions/button.png");
        imgBack         = ResourceLoader.loadImage("/images/back/button.png");
        imgExit         = ResourceLoader.loadImage("/images/exit/button.png");
        imgMute         = ResourceLoader.loadImage("/images/mute/button.png");
        imgPlay         = ResourceLoader.loadImage("/images/play/icon.png");
    }

    private void loadFonts() {
        Font base = ResourceLoader.loadFont("/fonts/Kenney Future Narrow.ttf", 36f);
        if (base == null) {
            base = new Font("Arial", Font.BOLD, 36);
        }
        titleFont       = base.deriveFont(72f);
        headingFont      = base.deriveFont(40f);
        subHeadingFont   = base.deriveFont(30f);
        labelFont        = base.deriveFont(22f);
        scoreValueFont   = base.deriveFont(44f);
        scoreValue2Font  = base.deriveFont(28f);
        smallFont        = base.deriveFont(16f);
    }

    // =========================================================================
    // paintComponent — dispatch only, zero mutation
    // =========================================================================

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g.create();
        try {
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);

            long nowMs = System.currentTimeMillis();

            drawBackground(g2);

            switch (engine.getGameState()) {
                case MENU:
                    drawMainMenu(g2);
                    break;
                case MODE_SELECT:
                    drawModeSelect(g2);
                    break;
                case INSTRUCTIONS:
                    drawInstructions(g2);
                    break;
                case PLAYING:
                    drawGame(g2, nowMs);
                    break;
                case PAUSED:
                    drawGame(g2, nowMs);
                    drawPauseOverlay(g2);
                    break;
                case GAME_OVER:
                    drawGame(g2, nowMs);
                    drawGameOverOverlay(g2);
                    break;
                case WIN:
                    drawGame(g2, nowMs);
                    drawWinOverlay(g2);
                    break;
                default:
                    break;
            }

            drawMuteIndicator(g2);
        } finally {
            g2.dispose();
        }
    }

    // =========================================================================
    // Background
    // =========================================================================

    private void drawBackground(Graphics2D g2) {
        g2.setColor(BG);
        g2.fillRect(0, 0, getWidth(), getHeight());
    }

    // =========================================================================
    // In-game screen
    // =========================================================================

    private void drawGame(Graphics2D g2, long nowMs) {
        drawBoard(g2);
        drawPreview(g2);
        drawDragGhost(g2);
        drawFlashAnimation(g2, nowMs);
        drawPopAnimation(g2, nowMs);
        drawScorePopups(g2, nowMs);
        drawTray(g2);
        drawScorePanel(g2);
        drawModeBanner(g2);
    }

    private void drawBoard(Graphics2D g2) {
        Board board = engine.getBoard();
        int boardPx = Board.SIZE * CELL_SIZE;

        // Board container background
        g2.setColor(new Color(0x0f3460));
        g2.fillRoundRect(BOARD_OFFSET_X - 4, BOARD_OFFSET_Y - 4, boardPx + 8, boardPx + 8, 8, 8);

        for (int r = 0; r < Board.SIZE; r++) {
            for (int c = 0; c < Board.SIZE; c++) {
                int x = BOARD_OFFSET_X + c * CELL_SIZE;
                int y = BOARD_OFFSET_Y + r * CELL_SIZE;

                // Cell background (1px gap shows the container colour)
                g2.setColor(CELL_BG);
                g2.fillRect(x, y, CELL_SIZE - 1, CELL_SIZE - 1);

                if (board.isCellFilled(r, c)) {
                    String name = board.getCellColorName(r, c);
                    BufferedImage img = (name == null) ? null : blockImages.get(name);
                    if (img != null) {
                        g2.drawImage(img, x + 1, y + 1, CELL_SIZE - 2, CELL_SIZE - 2, null);
                    } else {
                        Color col = board.getCellColor(r, c);
                        g2.setColor(col != null ? col : Color.GRAY);
                        g2.fillRoundRect(x + 2, y + 2, CELL_SIZE - 4, CELL_SIZE - 4, 8, 8);
                    }
                }
            }
        }

        drawGrid(g2);
    }

    private void drawGrid(Graphics2D g2) {
        Composite old = g2.getComposite();
        g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.5f));
        g2.setColor(GRID);
        for (int i = 0; i <= Board.SIZE; i++) {
            int x = BOARD_OFFSET_X + i * CELL_SIZE;
            int y = BOARD_OFFSET_Y + i * CELL_SIZE;
            g2.drawLine(x, BOARD_OFFSET_Y, x, BOARD_OFFSET_Y + Board.SIZE * CELL_SIZE);
            g2.drawLine(BOARD_OFFSET_X, y, BOARD_OFFSET_X + Board.SIZE * CELL_SIZE, y);
        }
        g2.setComposite(old);
    }

    private void drawPreview(Graphics2D g2) {
        int idx = engine.getSelectedPieceIndex();
        if (idx < 0 || engine.isDragging()) {
            return;
        }
        BlockPiece[] tray = engine.getTray();
        if (idx >= tray.length || tray[idx] == null) {
            return;
        }
        BlockPiece piece = tray[idx];
        int row = engine.getPreviewRow();
        int col = engine.getPreviewCol();
        boolean canPlace = engine.getBoard().canPlace(piece, row, col);
        Color tint = canPlace ? new Color(0, 255, 0) : new Color(255, 0, 0);

        Shape oldClip = g2.getClip();
        g2.setClip(BOARD_OFFSET_X, BOARD_OFFSET_Y, Board.SIZE * CELL_SIZE, Board.SIZE * CELL_SIZE);
        drawTintedPieceAtGrid(g2, piece, col, row, tint, 120f / 255f);
        g2.setClip(oldClip);
    }

    private void drawDragGhost(Graphics2D g2) {
        if (!engine.isDragging()) {
            return;
        }
        int idx = engine.getSelectedPieceIndex();
        BlockPiece[] tray = engine.getTray();
        if (idx < 0 || idx >= tray.length || tray[idx] == null) {
            return;
        }
        BlockPiece piece = tray[idx];
        int sx = engine.getDragScreenX();
        int sy = engine.getDragScreenY();

        // Snapped preview on the board (green/red tint like the keyboard preview)
        int snapCol = (sx - BOARD_OFFSET_X) / CELL_SIZE;
        int snapRow = (sy - BOARD_OFFSET_Y) / CELL_SIZE;
        boolean canPlace = engine.getBoard().canPlace(piece, snapRow, snapCol);
        Color tint = canPlace ? new Color(0, 255, 0) : new Color(255, 0, 0);

        Shape oldClip = g2.getClip();
        g2.setClip(BOARD_OFFSET_X, BOARD_OFFSET_Y, Board.SIZE * CELL_SIZE, Board.SIZE * CELL_SIZE);
        drawTintedPieceAtGrid(g2, piece, snapCol, snapRow, tint, 120f / 255f);
        g2.setClip(oldClip);

        // Piece image following the cursor for drag feel
        int pw = piece.getCols() * DRAG_GHOST_CELL;
        int ph = piece.getRows() * DRAG_GHOST_CELL;
        float ox = sx - pw / 2f;
        float oy = sy - ph / 2f;
        drawPieceCells(g2, piece, ox, oy, DRAG_GHOST_CELL, 0.75f);
    }

    private void drawFlashAnimation(Graphics2D g2, long nowMs) {
        AnimationState a = engine.getAnimationState();
        if (!a.flashActive) {
            return;
        }
        float progress = (nowMs - a.flashStartMs) / (float) AnimationState.FLASH_DURATION_MS;
        int alpha = (int) (255 * (1f - progress));
        alpha = Math.max(0, Math.min(255, alpha));
        Composite old = g2.getComposite();
        g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha / 255f));
        g2.setColor(Color.WHITE);
        for (Point p : a.flashCells) {
            g2.fillRect(BOARD_OFFSET_X + p.x * CELL_SIZE, BOARD_OFFSET_Y + p.y * CELL_SIZE,
                    CELL_SIZE, CELL_SIZE);
        }
        g2.setComposite(old);
    }

    private void drawPopAnimation(Graphics2D g2, long nowMs) {
        AnimationState a = engine.getAnimationState();
        if (!a.popActive) {
            return;
        }
        float progress = (nowMs - a.popStartMs) / (float) AnimationState.POP_DURATION_MS;
        double scale = 1.0 + 0.25 * Math.sin(Math.PI * progress);
        for (Point p : a.popCells) {
            Color c = engine.getBoard().getCellColor(p.y, p.x);
            if (c == null) {
                continue;
            }
            int x = BOARD_OFFSET_X + p.x * CELL_SIZE;
            int y = BOARD_OFFSET_Y + p.y * CELL_SIZE;
            int cx = x + CELL_SIZE / 2;
            int cy = y + CELL_SIZE / 2;
            AffineTransform t = g2.getTransform();
            g2.translate(cx, cy);
            g2.scale(scale, scale);
            g2.translate(-cx, -cy);
            g2.setColor(c);
            g2.fillRoundRect(x + 2, y + 2, CELL_SIZE - 4, CELL_SIZE - 4, 8, 8);
            g2.setTransform(t);
        }
    }

    private void drawScorePopups(Graphics2D g2, long nowMs) {
        AnimationState a = engine.getAnimationState();
        g2.setFont(scoreValue2Font);
        FontMetrics fm = g2.getFontMetrics();
        for (ScorePopupEntry e : a.scorePopups) {
            float progress = (nowMs - e.startMs) / (float) ScorePopupEntry.POPUP_DURATION_MS;
            if (progress < 0f || progress > 1f) {
                continue;
            }
            int alpha = (int) (255 * (1f - progress));
            alpha = Math.max(0, Math.min(255, alpha));
            float y = e.y - progress * 30f;
            int w = fm.stringWidth(e.text);
            Composite old = g2.getComposite();
            g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha / 255f));
            g2.setColor(Color.YELLOW);
            g2.drawString(e.text, e.x - w / 2f, y);
            g2.setComposite(old);
        }
    }

    private void drawTray(Graphics2D g2) {
        BlockPiece[] tray = engine.getTray();
        int selected = engine.getSelectedPieceIndex();

        for (int i = 0; i < 3; i++) {
            int sx = BOARD_OFFSET_X + i * (TRAY_SLOT_SIZE + TRAY_GAP);
            int sy = TRAY_Y;

            if (imgSlotBg != null) {
                g2.drawImage(imgSlotBg, sx, sy, TRAY_SLOT_SIZE, TRAY_SLOT_SIZE, null);
            } else {
                g2.setColor(CELL_BG);
                g2.fillRoundRect(sx, sy, TRAY_SLOT_SIZE, TRAY_SLOT_SIZE, 10, 10);
            }
            g2.setColor(GRID);
            g2.drawRoundRect(sx, sy, TRAY_SLOT_SIZE, TRAY_SLOT_SIZE, 10, 10);

            if (tray[i] != null) {
                drawPieceInSlot(g2, tray[i], sx, sy, TRAY_SLOT_SIZE, TRAY_SLOT_SIZE);
            }

            if (i == selected && tray[i] != null) {
                g2.setColor(Color.YELLOW);
                g2.setStroke(new BasicStroke(3));
                g2.drawRoundRect(sx - 2, sy - 2, TRAY_SLOT_SIZE + 4, TRAY_SLOT_SIZE + 4, 10, 10);
                g2.setStroke(new BasicStroke(1));
            }

            // Key hint above the slot
            drawCenteredStringInRect(g2, String.valueOf(i + 1),
                    sx, sy - 26, TRAY_SLOT_SIZE, 22, smallFont, LABEL_FG);
        }
    }

    private void drawScorePanel(Graphics2D g2) {
        int x = SCORE_PANEL_X;
        int y = BOARD_OFFSET_Y;
        int w = SCORE_PANEL_W;
        int h = SCORE_PANEL_H;

        g2.setColor(CELL_BG);
        g2.fillRoundRect(x, y, w, h, 12, 12);
        g2.setColor(PANEL_BORDER);
        g2.drawRoundRect(x, y, w, h, 12, 12);

        // Header image ("SCORE" bar) if present
        int imgW = 220;
        int imgH = (int) (220 * 64f / 192f);
        if (imgScoreBg != null) {
            g2.drawImage(imgScoreBg, x + (w - imgW) / 2, y + 10, imgW, imgH, null);
        }

        // "SCORE" label (drawn regardless so the panel is readable if the
        // image is a plain bar)
        int labelY = (imgScoreBg != null) ? y + imgH + 16 : y + 30;
        drawCenteredStringInRect(g2, "SCORE", x, labelY, w, 24, labelFont, LABEL_FG);

        // Current score
        drawCenteredStringInRect(g2, String.valueOf(engine.getCurrentScore()),
                x, labelY + 28, w, 46, scoreValueFont, Color.WHITE);

        // Best score with star icon
        int bestY = labelY + 86;
        int star = 30;
        int iconX = x + 30;
        if (imgBest != null) {
            g2.drawImage(imgBest, iconX, bestY - star, star, star, null);
        }
        g2.setFont(labelFont);
        g2.setColor(LABEL_FG);
        g2.drawString("BEST", iconX + star + 8, bestY - 4);
        g2.setFont(scoreValue2Font);
        g2.setColor(Color.YELLOW);
        g2.drawString(String.valueOf(engine.getBestScore()), iconX + star + 8, bestY + 24);

        // Elapsed time
        long ms = engine.getElapsedMs();
        int totalSec = (int) (ms / 1000);
        String time = String.format("TIME %02d:%02d", totalSec / 60, totalSec % 60);
        drawCenteredStringInRect(g2, time, x, bestY + 44, w, 26, labelFont, LABEL_FG);

        // Time Attack countdown
        if (engine.getGameMode() == GameMode.TIME_ATTACK) {
            int rem = engine.getTimeAttackRemainingSeconds();
            String left = String.format("LEFT %02d:%02d", rem / 60, rem % 60);
            Color col = rem < 30 ? Color.RED : LABEL_FG;
            drawCenteredStringInRect(g2, left, x, bestY + 72, w, 26, labelFont, col);
        }
    }

    private void drawModeBanner(Graphics2D g2) {
        GameMode mode = engine.getGameMode();
        if (mode == null) {
            return;
        }
        String text = mode == GameMode.CLASSIC ? "CLASSIC" : "TIME ATTACK";
        g2.setFont(smallFont);
        g2.setColor(LABEL_FG);
        FontMetrics fm = g2.getFontMetrics();
        g2.drawString(text, BOARD_OFFSET_X, BOARD_OFFSET_Y - 16);
    }

    // =========================================================================
    // Menu screens
    // =========================================================================

    private void drawMainMenu(Graphics2D g2) {
        // BLOCKED title — cyan-to-purple gradient
        GradientPaint gp = new GradientPaint(0, 70, Color.CYAN, 0, 170, new Color(0x8800ff));
        drawCenteredStringInRect(g2, "BLOCKED", 0, 60, WINDOW_WIDTH, 110, titleFont, gp);

        int bw = 220;
        int bh = 72;
        int cx = (WINDOW_WIDTH - bw) / 2;
        playRect     = drawButton(g2, imgMenu,         "PLAY",        cx, 300, bw, bh);
        instructRect = drawButton(g2, imgInstructions, "INSTRUCTIONS", cx, 390, bw, bh);
        exitRect     = drawButton(g2, imgExit,         "EXIT",        cx, 480, bw, bh);
    }

    private void drawModeSelect(Graphics2D g2) {
        drawCenteredStringInRect(g2, "SELECT MODE", 0, 70, WINDOW_WIDTH, 60, headingFont, Color.WHITE);

        int bw = 240;
        int bh = 80;
        int gap = 40;
        int total = bw * 2 + gap;
        int leftX = (WINDOW_WIDTH - total) / 2;
        classicRect    = drawButton(g2, imgClassic,    "CLASSIC",     leftX, 260, bw, bh);
        timeAttackRect = drawButton(g2, imgTimeAttack, "TIME ATTACK", leftX + bw + gap, 260, bw, bh);

        backRect = drawButton(g2, imgBack, "BACK", 50, 620, 130, 44);
    }

    private void drawInstructions(Graphics2D g2) {
        drawCenteredStringInRect(g2, "HOW TO PLAY", 0, 50, WINDOW_WIDTH, 60, headingFont, Color.WHITE);

        String[] rules = {
            "Place block pieces onto the 8x8 grid.",
            "Complete a full row or column to clear it and score.",
            "Each placed cell scores 1 point.",
            "Clearing lines awards a bonus: 10 x lines x lines.",
            "CLASSIC: play until no piece fits.",
            "TIME ATTACK: reach 500 before 180 seconds run out."
        };
        int y = 140;
        for (String s : rules) {
            drawCenteredStringInRect(g2, s, 0, y, WINDOW_WIDTH, 30, labelFont, TEXT_FG);
            y += 34;
        }

        drawCenteredStringInRect(g2, "CONTROLS", 0, y + 10, WINDOW_WIDTH, 34, subHeadingFont, Color.CYAN);
        y += 50;
        String[] controls = {
            "Mouse: drag a tray piece onto the board, release to place.",
            "1 / 2 / 3 : select a tray piece",
            "Arrows : move preview     Enter / Space : place",
            "P : pause     R : restart     M : mute"
        };
        for (String s : controls) {
            drawCenteredStringInRect(g2, s, 0, y, WINDOW_WIDTH, 30, labelFont, TEXT_FG);
            y += 32;
        }

        backRect = drawButton(g2, imgBack, "BACK", 50, 620, 130, 44);
    }

    // =========================================================================
    // Overlays
    // =========================================================================

    private void drawPauseOverlay(Graphics2D g2) {
        g2.setColor(new Color(0, 0, 0, 180));
        g2.fillRect(0, 0, WINDOW_WIDTH, WINDOW_HEIGHT);

        drawCenteredStringInRect(g2, "PAUSED", 0, 130, WINDOW_WIDTH, 80, titleFont, Color.WHITE);

        int bw = 200;
        int bh = 64;
        int cx = (WINDOW_WIDTH - bw) / 2;
        resumeRect  = drawButton(g2, imgResume,  "RESUME",    cx, 300, bw, bh);
        restartRect = drawButton(g2, imgRestart, "RESTART",   cx, 380, bw, bh);
        menuRect    = drawButton(g2, imgMenu,    "MAIN MENU", cx, 460, bw, bh);
    }

    private void drawGameOverOverlay(Graphics2D g2) {
        g2.setColor(new Color(0, 0, 0, 200));
        g2.fillRect(0, 0, WINDOW_WIDTH, WINDOW_HEIGHT);

        // Cross icon
        int icon = 64;
        if (imgGameOver != null) {
            g2.drawImage(imgGameOver, (WINDOW_WIDTH - icon) / 2, 70, icon, icon, null);
        }

        drawCenteredStringInRect(g2, "GAME OVER", 0, 150, WINDOW_WIDTH, 80, titleFont, Color.RED);

        drawCenteredStringInRect(g2, "Score: " + engine.getCurrentScore(),
                0, 240, WINDOW_WIDTH, 36, labelFont, Color.WHITE);
        drawBestLine(g2, 286);

        int bw = 200;
        int bh = 64;
        int cx = (WINDOW_WIDTH - bw) / 2;
        restartRect = drawButton(g2, imgRestart, "RESTART",   cx, 430, bw, bh);
        menuRect    = drawButton(g2, imgMenu,    "MAIN MENU", cx, 510, bw, bh);
    }

    private void drawWinOverlay(Graphics2D g2) {
        g2.setColor(new Color(0, 60, 0, 200));
        g2.fillRect(0, 0, WINDOW_WIDTH, WINDOW_HEIGHT);

        int icon = 64;
        if (imgWin != null) {
            g2.drawImage(imgWin, (WINDOW_WIDTH - icon) / 2, 70, icon, icon, null);
        }

        drawCenteredStringInRect(g2, "YOU WIN!", 0, 150, WINDOW_WIDTH, 80, titleFont, Color.GREEN);

        drawCenteredStringInRect(g2, "Score: " + engine.getCurrentScore(),
                0, 240, WINDOW_WIDTH, 36, labelFont, Color.WHITE);
        drawBestLine(g2, 286);

        int bw = 200;
        int bh = 64;
        int cx = (WINDOW_WIDTH - bw) / 2;
        restartRect = drawButton(g2, imgRestart, "RESTART",   cx, 430, bw, bh);
        menuRect    = drawButton(g2, imgMenu,    "MAIN MENU", cx, 510, bw, bh);
    }

    /** Draws the "Best: N" line with a star icon, centered on the window. */
    private void drawBestLine(Graphics2D g2, int centerY) {
        String best = "Best: " + engine.getBestScore();
        g2.setFont(labelFont);
        FontMetrics fm = g2.getFontMetrics();
        int textW = fm.stringWidth(best);
        int star = 28;
        int totalW = star + 8 + textW;
        int startX = (WINDOW_WIDTH - totalW) / 2;
        if (imgBest != null) {
            g2.drawImage(imgBest, startX, centerY - star + 4, star, star, null);
        }
        g2.setColor(LABEL_FG);
        g2.drawString(best, startX + star + 8, centerY + labelFont.getSize() / 3);
    }

    // =========================================================================
    // Mute indicator (top-right, reflects M-key state)
    // =========================================================================

    private void drawMuteIndicator(Graphics2D g2) {
        int w = 56;
        int h = 38;
        int x = WINDOW_WIDTH - w - 14;
        int y = 12;
        drawButton(g2, imgMute, "MUTE", x, y, w, h);
        if (engine.isMuted()) {
            g2.setColor(Color.RED);
            g2.setStroke(new BasicStroke(3));
            g2.drawLine(x + 6, y + 6, x + w - 6, y + h - 6);
            g2.setStroke(new BasicStroke(1));
        }
    }

    // =========================================================================
    // Click routing
    // =========================================================================

    /**
     * Routes a click to the appropriate engine method based on the stored
     * button rectangles for the current screen. Called by {@link GameFrame}'s
     * mouse adapter after the engine has processed the press.
     */
    public void handleClick(int x, int y, GameEngine engine) {
        GameState state = engine.getGameState();
        switch (state) {
            case MENU:
                if (playRect != null && playRect.contains(x, y)) {
                    engine.setModeSelectState();
                } else if (instructRect != null && instructRect.contains(x, y)) {
                    engine.setInstructionsState();
                } else if (exitRect != null && exitRect.contains(x, y)) {
                    System.exit(0);
                }
                break;
            case MODE_SELECT:
                if (classicRect != null && classicRect.contains(x, y)) {
                    engine.startGame(GameMode.CLASSIC);
                } else if (timeAttackRect != null && timeAttackRect.contains(x, y)) {
                    engine.startGame(GameMode.TIME_ATTACK);
                } else if (backRect != null && backRect.contains(x, y)) {
                    engine.goToMenu();
                }
                break;
            case PAUSED:
                if (resumeRect != null && resumeRect.contains(x, y)) {
                    engine.resume();
                } else if (restartRect != null && restartRect.contains(x, y)) {
                    engine.restart();
                } else if (menuRect != null && menuRect.contains(x, y)) {
                    engine.goToMenu();
                }
                break;
            case GAME_OVER:
            case WIN:
                if (restartRect != null && restartRect.contains(x, y)) {
                    engine.restart();
                } else if (menuRect != null && menuRect.contains(x, y)) {
                    engine.goToMenu();
                }
                break;
            case INSTRUCTIONS:
                if (backRect != null && backRect.contains(x, y)) {
                    engine.goToMenu();
                }
                break;
            default:
                break;
        }
    }

    // =========================================================================
    // Drawing helpers
    // =========================================================================

    /**
     * Draws a button: the supplied image scaled to fit (aspect preserved) and
     * centred in the rectangle, or a dark rounded rectangle with a centred
     * text label when the image is missing. Returns the hit rectangle.
     */
    private Rectangle drawButton(Graphics2D g2, BufferedImage img, String label,
                                  int x, int y, int w, int h) {
        if (img != null) {
            int iw = img.getWidth();
            int ih = img.getHeight();
            double scale = Math.min((double) w / iw, (double) h / ih);
            int dw = (int) (iw * scale);
            int dh = (int) (ih * scale);
            int dx = x + (w - dw) / 2;
            int dy = y + (h - dh) / 2;
            g2.drawImage(img, dx, dy, dw, dh, null);
        } else {
            g2.setColor(new Color(0x16213e));
            g2.fillRoundRect(x, y, w, h, 12, 12);
            g2.setColor(PANEL_BORDER);
            g2.drawRoundRect(x, y, w, h, 12, 12);
            drawCenteredStringInRect(g2, label, x, y, w, h, labelFont, Color.WHITE);
        }
        return new Rectangle(x, y, w, h);
    }

    /**
     * Draws a piece as coloured cells (block PNG if available, else a rounded
     * colour square) anchored at the given screen-space origin.
     */
    private void drawPieceCells(Graphics2D g2, BlockPiece piece, float ox, float oy,
                                 int cellPx, float alpha) {
        Composite old = g2.getComposite();
        if (alpha < 1f) {
            g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha));
        }
        BufferedImage img = blockImages.get(piece.getColorName());
        for (int[] cell : piece.getCells()) {
            int cx = (int) (ox + cell[1] * cellPx);
            int cy = (int) (oy + cell[0] * cellPx);
            if (img != null) {
                g2.drawImage(img, cx, cy, cellPx, cellPx, null);
            } else {
                g2.setColor(piece.getColor());
                g2.fillRoundRect(cx + 1, cy + 1, cellPx - 2, cellPx - 2, 6, 6);
            }
        }
        g2.setComposite(old);
    }

    /** Draws a piece centred inside a tray slot. */
    private void drawPieceInSlot(Graphics2D g2, BlockPiece piece, int slotX, int slotY,
                                  int slotW, int slotH) {
        int rows = piece.getRows();
        int cols = piece.getCols();
        int maxDim = Math.max(1, Math.max(rows, cols));
        int cellPx = Math.max(8, (Math.min(slotW, slotH) - 16) / maxDim);
        int pw = cols * cellPx;
        int ph = rows * cellPx;
        float ox = slotX + (slotW - pw) / 2f;
        float oy = slotY + (slotH - ph) / 2f;
        drawPieceCells(g2, piece, ox, oy, cellPx, 1f);
    }

    /** Draws a piece as semi-transparent green/red cells on the board grid. */
    private void drawTintedPieceAtGrid(Graphics2D g2, BlockPiece piece, int gridCol,
                                         int gridRow, Color tint, float alpha) {
        Composite old = g2.getComposite();
        g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha));
        g2.setColor(tint);
        for (int[] cell : piece.getCells()) {
            int c = gridCol + cell[1];
            int r = gridRow + cell[0];
            int x = BOARD_OFFSET_X + c * CELL_SIZE;
            int y = BOARD_OFFSET_Y + r * CELL_SIZE;
            g2.fillRoundRect(x + 2, y + 2, CELL_SIZE - 4, CELL_SIZE - 4, 8, 8);
        }
        g2.setComposite(old);
    }

    /** Horizontally and vertically centres a string inside a rectangle. */
    private void drawCenteredStringInRect(Graphics2D g2, String text, int x, int y,
                                            int w, int h, Font font, Paint paint) {
        g2.setFont(font);
        FontMetrics fm = g2.getFontMetrics();
        int tw = fm.stringWidth(text);
        int tx = x + (w - tw) / 2;
        int ty = y + (h - fm.getHeight()) / 2 + fm.getAscent();
        g2.setPaint(paint);
        g2.drawString(text, tx, ty);
    }
}
