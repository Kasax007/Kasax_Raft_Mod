package net.kasax.raft.content.gear;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.kasax.raft.Raft;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.Item;

/**
 * Armour set bonuses, checked once a second: the full shell set gives Resistance I while in water, the full explorer
 * set +10 % walking speed and Luck I (better fishing and chest loot).
 */
public final class GearEvents {
    public static final Identifier EXPLORER_SPEED = Raft.id("explorer_speed");

    private GearEvents() {
    }

    public static void init() {
        ServerTickEvents.END_SERVER_TICK.register(GearEvents::tick);
    }

    public static boolean wears(ServerPlayer p, Item head, Item chest, Item legs, Item feet) {
        return p.getItemBySlot(EquipmentSlot.HEAD).is(head) && p.getItemBySlot(EquipmentSlot.CHEST).is(chest)
                && p.getItemBySlot(EquipmentSlot.LEGS).is(legs) && p.getItemBySlot(EquipmentSlot.FEET).is(feet);
    }

    private static void tick(MinecraftServer server) {
        if (server.getTickCount() % 20 != 0) return;
        for (ServerPlayer p : server.getPlayerList().getPlayers()) {
            if (wears(p, GearContent.SHELL_HELMET, GearContent.SHELL_CHESTPLATE, GearContent.SHELL_LEGGINGS, GearContent.SHELL_BOOTS)
                    && p.isInWater()) {
                p.addEffect(new MobEffectInstance(MobEffects.RESISTANCE, 50, 0, true, false, true));
            }
            boolean explorer = wears(p, GearContent.EXPLORER_HELMET, GearContent.EXPLORER_CHESTPLATE,
                    GearContent.EXPLORER_LEGGINGS, GearContent.EXPLORER_BOOTS);
            AttributeInstance speed = p.getAttribute(Attributes.MOVEMENT_SPEED);
            if (explorer) {
                p.addEffect(new MobEffectInstance(MobEffects.LUCK, 50, 0, true, false, true));
                if (speed != null && !speed.hasModifier(EXPLORER_SPEED)) {
                    speed.addTransientModifier(new AttributeModifier(EXPLORER_SPEED, 0.1, AttributeModifier.Operation.ADD_MULTIPLIED_BASE));
                }
            } else if (speed != null && speed.hasModifier(EXPLORER_SPEED)) {
                speed.removeModifier(EXPLORER_SPEED);
            }
        }
    }
}
