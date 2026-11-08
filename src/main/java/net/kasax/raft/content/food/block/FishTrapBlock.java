package net.kasax.raft.content.food.block;

import net.kasax.raft.content.cozy.block.CozyBlock;
import net.kasax.raft.content.food.FoodContent;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;

/** A wicker fish trap: put it in water (with open water around it) and it slowly fills its 9 slots with catch. */
public class FishTrapBlock extends CozyBlock implements EntityBlock {
    public FishTrapBlock(Properties properties, Config config) {
        super(properties, config);
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new FishTrapBlockEntity(pos, state);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide || type != FoodContent.FISH_TRAP_ENTITY ? null
                : (l, p, s, be) -> FishTrapBlockEntity.serverTick(l, p, s, (FishTrapBlockEntity) be);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (level instanceof ServerLevel && level.getBlockEntity(pos) instanceof FishTrapBlockEntity trap) {
            player.openMenu(trap);
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    protected boolean hasAnalogOutputSignal(BlockState state) {
        return true;
    }

    @Override
    protected int getAnalogOutputSignal(BlockState state, Level level, BlockPos pos) {
        return net.minecraft.world.inventory.AbstractContainerMenu.getRedstoneSignalFromBlockEntity(level.getBlockEntity(pos));
    }
}
