package com.blocked.model;

/**
 * The two play modes offered on the mode-select screen.
 * <ul>
 *   <li>{@code CLASSIC} — endless placement until no tray piece fits.</li>
 *   <li>{@code TIME_ATTACK} — reach the target score before the timer runs out.</li>
 * </ul>
 */
public enum GameMode {
    CLASSIC,
    TIME_ATTACK;

    /** Duration of a Time Attack run, in seconds. */
    public static final int TIME_ATTACK_SECONDS = 180;

    /** Score a player must reach to win a Time Attack run. */
    public static final int TIME_ATTACK_TARGET = 500;
}
