package net.kasax.raft.content.vessel.client;

import net.kasax.raft.content.vessel.block.HelmBlock;

import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.kasax.raft.content.vessel.VesselPose;
import net.kasax.raft.content.vessel.VesselStructure;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
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
    final VesselStructure structure;
    final Object modelSet;
    final int[] x;
    final int[] y;
    final int[] z;
    final BlockState[] states;
    /** Block light from the raft's own lanterns, lamps and torches (the world has no light where the raft is). */
    final int[] ownLight;
    final int[] light;
    /** The helm, drawn as its base (states[helmIndex]) and its wheel (wheel) that turns with the steering, or -1. */
    final int helmIndex;
    final BlockState wheel;
    private long lightTick = Long.MIN_VALUE;

    private VesselClientModel(VesselStructure structure, Object modelSet, List<int[]> positions, List<BlockState> states,
                              int helmIndex, BlockState wheel) {
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
        this.states = states.toArray(new BlockState[0]);
        this.ownLight = ownLight(structure, this.x, this.y, this.z);
        this.light = new int[n];
        this.helmIndex = helmIndex;
        this.wheel = wheel;
    }

    static Object currentModelSet() {
        return Minecraft.getInstance().getModelManager();
    }

    boolean isCurrent(VesselStructure structure) {
        return this.structure == structure && this.modelSet == currentModelSet();
    }

    static VesselClientModel build(VesselStructure structure) {
        List<int[]> positions = new ArrayList<>();
        List<BlockState> states = new ArrayList<>();
        int helmIndex = -1;
        BlockState wheel = null;
        for (Long2ObjectMap.Entry<BlockState> e : structure.blocks().long2ObjectEntrySet()) {
            long key = e.getLongKey();
            int bx = BlockPos.getX(key);
            int by = BlockPos.getY(key);
            int bz = BlockPos.getZ(key);
            if (enclosed(structure, bx, by, bz)) {
                continue;
            }
            if (e.getValue().getRenderShape() == net.minecraft.world.level.block.RenderShape.INVISIBLE) {
                continue;
            }
            BlockState shown = e.getValue();
            boolean helm = key == BlockPos.asLong(structure.helm().getX(), structure.helm().getY(), structure.helm().getZ())
                    && shown.getBlock() instanceof HelmBlock;
            if (helm) {
                // the helm without its wheel; the wheel is drawn by itself and turns with the steering
                wheel = shown.setValue(HelmBlock.PART, HelmBlock.Part.WHEEL);
                shown = shown.setValue(HelmBlock.PART, HelmBlock.Part.BASE);
                helmIndex = positions.size();
            }
            positions.add(new int[]{bx, by, bz});
            states.add(shown);
        }
        return new VesselClientModel(structure, currentModelSet(), positions, states, helmIndex, wheel);
    }

    /** Light of the raft's own light sources at each drawn block: falls off by one per block (Manhattan distance). */
    private static int[] ownLight(VesselStructure structure, int[] x, int[] y, int[] z) {
        int[] light = new int[x.length];
        List<int[]> sources = new ArrayList<>();
        for (Long2ObjectMap.Entry<BlockState> e : structure.blocks().long2ObjectEntrySet()) {
            int emission = e.getValue().getLightEmission();
            if (emission > 0) {
                long key = e.getLongKey();
                sources.add(new int[]{BlockPos.getX(key), BlockPos.getY(key), BlockPos.getZ(key), emission});
            }
        }
        if (sources.isEmpty()) {
            return light;
        }
        for (int i = 0; i < x.length; i++) {
            int best = 0;
            for (int[] s : sources) {
                best = Math.max(best, s[3] - Math.abs(s[0] - x[i]) - Math.abs(s[1] - y[i]) - Math.abs(s[2] - z[i]));
            }
            light[i] = best;
        }
        return light;
    }

    private static boolean enclosed(VesselStructure structure, int x, int y, int z) {
        for (Direction d : Direction.values()) {
            BlockState n = structure.get(BlockPos.asLong(x + d.getStepX(), y + d.getStepY(), z + d.getStepZ()));
            if (n == null || !n.isSolidRender(net.minecraft.world.level.EmptyBlockGetter.INSTANCE, BlockPos.ZERO)) {
                return false;
            }
        }
        return true;
    }

    /**
     * The world light at every block (block and sky light at its centre), refreshed once per game tick, and at least
     * the light of the raft's own lanterns, lamps and torches.
     */
    void updateLight(Level level, VesselPose pose, long gameTime) {
        if (gameTime == this.lightTick) {
            return;
        }
        this.lightTick = gameTime;
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        for (int i = 0; i < this.states.length; i++) {
            Vec3 c = pose.toWorld(this.x[i] + 0.5, this.y[i] + 0.5, this.z[i] + 0.5);
            cursor.set(c.x, c.y, c.z);
            int block = Math.max(level.getBrightness(LightLayer.BLOCK, cursor), this.ownLight[i]);
            this.light[i] = net.minecraft.client.renderer.LightTexture.pack(block, level.getBrightness(LightLayer.SKY, cursor));
        }
    }
}
