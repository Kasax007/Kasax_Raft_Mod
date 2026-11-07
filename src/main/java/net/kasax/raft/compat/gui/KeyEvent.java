package net.kasax.raft.compat.gui;

import com.mojang.blaze3d.platform.InputConstants;

/** 1.21.1 port helper: a key press as in the newer game, with 1.21.1's (GLFW) key codes from {@link InputConstants}. */
public record KeyEvent(int key, int scancode, int modifiers) {
    public int input() {
        return this.key;
    }

    public boolean isSelection() {
        return this.key == InputConstants.KEY_RETURN || this.key == InputConstants.KEY_SPACE || this.key == InputConstants.KEY_NUMPADENTER;
    }

    public boolean isConfirmation() {
        return this.key == InputConstants.KEY_RETURN || this.key == InputConstants.KEY_NUMPADENTER;
    }

    public boolean isEscape() {
        return this.key == InputConstants.KEY_ESCAPE;
    }

    public boolean isLeft() {
        return this.key == InputConstants.KEY_LEFT;
    }

    public boolean isRight() {
        return this.key == InputConstants.KEY_RIGHT;
    }

    public boolean isUp() {
        return this.key == InputConstants.KEY_UP;
    }

    public boolean isDown() {
        return this.key == InputConstants.KEY_DOWN;
    }

    public boolean hasShiftDown() {
        return (this.modifiers & 1) != 0;
    }
}
