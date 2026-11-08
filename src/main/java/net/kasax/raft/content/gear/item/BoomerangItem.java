package net.kasax.raft.content.gear.item;

import net.kasax.raft.content.gear.GearContent;
import net.kasax.raft.content.gear.entity.BoomerangEntity;
import net.kasax.raft.item.custom.TooltipItem;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/** The driftwood boomerang: use to throw it; it loses one point of durability per throw. */
public class BoomerangItem extends TooltipItem {
    public BoomerangItem(Properties properties) {
        super(properties, "tooltip.raft.boomerang", "tooltip.raft.boomerang.1");
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level instanceof ServerLevel server) {
            ItemStack thrown = stack.copyWithCount(1);
            boolean creative = player.hasInfiniteMaterials();
            if (!creative) {
                thrown.setDamageValue(thrown.getDamageValue() + 1);
                if (thrown.getDamageValue() >= thrown.getMaxDamage()) {
                    server.playSound(null, player.getX(), player.getY(), player.getZ(), net.minecraft.sounds.SoundEvents.ITEM_BREAK, SoundSource.PLAYERS, 0.8F, 1.0F);
                    stack.shrink(1);
                    return InteractionResult.SUCCESS;
                }
            }
            BoomerangEntity boomerang = new BoomerangEntity(level, player, thrown, creative);
            boomerang.shootFromRotation(player, player.getXRot(), player.getYRot(), 0.0F, 1.2F, 0.5F);
            server.addFreshEntity(boomerang);
            server.playSound(null, player.getX(), player.getY(), player.getZ(), GearContent.BOOMERANG_THROW,
                    SoundSource.PLAYERS, 0.8F, 1.0F);
            if (!creative) stack.shrink(1);
        }
        player.getCooldowns().addCooldown(stack, 10);
        return InteractionResult.SUCCESS;
    }
}
