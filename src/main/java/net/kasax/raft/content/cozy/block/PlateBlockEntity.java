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
import net.kasax.raft.compat.ValueInput;
import net.kasax.raft.compat.ValueOutput;
import org.jetbrains.annotations.Nullable;

/** The food on a serving plate (synced to clients for the renderer). */
public class PlateBlockEntity extends BlockEntity implements net.kasax.raft.compat.RemovalSideEffects {
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
    protected void saveAdditional(net.minecraft.nbt.CompoundTag tag121, net.minecraft.core.HolderLookup.Provider reg121) {
        ValueOutput output = ValueOutput.of(tag121, reg121);
        super.saveAdditional(tag121, reg121);
        if (!food.isEmpty()) output.store("food", ItemStack.CODEC, food);
    }

    @Override
    protected void loadAdditional(net.minecraft.nbt.CompoundTag tag121, net.minecraft.core.HolderLookup.Provider reg121) {
        ValueInput input = ValueInput.of(tag121, reg121);
        super.loadAdditional(tag121, reg121);
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
