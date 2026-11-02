package net.kasax.raft.content.marine.entity;

import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.control.LookControl;

/**
 * Look control for the gliding swimmers: the head stays in line with the body and the pitch is left to
 * {@link GlideMoveControl} (the vanilla controls would pull it back to level every tick, which stops diving).
 */
public class GlideLookControl extends LookControl {
    public GlideLookControl(Mob mob) {
        super(mob);
    }

    @Override
    protected boolean resetXRotOnTick() {
        return false;
    }

    @Override
    public void tick() {
        this.mob.yHeadRot = this.rotateTowards(this.mob.yHeadRot, this.mob.yBodyRot, 10.0F);
    }
}
