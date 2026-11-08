package net.kasax.raft.content.decor.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.core.UUIDUtil;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.ContainerHelper;

import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.entity.ContainerOpenersCounter;
import net.minecraft.world.level.block.entity.RandomizableContainerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.kasax.raft.compat.ValueInput;
import net.kasax.raft.compat.ValueOutput;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.UUID;
import net.kasax.raft.content.decor.lootr.LootrCompat;

/**
 * The 27 slots of a treasure chest or wooden crate (works with loot tables like vanilla chests). Opening sets the
 * block's {@code open} state and plays the block's open/close sound.
 */
public class DecorChestBlockEntity extends RandomizableContainerBlockEntity {
    private NonNullList<ItemStack> items = NonNullList.withSize(27, ItemStack.EMPTY);
    /** With Lootr (3.4): the id of this container's per-player inventories, whether anyone opened it, and the Lootr
     * view of it (LootrCompat). Plain fields: nothing here needs Lootr. */
    private UUID lootrId;
    private boolean lootrOpened;
    private Object lootrView;

    private final ContainerOpenersCounter openers = new ContainerOpenersCounter() {
        @Override
        protected void onOpen(Level level, BlockPos pos, BlockState state) {
            sound(state, true);
            setOpen(state, true);
        }

        @Override
        protected void onClose(Level level, BlockPos pos, BlockState state) {
            sound(state, false);
            setOpen(state, false);
        }

        @Override
        protected void openerCountChanged(Level level, BlockPos pos, BlockState state, int previous, int current) {
        }

        @Override
        public boolean isOwnContainer(Player player) {
            return player.containerMenu instanceof ChestMenu menu && (menu.getContainer() == DecorChestBlockEntity.this
                    || LootrCompat.isOwnMenu(menu.getContainer(), DecorChestBlockEntity.this));
        }
    };

    public DecorChestBlockEntity(BlockEntityType<? extends DecorChestBlockEntity> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    @Override
    protected void saveAdditional(net.minecraft.nbt.CompoundTag tag121, net.minecraft.core.HolderLookup.Provider reg121) {
        ValueOutput output = ValueOutput.of(tag121, reg121);
        super.saveAdditional(output.tag(), output.lookup());
        if (!trySaveLootTable(output.tag())) ContainerHelper.saveAllItems(output.tag(), items, output.lookup());
        if (lootrId != null) {
            output.store("LootrId", UUIDUtil.CODEC, lootrId);
            output.putBoolean("LootrOpened", lootrOpened);
        }
    }

    @Override
    protected void loadAdditional(net.minecraft.nbt.CompoundTag tag121, net.minecraft.core.HolderLookup.Provider reg121) {
        ValueInput input = ValueInput.of(tag121, reg121);
        super.loadAdditional(input.tag(), input.lookup());
        items = NonNullList.withSize(getContainerSize(), ItemStack.EMPTY);
        if (!tryLoadLootTable(input.tag())) ContainerHelper.loadAllItems(input.tag(), items, input.lookup());
        lootrId = input.read("LootrId", UUIDUtil.CODEC).orElse(null);
        lootrOpened = input.getBooleanOr("LootrOpened", false);
    }

    /** With Lootr the loot table stays: each player gets their own roll of it when they open the container. */
    @Override
    public void unpackLootTable(@Nullable Player player) {
        if (!LootrCompat.handles(this)) {
            super.unpackLootTable(player);
        }
    }

    public boolean hasLootrId() {
        return lootrId != null;
    }

    /** The id Lootr keeps this container's inventories under (made the first time Lootr asks). */
    public UUID lootrId() {
        if (lootrId == null) {
            lootrId = UUID.randomUUID();
            setChanged();
        }
        return lootrId;
    }

    public boolean lootrOpened() {
        return lootrOpened;
    }

    public void setLootrOpened(boolean opened) {
        if (lootrOpened != opened) {
            lootrOpened = opened;
            setChanged();
        }
    }

    public Object lootrView() {
        return lootrView;
    }

    public void setLootrView(Object view) {
        lootrView = view;
    }

    @Override
    public int getContainerSize() {
        return 27;
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
        return Component.translatable("container.raft." + BuiltInRegistries.BLOCK.getKey(getBlockState().getBlock()).getPath());
    }

    @Override
    protected AbstractContainerMenu createMenu(int containerId, Inventory inventory) {
        return ChestMenu.threeRows(containerId, inventory, this);
    }

    @Override
    public void startOpen(net.minecraft.world.entity.player.Player user) {
        if (!remove && !user.isSpectator()) {
            openers.incrementOpeners(user, getLevel(), getBlockPos(), getBlockState());
        }
    }

    @Override
    public void stopOpen(net.minecraft.world.entity.player.Player user) {
        if (!remove && !user.isSpectator()) {
            openers.decrementOpeners(user, getLevel(), getBlockPos(), getBlockState());
        }
    }

    void recheckOpen() {
        if (!remove) openers.recheckOpeners(getLevel(), getBlockPos(), getBlockState());
    }

    private void setOpen(BlockState state, boolean open) {
        if (level != null && state.hasProperty(DecorChestBlock.OPEN)) {
            level.setBlockAndUpdate(getBlockPos(), state.setValue(DecorChestBlock.OPEN, open));
        }
    }

    private void sound(BlockState state, boolean open) {
        if (level == null || !(state.getBlock() instanceof DecorChestBlock block)) return;
        SoundEvent event = open ? block.openSound : block.closeSound;
        level.playSound(null, getBlockPos(), event, SoundSource.BLOCKS, 0.5F, level.getRandom().nextFloat() * 0.1F + 0.9F);
    }
}
