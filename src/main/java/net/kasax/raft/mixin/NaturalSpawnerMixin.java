package net.kasax.raft.mixin;

import net.kasax.raft.block.custom.ScarecrowBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.level.NaturalSpawner;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.biome.MobSpawnSettings;
import net.minecraft.world.level.chunk.ChunkGenerator;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** No natural monster spawns near a scarecrow. */
@Mixin(NaturalSpawner.class)
public abstract class NaturalSpawnerMixin {
    @Inject(method = "isValidSpawnPostitionForType", at = @At("HEAD"), cancellable = true)
    private static void raft$scarecrow(ServerLevel level, MobCategory category, StructureManager structures, ChunkGenerator generator,
                                       MobSpawnSettings.SpawnerData data, BlockPos.MutableBlockPos pos, double distance,
                                       CallbackInfoReturnable<Boolean> cir) {
        if (category == MobCategory.MONSTER && ScarecrowBlock.isNear(level, pos, ScarecrowBlock.SPAWN_RADIUS)) {
            cir.setReturnValue(false);
        }
    }
}
