package net.kasax.raft.platform;

import net.neoforged.neoforge.common.world.BiomeModifier;

/** Fabric's biome modification phases and the NeoForge phase each one runs in. */
public enum ModificationPhase {
    ADDITIONS(BiomeModifier.Phase.ADD),
    REMOVALS(BiomeModifier.Phase.REMOVE),
    REPLACEMENTS(BiomeModifier.Phase.MODIFY),
    POST_PROCESSING(BiomeModifier.Phase.AFTER_EVERYTHING);

    final BiomeModifier.Phase neo;

    ModificationPhase(BiomeModifier.Phase neo) {
        this.neo = neo;
    }
}
