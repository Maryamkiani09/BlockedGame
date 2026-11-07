package com.blocked.model;

import java.awt.Point;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Mutable holder for the transient animation state driven by the engine and
 * read by the renderer. Contains no game logic and no Swing types — it is a
 * plain bag of fields the engine updates and {@code paintComponent} reads.
 */
public class AnimationState {

    /** Duration of the line-clear flash overlay, in milliseconds. */
    public static final int FLASH_DURATION_MS = 180;

    /** Duration of the placement pop animation, in milliseconds. */
    public static final int POP_DURATION_MS = 130;

    /** Line-clear flash: a white overlay fades over the cleared cells. */
    public boolean flashActive;
    public Set<Point> flashCells;
    public long flashStartMs;

    /** Placement pop: cells briefly scale up when a piece is stamped. */
    public boolean popActive;
    public Set<Point> popCells;
    public long popStartMs;

    /** Floating "+N" score indicators. */
    public List<ScorePopupEntry> scorePopups = new ArrayList<>();

    public AnimationState() {
        this.flashCells = new HashSet<>();
        this.popCells = new HashSet<>();
    }

    /**
     * A single floating score indicator ("+N") rendered in screen space.
     */
    public static class ScorePopupEntry {

        /** Duration the popup is visible before fading out, in milliseconds. */
        public static final int POPUP_DURATION_MS = 600;

        public String text;
        public float x;
        public float y;
        public long startMs;
    }
}