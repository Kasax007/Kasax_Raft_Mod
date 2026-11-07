package net.kasax.raft.compat.render;

import net.minecraft.core.Direction;
import net.minecraft.world.entity.Pose;
import org.jetbrains.annotations.Nullable;

/** 1.21.1 port helper: render data of a living entity (see {@link EntityRenderState}). */
public class LivingEntityRenderState extends EntityRenderState {
    public float bodyRot;
    public float yRot;
    public float xRot;
    public float deathTime;
    public float walkAnimationPos;
    public float walkAnimationSpeed;
    public float scale = 1.0F;
    public float ageScale = 1.0F;
    public boolean isUpsideDown;
    public boolean isFullyFrozen;
    public boolean isBaby;
    public boolean isInWater;
    public boolean isAutoSpinAttack;
    public boolean hasRedOverlay;
    public boolean isInvisibleToPlayer;
    public @Nullable Direction bedOrientation;
    public Pose pose = Pose.STANDING;
}
