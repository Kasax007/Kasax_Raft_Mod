package net.kasax.raft.content.voyage;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.dispenser.BlockSource;
import net.minecraft.core.dispenser.DefaultDispenseItemBehavior;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.vehicle.Boat;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.DispenserBlock;
import net.minecraft.world.phys.Vec3;

/** Dispensers place Beach Craft boats on water like vanilla boats. */
public class ModBoatDispenseBehavior extends DefaultDispenseItemBehavior {
    private final DefaultDispenseItemBehavior fallback = new DefaultDispenseItemBehavior();
    private final EntityType<? extends Boat> type;

    public ModBoatDispenseBehavior(EntityType<? extends Boat> type) {
        this.type = type;
    }

    @Override
    public ItemStack execute(BlockSource source, ItemStack stack) {
        Direction direction = source.state().getValue(DispenserBlock.FACING);
        ServerLevel level = source.level();
        Vec3 center = source.center();
        double d = 0.5625 + this.type.getWidth() / 2.0;
        double x = center.x() + direction.getStepX() * d;
        double y = center.y() + direction.getStepY() * 1.125F;
        double z = center.z() + direction.getStepZ() * d;
        BlockPos front = source.pos().relative(direction);
        double up;
        if (level.getFluidState(front).is(FluidTags.WATER)) {
            up = 1.0;
        } else if (level.getBlockState(front).isAir() && level.getFluidState(front.below()).is(FluidTags.WATER)) {
            up = 0.0;
        } else {
            return this.fallback.dispense(source, stack);
        }
        Boat boat = this.type.create(level);
        if (boat == null) return this.fallback.dispense(source, stack);
        boat.setPos(x, y + up, z);
        EntityType.<Boat>createDefaultStackConfig(level, stack, null).accept(boat);
        boat.setYRot(direction.toYRot());
        level.addFreshEntity(boat);
        stack.shrink(1);
        return stack;
    }

    @Override
    protected void playSound(BlockSource source) {
        source.level().levelEvent(1000, source.pos(), 0);
    }
}
