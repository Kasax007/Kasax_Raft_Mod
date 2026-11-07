package net.kasax.raft.compat.render;

import net.minecraft.world.entity.AnimationState;

/**
 * 1.21.1 port helper: the newer game copies an entity's animation state into its render state. Here the render state
 * keeps a reference to the entity's state and reads the time from it like 1.21.1's own models do.
 */
public final class AnimState {
    private AnimationState source;

    public void copyFrom(AnimationState state) {
        this.source = state;
    }

    public boolean isStarted() {
        return this.source != null && this.source.isStarted();
    }

    /** Milliseconds since the animation started, at this render time. */
    public long getTimeInMillis(float ageInTicks) {
        if (this.source == null) return 0L;
        this.source.updateTime(ageInTicks, 1.0F);
        return this.source.getAccumulatedTime();
    }
}
