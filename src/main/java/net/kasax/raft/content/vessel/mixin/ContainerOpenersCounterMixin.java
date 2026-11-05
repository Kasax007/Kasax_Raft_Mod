package net.kasax.raft.content.vessel.mixin;

import net.kasax.raft.content.vessel.VesselShipyard;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.ContainerUser;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.ContainerOpenersCounter;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.ArrayList;
import java.util.List;

/**
 * A chest (barrel, ...) on a sailing raft is in the raft's shipyard plot, far from the players using it: count its
 * openers among all players of the level, not only the ones next to the plot (else it would close itself, with its
 * closing sound, while a player still has it open).
 */
@Mixin(ContainerOpenersCounter.class)
public abstract class ContainerOpenersCounterMixin {
    @Shadow
    private boolean hasContainerOpen(Entity entity, BlockPos blockPos) {
        throw new AssertionError();
    }

    @Inject(method = "getEntitiesWithContainerOpen", at = @At("RETURN"), cancellable = true)
    private void raft$openersOnRaft(Level level, BlockPos pos, CallbackInfoReturnable<List<ContainerUser>> cir) {
        if (!VesselShipyard.maybePlot(pos.getX(), pos.getZ()) || level.isClientSide()) {
            return;
        }
        List<ContainerUser> users = new ArrayList<>();
        for (Player player : level.players()) {
            if (this.hasContainerOpen(player, pos)) {
                users.add(player);
            }
        }
        cir.setReturnValue(users);
    }
}
