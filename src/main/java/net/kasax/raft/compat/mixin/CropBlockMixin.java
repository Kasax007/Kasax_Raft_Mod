package net.kasax.raft.compat.mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 1.21.1 port helper: the newer game plants crops on the blocks of the tag {@code minecraft:supports_crops} and lets
 * the blocks of {@code minecraft:grows_crops} speed them up like farmland. 1.21.1 only knows farmland; Beach Craft's
 * crop plot is in both tags.
 */
@Mixin(CropBlock.class)
public abstract class CropBlockMixin {
    @Inject(method = "mayPlaceOn", at = @At("HEAD"), cancellable = true)
    private void raft$supportsCrops(BlockState state, BlockGetter level, BlockPos pos, CallbackInfoReturnable<Boolean> cir) {
        if (state.is(TagKey.create(Registries.BLOCK, ResourceLocation.withDefaultNamespace("supports_crops")))) {
            cir.setReturnValue(true);
        }
    }

    @Redirect(method = "getGrowthSpeed", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/level/block/state/BlockState;is(Lnet/minecraft/world/level/block/Block;)Z"))
    private static boolean raft$growsCrops(BlockState state, Block block) {
        return state.is(block) || block == Blocks.FARMLAND
                && state.is(TagKey.create(Registries.BLOCK, ResourceLocation.withDefaultNamespace("grows_crops")))
                && state.hasProperty(net.minecraft.world.level.block.FarmBlock.MOISTURE);
    }
}
