package net.kasax.raft.compat.mixin;

import net.kasax.raft.Raft;
import net.kasax.raft.compat.RemovalSideEffects;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.Container;
import net.minecraft.world.Containers;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 1.21.1 port helper: before a Beach Craft block entity's block is replaced by another block, run its removal side
 * effects as the newer game does (containers drop their items).
 */
@Mixin(BlockBehaviour.BlockStateBase.class)
public abstract class BlockStateRemoveMixin {
    @Inject(method = "onRemove", at = @At("HEAD"))
    private void raft$preRemove(Level level, BlockPos pos, BlockState newState, boolean movedByPiston, CallbackInfo ci) {
        BlockState self = (BlockState) (Object) this;
        if (level.isClientSide() || !self.hasBlockEntity() || self.is(newState.getBlock())) return;
        BlockEntity be = level.getBlockEntity(pos);
        if (be == null) return;
        if (be instanceof RemovalSideEffects effects) {
            effects.preRemoveSideEffects(pos, self);
            return;
        }
        ResourceLocation type = BuiltInRegistries.BLOCK_ENTITY_TYPE.getKey(be.getType());
        if (type != null && Raft.MOD_ID.equals(type.getNamespace()) && be instanceof Container container
                && !(be instanceof AbstractFurnaceBlockEntity)) {
            Containers.dropContents(level, pos, container);
        }
    }
}
