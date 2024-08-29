package net.kasax.raft.block.custom;

import net.kasax.raft.block.ModBlocks;
import net.kasax.raft.item.ModItems;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.LeavesBlock;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.BooleanProperty;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.World;

public class PalmCoconutLeaves extends LeavesBlock {
    public static final BooleanProperty HAS_COCONUT = BooleanProperty.of("has_coconut");

    public PalmCoconutLeaves(Settings settings) {
        super(settings);
        this.setDefaultState(this.stateManager.getDefaultState().with(HAS_COCONUT, true). with(PERSISTENT, false));
    }

    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        super.appendProperties(builder);
        builder.add(HAS_COCONUT);
    }

    @Override
    public ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, Hand hand, BlockHitResult hit) {
        if (!world.isClient) {
            if (!state.get(PERSISTENT)) {
                // Drop a coconut
                dropStack(world, pos, new ItemStack(ModItems.COCONUT));
                world.playSound(null, pos.getX(), pos.getY(), pos.getZ(), SoundEvents.ENTITY_ITEM_FRAME_REMOVE_ITEM, SoundCategory.NEUTRAL, 0.5f, 1f);

                // Copy over block state properties from the current state to the new PALM_LEAVES block
                BlockState newState = ModBlocks.PALM_LEAVES.getDefaultState()
                        .with(DISTANCE, state.get(DISTANCE))
                        .with(PERSISTENT, false)
                        .with(HAS_COCONUT, true)
                        .with(WATERLOGGED, false);

                // Replace the block with the new PALM_LEAVES
                world.setBlockState(pos, newState, 3);
            }
            else return ActionResult.FAIL;
        }
        return ActionResult.SUCCESS;
    }
}
