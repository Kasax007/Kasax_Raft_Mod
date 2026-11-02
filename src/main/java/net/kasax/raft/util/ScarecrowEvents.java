package net.kasax.raft.util;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;
import net.kasax.raft.block.custom.ScarecrowBlock;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.MobCategory;

/** Scarecrows keep natural hostile spawns away (the farmland protection is a mixin). */
public class ScarecrowEvents {
    public static void register() {
        ServerEntityEvents.ALLOW_LOAD.register((entity, level, reason, fromDisk) -> {
            if (entity.getType().getCategory() != MobCategory.MONSTER) {
                return true;
            }
            if (reason != EntitySpawnReason.NATURAL && reason != EntitySpawnReason.CHUNK_GENERATION
                    && reason != EntitySpawnReason.PATROL && reason != EntitySpawnReason.REINFORCEMENT) {
                return true;
            }
            return !ScarecrowBlock.isNear(level, entity.blockPosition(), ScarecrowBlock.SPAWN_RADIUS);
        });
    }
}
