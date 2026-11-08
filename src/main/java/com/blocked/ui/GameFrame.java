package com.blocked.ui;

import com.blocked.data.ScoreRepository;
import com.blocked.engine.GameEngine;
import com.blocked.engine.SoundManager;
import com.blocked.model.AnimationState;
import com.blocked.model.Board;
import com.blocked.model.PieceGenerator;
import com.blocked.model.ScoreManager;

import javax.swing.JFrame;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.logging.FileHandler;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

/**
 * Top-level JFrame: the application entry point, the Swing Timer that drives
 * the game loop, and the wiring of keyboard / mouse events into the engine.
 * <p>
 * The frame owns no game state. It constructs the layered collaborators
 * (data -> model -> engine -> UI) and forwards every input event to the
 * engine, which remains the single source of truth.
 */
public class GameFrame extends JFrame {

    private static final long serialVersionUID = 1L;

    public static final int WINDOW_WIDTH  = 900;
    public static final int WINDOW_HEIGHT = 700;

    /** Swing Timer tick interval — ~60 Hz (16ms). */
    private static final int TICK_MS = 16;

    static {
        configureLogging();
    }

    private final GameEngine engine;
    private final GamePanel  panel;
    private final Timer       timer;

    public GameFrame() {
        super("BLOCKED");

        // --- Layered collaborator construction -------------------------------
        ScoreRepository repo   = new ScoreRepository();
        Board board             = new Board();
        ScoreManager scoreMgr   = new ScoreManager(repo);
        // PieceGenerator is a static-method utility (private constructor); the
        // engine accepts it only for API symmetry and uses the static methods
        // directly, so null is the correct value to inject.
        PieceGenerator pieces   = null;
        SoundManager sound      = new SoundManager();
        AnimationState anim     = new AnimationState();
        engine = new GameEngine(board, scoreMgr, pieces, repo, sound, anim);

        // --- UI -------------------------------------------------------------
        panel = new GamePanel(engine);
        setContentPane(panel);

        // --- Frame configuration --------------------------------------------
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setResizable(false);
        pack();                       // sizes the window to the panel's preferred size
        setLocationRelativeTo(null);  // centre on screen

        wireKeyEvents();
        wireMouseEvents();

        // --- Game loop: tick the engine, then repaint -----------------------
        timer = new Timer(TICK_MS, e -> {
            engine.tick(System.currentTimeMillis());
            panel.repaint();
        });
        timer.start();

        setVisible(true);

        // Keyboard focus must land on the panel so key events are delivered.
        panel.requestFocusInWindow();
    }

    // =========================================================================
    // Logging (best-effort — failures are swallowed)
    // =========================================================================

    private static void configureLogging() {
        try {
            Path logDir = Paths.get("logs");
            if (!Files.exists(logDir)) {
                Files.createDirectories(logDir);
            }
            FileHandler fileHandler = new FileHandler("logs/game.log", true);
            fileHandler.setFormatter(new SimpleFormatter());
            Logger.getLogger("").addHandler(fileHandler);
        } catch (Exception e) {
            // Logging setup is best-effort; never block the game from starting.
            System.err.println("Could not configure file logging: " + e.getMessage());
        }
    }

    // =========================================================================
    // Event wiring
    // =========================================================================

    private void wireKeyEvents() {
        KeyAdapter keys = new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                engine.handleKeyPress(e.getKeyCode());
            }
        };
        // Added to both so events are captured whether focus sits on the
        // frame or the content pane; only the focus owner actually fires.
        addKeyListener(keys);
        panel.addKeyListener(keys);
        setFocusable(true);
        panel.setFocusable(true);
    }

    private void wireMouseEvents() {
        panel.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                int x = e.getX();
                int y = e.getY();
                engine.handleMousePressed(x, y,
                        GamePanel.BOARD_OFFSET_X, GamePanel.BOARD_OFFSET_Y, GamePanel.CELL_SIZE);
                // Overlay / menu buttons are hit-tested here (no-op in PLAYING).
                panel.handleClick(x, y, engine);
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                engine.handleMouseReleased(e.getX(), e.getY(),
                        GamePanel.BOARD_OFFSET_X, GamePanel.BOARD_OFFSET_Y, GamePanel.CELL_SIZE);
            }
        });

        panel.addMouseMotionListener(new MouseMotionAdapter() {
            @Override
            public void mouseDragged(MouseEvent e) {
                engine.handleMouseDragged(e.getX(), e.getY());
            }
        });
    }

    // =========================================================================
    // Entry point
    // =========================================================================

    public static void main(String[] args) {
        SwingUtilities.invokeLater(GameFrame::new);
    }
}
