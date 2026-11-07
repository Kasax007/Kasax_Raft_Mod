package net.kasax.raft.compat.mixin;

import net.kasax.raft.block.entity.FurnaceEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** 1.21.1 port helper: the brick and titanium furnaces cook faster (the newer game asks the furnace for its speed). */
@Mixin(AbstractFurnaceBlockEntity.class)
public abstract class FurnaceCookTimeMixin {
    @Inject(method = "getTotalCookTime", at = @At("RETURN"), cancellable = true)
    private static void raft$fasterFurnace(Level level, AbstractFurnaceBlockEntity furnace, CallbackInfoReturnable<Integer> cir) {
        if (furnace instanceof FurnaceEntity raftFurnace) {
            cir.setReturnValue(Math.max(1, (int) Math.round(cir.getReturnValue() / raftFurnace.getSpeedModifier())));
        }
    }
}
