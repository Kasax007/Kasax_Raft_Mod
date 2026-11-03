package net.kasax.raft.content.diving.item;

import net.kasax.raft.content.diving.entity.Harpoon;
import net.minecraft.core.Direction;
import net.minecraft.core.Position;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ProjectileItem;
import net.minecraft.world.level.Level;

/** Ammunition of the harpoon gun; dispensers can fire it too. */
public class HarpoonItem extends Item implements ProjectileItem {
    public HarpoonItem(Properties properties) {
        super(properties);
    }

    @Override
    public Projectile asProjectile(Level level, Position position, ItemStack stack, Direction direction) {
        return new Harpoon(level, position.x(), position.y(), position.z(), stack.copyWithCount(1));
    }
}
