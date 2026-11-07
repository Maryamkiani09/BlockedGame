package com.blocked.data;

import javax.imageio.ImageIO;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import java.awt.Font;
import java.awt.image.BufferedImage;
import java.io.InputStream;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Static utility for loading classpath resources (images, sounds, fonts).
 * <p>Every method returns {@code null} — and logs the failure at WARNING —
 * instead of throwing, so the UI layer can fall back to programmatic drawing
 * when an asset is missing or unreadable.
 */
public final class ResourceLoader {

    private static final Logger LOGGER = Logger.getLogger(ResourceLoader.class.getName());

    private ResourceLoader() {
        // utility class — no instances
    }

    /**
     * Loads an image from the classpath (e.g. {@code "/images/blocks/blue.png"}).
     *
     * @return the image, or {@code null} if the resource is missing or unreadable
     */
    public static BufferedImage loadImage(String classpathPath) {
        try (InputStream stream = ResourceLoader.class.getResourceAsStream(classpathPath)) {
            if (stream == null) {
                LOGGER.log(Level.WARNING, "Image resource not found: {0}", classpathPath);
                return null;
            }
            return ImageIO.read(stream);
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Failed to load image: " + classpathPath, e);
            return null;
        }
    }

    /**
     * Loads a sound clip from the classpath (e.g. {@code "/sounds/place.wav"}).
     *
     * @return an open, ready-to-play clip, or {@code null} if the resource is
     *         missing or unreadable
     */
    public static Clip loadSound(String classpathPath) {
        try (InputStream stream = ResourceLoader.class.getResourceAsStream(classpathPath)) {
            if (stream == null) {
                LOGGER.log(Level.WARNING, "Sound resource not found: {0}", classpathPath);
                return null;
            }
            try (AudioInputStream audioStream = AudioSystem.getAudioInputStream(stream)) {
                Clip clip = AudioSystem.getClip();
                clip.open(audioStream);
                return clip;
            }
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Failed to load sound: " + classpathPath, e);
            return null;
        }
    }

    /**
     * Loads a TrueType font from the classpath and derives it at the given
     * point size (e.g. {@code "/fonts/Kenney Future Narrow.ttf"}).
     *
     * @return the derived font, or {@code null} if the resource is missing or
     *         unreadable
     */
    public static Font loadFont(String classpathPath, float size) {
        try (InputStream stream = ResourceLoader.class.getResourceAsStream(classpathPath)) {
            if (stream == null) {
                LOGGER.log(Level.WARNING, "Font resource not found: {0}", classpathPath);
                return null;
            }
            return Font.createFont(Font.TRUETYPE_FONT, stream).deriveFont(size);
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Failed to load font: " + classpathPath, e);
            return null;
        }
    }
}
