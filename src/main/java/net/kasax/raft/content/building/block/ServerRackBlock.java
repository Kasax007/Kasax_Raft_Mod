package net.kasax.raft.content.building.block;

import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.VoxelShape;

/** The Expedition's computer rack: blinking lights (animated texture) and a soft electric hum now and then. */
public class ServerRackBlock extends ShapedFacingBlock {
    public ServerRackBlock(Properties properties, VoxelShape northShape) {
        super(properties, northShape);
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (random.nextInt(120) == 0) {
            level.playLocalSound(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, SoundEvents.BEACON_AMBIENT,
                    SoundSource.BLOCKS, 0.12F, 1.7F + random.nextFloat() * 0.2F, false);
        }
    }
}
