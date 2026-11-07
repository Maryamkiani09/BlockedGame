package com.blocked.model;

/**
 * Finite states of the game's screen-switching state machine. The engine
 * owns the current value; the UI reads it to decide which screen to draw.
 */
public enum GameState {
    MENU,
    MODE_SELECT,
    PLAYING,
    PAUSED,
    GAME_OVER,
    WIN,
    INSTRUCTIONS
}
