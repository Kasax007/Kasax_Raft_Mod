package net.kasax.raft.platform;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.FireBlock;

/** Fabric's flammable block registry on the fire block's own table (public on NeoForge). */
public final class FlammableBlockRegistry {
    private static final FlammableBlockRegistry DEFAULT = new FlammableBlockRegistry();

    private FlammableBlockRegistry() {
    }

    public static FlammableBlockRegistry getDefaultInstance() {
        return DEFAULT;
    }

    public void add(Block block, int burn, int spread) {
        ((FireBlock) Blocks.FIRE).setFlammable(block, burn, spread);
    }
}
