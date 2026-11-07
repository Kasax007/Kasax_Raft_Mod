package net.kasax.raft.content.cozy.block;

import net.kasax.raft.content.cozy.CozyContent;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;

/** The food on a serving plate (synced to clients for the renderer). */
public class PlateBlockEntity extends BlockEntity {
    private ItemStack food = ItemStack.EMPTY;

    public PlateBlockEntity(BlockPos pos, BlockState state) {
        super(CozyContent.PLATE_ENTITY, pos, state);
    }

    public ItemStack food() {
        return food;
    }

    public void setFood(ItemStack stack) {
        food = stack.isEmpty() ? ItemStack.EMPTY : stack;
        setChanged();
        if (level != null) level.sendBlockUpdated(getBlockPos(), getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        if (!food.isEmpty()) output.store("food", ItemStack.CODEC, food);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        food = input.read("food", ItemStack.CODEC).orElse(ItemStack.EMPTY);
    }

    @Override
    public @Nullable Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        return saveCustomOnly(registries);
    }

    @Override
    public void preRemoveSideEffects(BlockPos pos, BlockState state) {
        if (level != null) PlateBlock.dropFood(level, pos, this);
    }
}
