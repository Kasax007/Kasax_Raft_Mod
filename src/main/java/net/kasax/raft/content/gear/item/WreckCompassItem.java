package net.kasax.raft.content.gear.item;

import net.kasax.raft.Raft;
import net.kasax.raft.item.custom.TooltipItem;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.TagKey;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.LodestoneTracker;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.structure.Structure;

import java.util.Optional;

/**
 * Wreck compass: use it and the needle turns to the nearest shipwreck (structure tag {@code raft:wrecks}: galleons,
 * cargo ships, fishing boats, sunken planes and vanilla shipwrecks) within about 1000 blocks. Works in the Endless Sea
 * and the overworld; 5 seconds between searches.
 */
public class WreckCompassItem extends TooltipItem {
    public static final TagKey<Structure> WRECKS = TagKey.create(Registries.STRUCTURE, Raft.id("wrecks"));

    public WreckCompassItem(Properties properties) {
        super(properties, "tooltip.raft.wreck_compass");
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!(level instanceof ServerLevel server)) return InteractionResult.SUCCESS;
        player.getCooldowns().addCooldown(stack, 100);
        BlockPos found = server.findNearestMapStructure(WRECKS, player.blockPosition(), 64, false);
        if (found == null) {
            player.sendOverlayMessage(Component.translatable("message.raft.wreck_compass.none"));
            stack.remove(DataComponents.LODESTONE_TRACKER);
            return InteractionResult.SUCCESS;
        }
        stack.set(DataComponents.LODESTONE_TRACKER, new LodestoneTracker(Optional.of(GlobalPos.of(level.dimension(), found)), false));
        int distance = (int) Math.sqrt(found.distSqr(new BlockPos(player.getBlockX(), found.getY(), player.getBlockZ())));
        player.sendOverlayMessage(Component.translatable("message.raft.wreck_compass.found", distance));
        server.playSound(null, player.blockPosition(), SoundEvents.LODESTONE_COMPASS_LOCK, SoundSource.PLAYERS, 1.0F, 1.0F);
        return InteractionResult.SUCCESS;
    }
}
