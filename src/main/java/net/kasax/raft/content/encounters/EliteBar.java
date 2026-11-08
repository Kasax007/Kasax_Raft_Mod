package net.kasax.raft.content.encounters;

import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.BossEvent;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;

/**
 * The boss bar of an elite enemy (the mini-bosses that guard the bigger structures). It is shown to players who see
 * the elite and follows its health; the owner calls {@link #tick}, {@link #seen} and {@link #unseen}.
 */
public final class EliteBar {
    private final LivingEntity owner;
    private final ServerBossEvent event;

    public EliteBar(LivingEntity owner, BossEvent.BossBarColor color) {
        this.owner = owner;
        this.event = new ServerBossEvent(Mth.createInsecureUUID(owner.getRandom()), owner.getDisplayName(), color,
                BossEvent.BossBarOverlay.NOTCHED_10);
        this.event.setVisible(false);
    }

    /** Turns the bar on (an elite) or off (a normal mob of the same kind). */
    public void setVisible(boolean visible) {
        this.event.setVisible(visible);
    }

    public void tick() {
        this.event.setName(this.owner.getDisplayName());
        this.event.setProgress(Mth.clamp(this.owner.getHealth() / this.owner.getMaxHealth(), 0.0F, 1.0F));
    }

    public void seen(ServerPlayer player) {
        this.event.addPlayer(player);
    }

    public void unseen(ServerPlayer player) {
        this.event.removePlayer(player);
    }

    /** Elite stats: bigger, more health (healed to full), harder hits, more armour, hard to knock back. */
    public static void makeElite(LivingEntity mob, double scale, double health, double damage, double armor) {
        set(mob, Attributes.SCALE, scale);
        set(mob, Attributes.MAX_HEALTH, health);
        set(mob, Attributes.ATTACK_DAMAGE, damage);
        set(mob, Attributes.ARMOR, armor);
        set(mob, Attributes.KNOCKBACK_RESISTANCE, 0.6);
        mob.setHealth(mob.getMaxHealth());
    }

    private static void set(LivingEntity mob, net.minecraft.core.Holder<net.minecraft.world.entity.ai.attributes.Attribute> attr, double value) {
        AttributeInstance a = mob.getAttribute(attr);
        if (a != null) {
            a.setBaseValue(value);
        }
    }
}
