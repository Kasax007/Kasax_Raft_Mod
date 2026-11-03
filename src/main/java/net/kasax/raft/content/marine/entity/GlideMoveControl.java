package net.kasax.raft.content.marine.entity;

import net.minecraft.util.Mth;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.control.MoveControl;

/**
 * Steering for big gliding swimmers (whale, manta ray). Unlike the vanilla swim control it does not need a path:
 * goals set a wanted position and the creature turns towards it with a limited turn rate, pitching smoothly up or
 * down, which gives wide, natural curves instead of the sharp corners of a path.
 */
public class GlideMoveControl extends MoveControl<Mob> {
    private final float maxYawPerTick;
    private final float maxPitch;
    private final float pitchPerTick;

    public GlideMoveControl(Mob mob, float maxYawPerTick, float maxPitch, float pitchPerTick) {
        super(mob);
        this.maxYawPerTick = maxYawPerTick;
        this.maxPitch = maxPitch;
        this.pitchPerTick = pitchPerTick;
    }

    @Override
    public void tick() {
        if (this.operation != Operation.MOVE_TO) {
            this.mob.setSpeed(0.0F);
            this.mob.setZza(0.0F);
            this.mob.setYya(0.0F);
            this.mob.setXRot(this.rotateTowards(this.mob.getXRot(), 0.0F, this.pitchPerTick));
            return;
        }
        double dx = this.wantedX - this.mob.getX();
        double dy = this.wantedY - this.mob.getY();
        double dz = this.wantedZ - this.mob.getZ();
        double horizontal = Math.sqrt(dx * dx + dz * dz);
        if (horizontal * horizontal + dy * dy < 0.5) {
            this.operation = Operation.WAIT;
            this.mob.setZza(0.0F);
            this.mob.setYya(0.0F);
            return;
        }
        // a target (almost) straight above or below: keep the heading instead of spinning around it
        float targetYaw = horizontal < 1.0 ? this.mob.getYRot() : (float) (Mth.atan2(dz, dx) * Mth.RAD_TO_DEG) - 90.0F;
        this.mob.setYRot(this.rotlerp(this.mob.getYRot(), targetYaw, this.maxYawPerTick));
        this.mob.yBodyRot = this.mob.getYRot();
        this.mob.yHeadRot = this.mob.getYRot();
        float speed = (float) (this.speedModifier * this.mob.getAttributeValue(Attributes.MOVEMENT_SPEED));
        if (this.mob.isInWater()) {
            float targetPitch = Mth.clamp(-(float) (Mth.atan2(dy, Math.max(horizontal, 0.5)) * Mth.RAD_TO_DEG), -this.maxPitch, this.maxPitch);
            this.mob.setXRot(this.rotateTowards(this.mob.getXRot(), targetPitch, this.pitchPerTick));
            // slow down in sharp turns
            float turn = Math.abs(Mth.wrapDegrees(targetYaw - this.mob.getYRot()));
            float turnFactor = 1.0F - Mth.clamp((turn - 20.0F) / 140.0F, 0.0F, 0.6F);
            float cos = Mth.cos(this.mob.getXRot() * Mth.DEG_TO_RAD);
            float sin = Mth.sin(this.mob.getXRot() * Mth.DEG_TO_RAD);
            // the input vector is a unit direction (scaled down in turns); moveRelative multiplies it by the speed,
            // so the creature accelerates by `speed` per tick and glides at about ten times that (water drag 0.9)
            this.mob.setSpeed(speed);
            this.mob.zza = cos * turnFactor;
            this.mob.yya = -sin * turnFactor;
        } else {
            this.mob.setSpeed(speed * 0.05F);
            this.mob.zza = 0.0F;
            this.mob.yya = 0.0F;
        }
    }
}
