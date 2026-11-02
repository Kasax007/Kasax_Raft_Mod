package net.kasax.raft.world.gen;

import net.minecraft.util.random.WeightedList;
import net.minecraft.world.level.block.grower.TreeGrower;

/** Tree growers for the raft saplings (they grow the configured features of {@link ModConfiguredFeatures}). */
public class ModTreeGrowers {
    public static final TreeGrower DRIFTWOOD = new TreeGrower("raft_driftwood",
            WeightedList.of(ModConfiguredFeatures.DRIFTWOOD_KEY), WeightedList.of(), WeightedList.of(),
            ModConfiguredFeatures.DRIFTWOOD_KEY);

    public static final TreeGrower PALM = new TreeGrower("raft_palm",
            WeightedList.of(ModConfiguredFeatures.PALMWOOD_KEY), WeightedList.of(), WeightedList.of(),
            ModConfiguredFeatures.PALMWOOD_KEY);
}
