package net.kasax.raft.compat.render;

import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.EntityType;
import org.jetbrains.annotations.Nullable;

/**
 * 1.21.1 port helper: the data a renderer reads from an entity for one frame, as in the newer game. The compat
 * renderers fill it before they draw, so models and renderers keep their newer structure.
 */
public class EntityRenderState {
    public static final int NO_OUTLINE = 0;
    public EntityType<?> entityType;
    public double x;
    public double y;
    public double z;
    public float ageInTicks;
    public float partialTick;
    public float boundingBoxWidth;
    public float boundingBoxHeight;
    public float eyeHeight;
    public double distanceToCameraSq;
    public boolean isInvisible;
    public boolean isDiscrete;
    public boolean displayFireAnimation;
    public int lightCoords = 15728880;
    public int outlineColor = NO_OUTLINE;
    public @Nullable Component nameTag;
    public float shadowRadius;
}
