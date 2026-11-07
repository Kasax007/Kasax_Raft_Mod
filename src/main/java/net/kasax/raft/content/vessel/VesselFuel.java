package net.kasax.raft.content.vessel;

import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.FurnaceBlockEntity;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.Vec3;

import java.util.Optional;

/** Fuel of the raft motor: any furnace fuel, burning as long as in a furnace; the tank holds 20 minutes. */
public final class VesselFuel {
    public static final int MAX_FUEL = 20 * 60 * 20;

    private VesselFuel() {
    }

    public static boolean isFuel(ItemStack stack) {
        return net.kasax.raft.compat.ItemProps.isFuel(stack);
    }

    /** Burn time of a fuel item in a furnace (the same data driven value the furnace uses). */
    public static int burnTicks(ServerLevel level, BlockPos pos, ItemStack stack) {
        return net.kasax.raft.compat.ItemProps.burnTime(stack);
    }

    /**
     * Puts one fuel item from the player's hand into a motor tank holding {@code fuel} ticks and returns the new
     * amount (unchanged when the tank is full or the item does not burn). Shows the result to the player.
     */
    public static int refuel(ServerLevel level, BlockPos pos, Player player, ItemStack stack, int fuel) {
        if (fuel >= MAX_FUEL) {
            player.displayClientMessage(Component.translatable("message.raft.vessel.motor_full"), true);
            return fuel;
        }
        int burn = burnTicks(level, pos, stack);
        if (burn <= 0) {
            return fuel;
        }
        net.minecraft.world.item.Item remainder = stack.getItem().getCraftingRemainingItem();
        if (!player.hasInfiniteMaterials()) {
            stack.shrink(1);
            if (remainder != null) {
                ItemStack rest = new ItemStack(remainder);
                if (!player.getInventory().add(rest)) {
                    player.drop(rest, false);
                }
            }
        }
        int result = Math.min(MAX_FUEL, fuel + burn);
        level.playSound(null, pos, SoundEvents.FIRECHARGE_USE, SoundSource.BLOCKS, 0.4F, 1.4F);
        showFuel(player, result);
        return result;
    }

    public static void showFuel(Player player, int fuel) {
        if (fuel <= 0) {
            player.displayClientMessage(Component.translatable("message.raft.vessel.motor_empty"), true);
            return;
        }
        int seconds = fuel / 20;
        player.displayClientMessage(Component.translatable("message.raft.vessel.motor_fuel",
                String.format(java.util.Locale.ROOT, "%d:%02d", seconds / 60, seconds % 60)), true);
    }
}
