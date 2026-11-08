package net.kasax.raft.content.cozy.block;

import net.kasax.raft.content.cozy.CozyContent;
import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.DispenserMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.ContainerOpenersCounter;
import net.minecraft.world.level.block.entity.RandomizableContainerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.kasax.raft.compat.RemovalSideEffects;

/** The 9 slots of a wicker basket. Opening sets the {@code open} state (lid up). */
public class BasketBlockEntity extends RandomizableContainerBlockEntity implements RemovalSideEffects {
    private NonNullList<ItemStack> items = NonNullList.withSize(9, ItemStack.EMPTY);

    private final ContainerOpenersCounter openers = new ContainerOpenersCounter() {
        @Override
        protected void onOpen(Level level, BlockPos pos, BlockState state) {
            level.playSound(null, pos, SoundEvents.BAMBOO_WOOD_TRAPDOOR_OPEN, SoundSource.BLOCKS, 0.6F, 1.1F);
            setOpen(state, true);
        }

        @Override
        protected void onClose(Level level, BlockPos pos, BlockState state) {
            level.playSound(null, pos, SoundEvents.BAMBOO_WOOD_TRAPDOOR_CLOSE, SoundSource.BLOCKS, 0.6F, 1.1F);
            setOpen(state, false);
        }

        @Override
        protected void openerCountChanged(Level level, BlockPos pos, BlockState state, int previous, int current) {
        }

        @Override
        public boolean isOwnContainer(Player player) {
            return player.containerMenu instanceof DispenserMenu && player.containerMenu.slots.size() > 0
                    && player.containerMenu.slots.get(0).container == BasketBlockEntity.this;
        }
    };

    public BasketBlockEntity(BlockPos pos, BlockState state) {
        super(CozyContent.BASKET_ENTITY, pos, state);
    }

    @Override
    protected void saveAdditional(net.minecraft.nbt.CompoundTag tag121, net.minecraft.core.HolderLookup.Provider reg121) {
        super.saveAdditional(tag121, reg121);
        if (!trySaveLootTable(tag121)) ContainerHelper.saveAllItems(tag121, items, false, reg121);
    }

    @Override
    protected void loadAdditional(net.minecraft.nbt.CompoundTag tag121, net.minecraft.core.HolderLookup.Provider reg121) {
        super.loadAdditional(tag121, reg121);
        items = NonNullList.withSize(getContainerSize(), ItemStack.EMPTY);
        if (!tryLoadLootTable(tag121)) ContainerHelper.loadAllItems(tag121, items, reg121);
    }

    @Override
    public int getContainerSize() {
        return 9;
    }

    @Override
    protected NonNullList<ItemStack> getItems() {
        return items;
    }

    @Override
    protected void setItems(NonNullList<ItemStack> items) {
        this.items = items;
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("container.raft.wicker_basket");
    }

    @Override
    protected AbstractContainerMenu createMenu(int containerId, Inventory inventory) {
        return new DispenserMenu(containerId, inventory, this);
    }

    @Override
    public void startOpen(Player user) {
        if (!remove && !user.isSpectator()) {
            openers.incrementOpeners(user, getLevel(), getBlockPos(), getBlockState());
        }
    }

    @Override
    public void stopOpen(Player user) {
        if (!remove && !user.isSpectator()) {
            openers.decrementOpeners(user, getLevel(), getBlockPos(), getBlockState());
        }
    }

    /** 1.21.1: the block's scheduled tick checks who still looks inside (the newer game counts it itself). */
    void recheckOpen() {
        if (!remove) openers.recheckOpeners(getLevel(), getBlockPos(), getBlockState());
    }

    /** Contents stay in the basket item, so nothing spills when it is broken. */
    @Override
    public void preRemoveSideEffects(BlockPos pos, BlockState state) {
    }

    private void setOpen(BlockState state, boolean open) {
        if (level != null && state.hasProperty(CozyBlock.OPEN)) {
            level.setBlock(getBlockPos(), state.setValue(CozyBlock.OPEN, open), Block.UPDATE_ALL);
        }
    }
}
