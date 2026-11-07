package net.kasax.raft.content.voyage;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySelector;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.function.Consumer;

/** Places an Expedition Raft on water (like a boat item); the raft gets the upgrades stored on the item. */
public class ExpeditionRaftItem extends Item {
    public ExpeditionRaftItem(Properties properties) {
        super(properties);
    }

    @Override
    public net.minecraft.world.InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        return net.kasax.raft.compat.Results.holder(this.use26(level, player, hand), player.getItemInHand(hand));
    }

    private InteractionResult use26(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        HitResult hit = getPlayerPOVHitResult(level, player, ClipContext.Fluid.ANY);
        if (hit.getType() != HitResult.Type.BLOCK) {
            return InteractionResult.PASS;
        }
        Vec3 view = player.getViewVector(1.0F);
        List<Entity> entities = level.getEntities(player, player.getBoundingBox().expandTowards(view.scale(5.0)).inflate(1.0),
                EntitySelector.NO_SPECTATORS.and(net.minecraft.world.entity.Entity::isPickable));
        Vec3 eye = player.getEyePosition();
        for (Entity entity : entities) {
            AABB box = entity.getBoundingBox().inflate(entity.getPickRadius());
            if (box.contains(eye)) {
                return InteractionResult.PASS;
            }
        }
        ExpeditionRaft raft = VoyageContent.EXPEDITION_RAFT.create(level);
        if (raft == null) {
            return InteractionResult.FAIL;
        }
        Vec3 at = hit.getLocation();
        raft.setPos(at.x, at.y, at.z);
        raft.xo = at.x;
        raft.yo = at.y;
        raft.zo = at.z;
        raft.setUpgrades(stack.getOrDefault(VoyageContent.RAFT_UPGRADES, 0));
        raft.setYRot(player.getYRot());
        if (!level.noCollision(raft, raft.getBoundingBox())) {
            if (!level.isClientSide()) {
                player.displayClientMessage(Component.translatable("message.raft.expedition_raft.no_room"), true);
            }
            return InteractionResult.FAIL;
        }
        if (level instanceof ServerLevel serverLevel) {
            EntityType.<ExpeditionRaft>createDefaultStackConfig(serverLevel, stack, player).accept(raft);
            serverLevel.addFreshEntity(raft);
            serverLevel.gameEvent(player, GameEvent.ENTITY_PLACE, at);
            stack.consume(1, player);
        }
        player.awardStat(Stats.ITEM_USED.get(this));
        return InteractionResult.SUCCESS;
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, java.util.List<net.minecraft.network.chat.Component> builderList121, TooltipFlag flag) {
        java.util.function.Consumer<net.minecraft.network.chat.Component> builder = builderList121::add;
        builder.accept(Component.translatable("tooltip.raft.expedition_raft").withStyle(ChatFormatting.AQUA));
        int mask = stack.getOrDefault(VoyageContent.RAFT_UPGRADES, 0);
        if (mask != 0) {
            builder.accept(Component.translatable("tooltip.raft.expedition_raft.upgrades").withStyle(ChatFormatting.GRAY));
            for (RaftUpgrade upgrade : RaftUpgrade.values()) {
                if (upgrade.in(mask)) {
                    builder.accept(Component.literal(" ").append(Component.translatable(upgrade.langKey())).withStyle(ChatFormatting.GOLD));
                }
            }
        } else {
            builder.accept(Component.translatable("tooltip.raft.expedition_raft.no_upgrades").withStyle(ChatFormatting.GRAY));
        }
        super.appendHoverText(stack, context, builderList121, flag);
    }
}
