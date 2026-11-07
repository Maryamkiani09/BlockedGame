package com.blocked.engine;

import com.blocked.data.ResourceLoader;

import javax.sound.sampled.Clip;
import java.util.logging.Logger;

/**
 * Owns the four sound clips used by the game and exposes simple play methods.
 * Missing clips (null) are silently ignored. All sound output is suppressed
 * when {@link #muted} is {@code true}.
 * <p>No Swing imports.</p>
 */
public class SoundManager {

    private static final Logger LOGGER = Logger.getLogger(SoundManager.class.getName());

    private final Clip selectClip;
    private final Clip placeClip;
    private final Clip clearClip;
    private final Clip invalidClip;

    private boolean muted = false;

    /**
     * Loads all four clips from the classpath. Missing or unreadable resources
     * result in a {@code null} clip that is silently skipped when played.
     */
    public SoundManager() {
        selectClip  = ResourceLoader.loadSound("/sounds/select.wav");
        placeClip   = ResourceLoader.loadSound("/sounds/place.wav");
        clearClip   = ResourceLoader.loadSound("/sounds/clear.wav");
        invalidClip = ResourceLoader.loadSound("/sounds/invalid.wav");
        LOGGER.info("SoundManager initialised — select=" + (selectClip  != null)
                + " place=" + (placeClip   != null)
                + " clear=" + (clearClip   != null)
                + " invalid=" + (invalidClip != null));
    }

    // -------------------------------------------------------------------------
    // Public play methods
    // -------------------------------------------------------------------------

    /** Plays the piece-selection sound. */
    public void playSelect() {
        play(selectClip);
    }

    /** Plays the successful placement sound. */
    public void playPlace() {
        play(placeClip);
    }

    /** Plays the line-clear sound. */
    public void playClear() {
        play(clearClip);
    }

    /** Plays the invalid-placement sound. */
    public void playInvalid() {
        play(invalidClip);
    }

    // -------------------------------------------------------------------------
    // Mute control
    // -------------------------------------------------------------------------

    /** Flips the mute state. */
    public void toggleMute() {
        muted = !muted;
    }

    /** @return {@code true} when all sounds are suppressed */
    public boolean isMuted() {
        return muted;
    }

    // -------------------------------------------------------------------------
    // Internal helper
    // -------------------------------------------------------------------------

    /**
     * Rewinds and starts the clip. Does nothing if the clip is {@code null}
     * or the manager is muted.
     */
    private void play(Clip c) {
        if (c == null || muted) {
            return;
        }
        c.stop();
        c.setFramePosition(0);
        c.start();
    }
}
