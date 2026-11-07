package net.kasax.raft.content.vessel.client;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.kasax.raft.compat.render.EntityRenderState;
import net.minecraft.core.BlockPos;
import org.jetbrains.annotations.Nullable;

/** Render state of a sailing raft: its draw cache, yaw and the block under the crosshair (for the outline). */
@Environment(EnvType.CLIENT)
public class VesselRenderState extends EntityRenderState {
    @Nullable VesselClientModel model;
    float yaw;
    /** How far the helm's wheel is turned, -1..1. */
    float wheel;
    @Nullable BlockPos target;
}
