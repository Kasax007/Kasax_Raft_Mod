package net.kasax.raft.content.vessel.client;

import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.kasax.raft.content.vessel.VesselPose;
import net.kasax.raft.content.vessel.VesselStructure;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.block.BlockModelRenderState;
import net.minecraft.client.renderer.block.BlockModelResolver;
import net.minecraft.client.renderer.block.model.BlockDisplayContext;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

/**
 * What the renderer needs to draw a raft, built once per raft (and again after a resource reload): the block model of
 * every visible block (blocks enclosed by full opaque blocks on all sides are skipped) and, once per tick, the world
 * light at each block.
 */
@Environment(EnvType.CLIENT)
final class VesselClientModel {
    private static final BlockDisplayContext CONTEXT = BlockDisplayContext.create();

    final VesselStructure structure;
    final Object modelSet;
    final int[] x;
    final int[] y;
    final int[] z;
    final BlockModelRenderState[] models;
    final BlockState[] states;
    final int[] light;
    private long lightTick = Long.MIN_VALUE;

    private VesselClientModel(VesselStructure structure, Object modelSet, List<int[]> positions, List<BlockModelRenderState> models,
                              List<BlockState> states) {
        this.structure = structure;
        this.modelSet = modelSet;
        int n = positions.size();
        this.x = new int[n];
        this.y = new int[n];
        this.z = new int[n];
        for (int i = 0; i < n; i++) {
            this.x[i] = positions.get(i)[0];
            this.y[i] = positions.get(i)[1];
            this.z[i] = positions.get(i)[2];
        }
        this.models = models.toArray(new BlockModelRenderState[0]);
        this.states = states.toArray(new BlockState[0]);
        this.light = new int[n];
    }

    static Object currentModelSet() {
        return Minecraft.getInstance().getModelManager().getBlockModelSet();
    }

    boolean isCurrent(VesselStructure structure) {
        return this.structure == structure && this.modelSet == currentModelSet();
    }

    static VesselClientModel build(VesselStructure structure, BlockModelResolver resolver) {
        List<int[]> positions = new ArrayList<>();
        List<BlockModelRenderState> models = new ArrayList<>();
        List<BlockState> states = new ArrayList<>();
        for (Long2ObjectMap.Entry<BlockState> e : structure.blocks().long2ObjectEntrySet()) {
            long key = e.getLongKey();
            int bx = BlockPos.getX(key);
            int by = BlockPos.getY(key);
            int bz = BlockPos.getZ(key);
            if (enclosed(structure, bx, by, bz)) {
                continue;
            }
            BlockModelRenderState model = new BlockModelRenderState();
            resolver.update(model, e.getValue(), CONTEXT);
            if (model.isEmpty()) {
                continue;
            }
            positions.add(new int[]{bx, by, bz});
            models.add(model);
            states.add(e.getValue());
        }
        return new VesselClientModel(structure, currentModelSet(), positions, models, states);
    }

    private static boolean enclosed(VesselStructure structure, int x, int y, int z) {
        for (Direction d : Direction.values()) {
            BlockState n = structure.get(BlockPos.asLong(x + d.getStepX(), y + d.getStepY(), z + d.getStepZ()));
            if (n == null || !n.isSolidRender()) {
                return false;
            }
        }
        return true;
    }

    /**
     * The world light at every block (block and sky light at its centre), refreshed once per game tick. Blocks that give
     * light (lanterns, lit lamps, a burning furnace) are at least as bright as their own light: the world has no light
     * source where the raft is.
     */
    void updateLight(Level level, VesselPose pose, long gameTime) {
        if (gameTime == this.lightTick) {
            return;
        }
        this.lightTick = gameTime;
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        for (int i = 0; i < this.models.length; i++) {
            Vec3 c = pose.toWorld(this.x[i] + 0.5, this.y[i] + 0.5, this.z[i] + 0.5);
            cursor.set(c.x, c.y, c.z);
            int block = Math.max(level.getBrightness(LightLayer.BLOCK, cursor), this.states[i].getLightEmission());
            this.light[i] = LightCoordsUtil.pack(block, level.getBrightness(LightLayer.SKY, cursor));
        }
    }
}
