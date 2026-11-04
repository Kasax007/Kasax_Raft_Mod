package net.kasax.raft.content.vessel.client;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.core.BlockPos;
import org.jspecify.annotations.Nullable;

/** Render state of a sailing raft: its draw cache, yaw and the block under the crosshair (for the outline). */
@Environment(EnvType.CLIENT)
public class VesselRenderState extends EntityRenderState {
    @Nullable VesselClientModel model;
    float yaw;
    @Nullable BlockPos target;
}
