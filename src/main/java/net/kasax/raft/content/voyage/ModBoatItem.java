package net.kasax.raft.content.voyage;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySelector;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.vehicle.Boat;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.function.Predicate;

/** A boat item that places one of Beach Craft's boat entity types (same rules as the vanilla boat item). */
public class ModBoatItem extends Item {
    private static final Predicate<Entity> ENTITY_PREDICATE = EntitySelector.NO_SPECTATORS.and(Entity::isPickable);
    private final EntityType<? extends Boat> type;

    public ModBoatItem(EntityType<? extends Boat> type, Properties properties) {
        super(properties);
        this.type = type;
    }

    public EntityType<? extends Boat> boatType() {
        return this.type;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        HitResult hit = getPlayerPOVHitResult(level, player, ClipContext.Fluid.ANY);
        if (hit.getType() == HitResult.Type.MISS) return InteractionResultHolder.pass(stack);
        Vec3 view = player.getViewVector(1.0F);
        List<Entity> list = level.getEntities(player, player.getBoundingBox().expandTowards(view.scale(5.0)).inflate(1.0), ENTITY_PREDICATE);
        Vec3 eye = player.getEyePosition();
        for (Entity entity : list) {
            AABB box = entity.getBoundingBox().inflate(entity.getPickRadius());
            if (box.contains(eye)) return InteractionResultHolder.pass(stack);
        }
        if (hit.getType() != HitResult.Type.BLOCK) return InteractionResultHolder.pass(stack);
        Boat boat = this.create(level, hit.getLocation(), stack, player);
        if (boat == null) return InteractionResultHolder.fail(stack);
        boat.setYRot(player.getYRot());
        if (!level.noCollision(boat, boat.getBoundingBox())) return InteractionResultHolder.fail(stack);
        if (!level.isClientSide()) {
            level.addFreshEntity(boat);
            level.gameEvent(player, GameEvent.ENTITY_PLACE, hit.getLocation());
            stack.consume(1, player);
        }
        player.awardStat(Stats.ITEM_USED.get(this));
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
    }

    public Boat create(Level level, Vec3 at, ItemStack stack, Player player) {
        Boat boat = this.type.create(level);
        if (boat == null) return null;
        boat.setPos(at.x, at.y, at.z);
        boat.xo = at.x;
        boat.yo = at.y;
        boat.zo = at.z;
        if (level instanceof ServerLevel serverLevel && player != null) {
            EntityType.<Boat>createDefaultStackConfig(serverLevel, stack, player).accept(boat);
        }
        return boat;
    }
}
