package net.kasax.raft.mixin;

import net.kasax.raft.block.custom.ScarecrowBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.FarmBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** A scarecrow keeps farmland from being trampled by entities within its protected radius. */
@Mixin(FarmBlock.class)
public abstract class FarmlandBlockMixin {
    @Inject(method = "turnToBaseBlock", at = @At("HEAD"), cancellable = true)
    private static void raft$scarecrowProtectsFarmland(Entity sourceEntity, BlockState state, Level level, BlockPos pos, CallbackInfo ci) {
        if (sourceEntity != null && level instanceof ServerLevel serverLevel
                && ScarecrowBlock.isNear(serverLevel, pos, ScarecrowBlock.TRAMPLE_RADIUS)) {
            ci.cancel();
        }
    }
}
