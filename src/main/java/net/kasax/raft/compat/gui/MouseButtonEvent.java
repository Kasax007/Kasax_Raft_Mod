package net.kasax.raft.compat.gui;

/** 1.21.1 port helper: a mouse click as in the newer game; buttons are 1.21.1's (GLFW) numbers, see InputConstants. */
public record MouseButtonEvent(double x, double y, int button) {
}
