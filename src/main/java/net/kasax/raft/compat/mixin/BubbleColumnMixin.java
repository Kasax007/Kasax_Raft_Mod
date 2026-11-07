package net.kasax.raft.compat.mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.BubbleColumnBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 1.21.1 port helper: the newer game lets every block in the tag {@code minecraft:enables_bubble_column_push_up}
 * start a rising bubble column (Beach Craft's bubble vent is in it); 1.21.1 only knows soul sand.
 */
@Mixin(BubbleColumnBlock.class)
public abstract class BubbleColumnMixin {
    @Unique
    private static final TagKey<Block> RAFT$PUSH_UP = TagKey.create(Registries.BLOCK,
            ResourceLocation.withDefaultNamespace("enables_bubble_column_push_up"));

    @Inject(method = "getColumnState", at = @At("HEAD"), cancellable = true)
    private static void raft$pushUp(BlockState below, CallbackInfoReturnable<BlockState> cir) {
        if (below.is(RAFT$PUSH_UP)) {
            cir.setReturnValue(Blocks.BUBBLE_COLUMN.defaultBlockState().setValue(BubbleColumnBlock.DRAG_DOWN, false));
        }
    }

    @Inject(method = "canSurvive", at = @At("HEAD"), cancellable = true)
    private void raft$survive(BlockState state, LevelReader level, BlockPos pos, CallbackInfoReturnable<Boolean> cir) {
        if (level.getBlockState(pos.below()).is(RAFT$PUSH_UP)) cir.setReturnValue(true);
    }
}
