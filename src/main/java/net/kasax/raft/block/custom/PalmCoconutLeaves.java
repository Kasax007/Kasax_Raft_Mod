package net.kasax.raft.block.custom;

import net.kasax.raft.block.ModBlocks;
import net.kasax.raft.item.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;

/** Palm leaves carrying a coconut. Right-click to pick the coconut; the leaves regrow one after a while. */
public class PalmCoconutLeaves extends LeavesBlock {
    public static final BooleanProperty HAS_COCONUT = BooleanProperty.create("has_coconut");

    public PalmCoconutLeaves(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any()
                .setValue(DISTANCE, 7).setValue(PERSISTENT, false).setValue(WATERLOGGED, false)
                .setValue(HAS_COCONUT, true));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(HAS_COCONUT);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (state.getValue(PERSISTENT)) {
            return InteractionResult.FAIL;
        }
        if (!level.isClientSide()) {
            Block.popResource(level, pos, new ItemStack(ModItems.COCONUT));
            level.playSound(null, pos, SoundEvents.ITEM_FRAME_REMOVE_ITEM, SoundSource.NEUTRAL, 0.5f, 1f);

            BlockState newState = ModBlocks.PALM_LEAVES.defaultBlockState()
                    .setValue(DISTANCE, state.getValue(DISTANCE))
                    .setValue(PERSISTENT, false)
                    .setValue(HAS_COCONUT, true)
                    .setValue(WATERLOGGED, state.getValue(WATERLOGGED));
            level.setBlock(pos, newState, Block.UPDATE_ALL);
        }
        return InteractionResult.SUCCESS;
    }
}
