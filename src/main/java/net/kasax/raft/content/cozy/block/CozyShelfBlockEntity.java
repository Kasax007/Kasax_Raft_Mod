package net.kasax.raft.content.cozy.block;

import net.kasax.raft.content.cozy.CozyContent;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import org.jetbrains.annotations.Nullable;

/** 1.21.1 port: the three items of a shelf ({@link CozyShelfBlock}); hoppers and comparators see it as a container. */
public class CozyShelfBlockEntity extends BlockEntity implements Container {
    public static final int MAX_ITEMS = 3;
    private static final String ALIGN_ITEMS_TO_BOTTOM = "align_items_to_bottom";
    private final NonNullList<ItemStack> items = NonNullList.withSize(MAX_ITEMS, ItemStack.EMPTY);
    private boolean alignItemsToBottom;

    public CozyShelfBlockEntity(BlockPos pos, BlockState state) {
        super(CozyContent.SHELF_ENTITY, pos, state);
    }

    public NonNullList<ItemStack> getItems() {
        return items;
    }

    public boolean alignItemsToBottom() {
        return alignItemsToBottom;
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        items.clear();
        ContainerHelper.loadAllItems(tag, items, registries);
        alignItemsToBottom = tag.getBoolean(ALIGN_ITEMS_TO_BOTTOM);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        ContainerHelper.saveAllItems(tag, items, true, registries);
        tag.putBoolean(ALIGN_ITEMS_TO_BOTTOM, alignItemsToBottom);
    }

    @Override
    public @Nullable Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = new CompoundTag();
        ContainerHelper.saveAllItems(tag, items, true, registries);
        tag.putBoolean(ALIGN_ITEMS_TO_BOTTOM, alignItemsToBottom);
        return tag;
    }

    @Override
    protected void applyImplicitComponents(BlockEntity.DataComponentInput components) {
        super.applyImplicitComponents(components);
        components.getOrDefault(DataComponents.CONTAINER, ItemContainerContents.EMPTY).copyInto(items);
    }

    @Override
    protected void collectImplicitComponents(DataComponentMap.Builder components) {
        super.collectImplicitComponents(components);
        components.set(DataComponents.CONTAINER, ItemContainerContents.fromItems(items));
    }

    @Override
    public void removeComponentsFromTag(CompoundTag tag) {
        tag.remove("Items");
    }

    /** Puts the stack on the slot and returns what stood there (no update sent). */
    public ItemStack swapItemNoUpdate(int slot, ItemStack stack) {
        ItemStack old = items.get(slot);
        items.set(slot, stack);
        return old;
    }

    /** Saves, tells the clients and sends the game event (null: none). */
    public void setChanged(@Nullable Holder<GameEvent> event) {
        super.setChanged();
        if (level != null) {
            if (event != null) level.gameEvent(event, worldPosition, GameEvent.Context.of(getBlockState()));
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }

    @Override
    public void setChanged() {
        setChanged(GameEvent.BLOCK_ACTIVATE);
    }

    // ------------------------------------------------------------------------------------------------ container
    @Override
    public int getContainerSize() {
        return MAX_ITEMS;
    }

    @Override
    public boolean isEmpty() {
        for (ItemStack stack : items) if (!stack.isEmpty()) return false;
        return true;
    }

    @Override
    public ItemStack getItem(int slot) {
        return items.get(slot);
    }

    @Override
    public ItemStack removeItem(int slot, int amount) {
        ItemStack out = ContainerHelper.removeItem(items, slot, amount);
        if (!out.isEmpty()) setChanged();
        return out;
    }

    @Override
    public ItemStack removeItemNoUpdate(int slot) {
        return ContainerHelper.takeItem(items, slot);
    }

    @Override
    public void setItem(int slot, ItemStack stack) {
        items.set(slot, stack);
        stack.limitSize(getMaxStackSize(stack));
        setChanged();
    }

    @Override
    public boolean stillValid(Player player) {
        return Container.stillValidBlockEntity(this, player);
    }

    @Override
    public void clearContent() {
        items.clear();
    }
}
