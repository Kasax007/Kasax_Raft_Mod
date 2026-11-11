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

    // NeoForge: getGrowthSpeed counts every FarmBlock (the crop plot is one) and asks isFertile (moisture of any
    // FarmBlock), so the crop plot already speeds crops up like farmland; Fabric's redirect of is(FARMLAND) is not needed.
}
