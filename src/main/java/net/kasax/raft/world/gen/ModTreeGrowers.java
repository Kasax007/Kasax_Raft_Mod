package net.kasax.raft.world.gen;

import net.minecraft.world.level.block.grower.TreeGrower;

import java.util.Optional;

/** Tree growers for the raft saplings (they grow the configured features of {@link ModConfiguredFeatures}). */
public class ModTreeGrowers {
    public static final TreeGrower DRIFTWOOD = new TreeGrower("raft_driftwood", Optional.empty(),
            Optional.of(ModConfiguredFeatures.DRIFTWOOD_KEY), Optional.empty());

    public static final TreeGrower PALM = new TreeGrower("raft_palm", Optional.empty(),
            Optional.of(ModConfiguredFeatures.PALMWOOD_KEY), Optional.empty());
}
