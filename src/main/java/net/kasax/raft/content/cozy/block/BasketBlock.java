package net.kasax.raft.content.cozy.block;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.jspecify.annotations.Nullable;

/**
 * Wicker picnic basket: 9 slots; like a shulker box it keeps its contents when picked up (the loot table copies the
 * container component). The lid opens while someone looks inside.
 */
public class BasketBlock extends CozyBlock implements EntityBlock {
    public BasketBlock(Properties properties, Config config) {
        super(properties, config);
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new BasketBlockEntity(pos, state);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (level instanceof ServerLevel && level.getBlockEntity(pos) instanceof BasketBlockEntity basket) {
            player.openMenu(basket);
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public BlockState playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
        // creative players do not get drops: still give them the filled basket, like a shulker box
        if (level instanceof ServerLevel server && player.preventsBlockDrops()
                && level.getBlockEntity(pos) instanceof BasketBlockEntity basket && !basket.isEmpty()) {
            ItemStack stack = new ItemStack(this);
            stack.applyComponents(basket.collectComponents());
            ItemEntity item = new ItemEntity(server, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, stack);
            item.setDefaultPickUpDelay();
            server.addFreshEntity(item);
        }
        return super.playerWillDestroy(level, pos, state, player);
    }

    @Override
    protected boolean hasAnalogOutputSignal(BlockState state) {
        return true;
    }

    @Override
    protected int getAnalogOutputSignal(BlockState state, Level level, BlockPos pos, net.minecraft.core.Direction direction) {
        return net.minecraft.world.inventory.AbstractContainerMenu.getRedstoneSignalFromBlockEntity(level.getBlockEntity(pos));
    }
}
