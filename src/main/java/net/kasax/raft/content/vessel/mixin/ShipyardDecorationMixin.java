package net.kasax.raft.content.vessel.mixin;

import net.kasax.raft.content.vessel.VesselShipyard;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkGenerator;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Shipyard chunks get no features and no structures: see {@link VesselShipyard#isShipyardChunk}. */
@Mixin(ChunkGenerator.class)
public class ShipyardDecorationMixin {
    @Inject(method = "applyBiomeDecoration", at = @At("HEAD"), cancellable = true)
    private void raft$noShipyardFeatures(WorldGenLevel level, ChunkAccess chunk, StructureManager structureManager, CallbackInfo ci) {
        if (VesselShipyard.isShipyardChunk(chunk.getPos().x(), chunk.getPos().z())) {
            ci.cancel();
        }
    }

    @Inject(method = "createStructures", at = @At("HEAD"), cancellable = true)
    private void raft$noShipyardStructures(net.minecraft.core.RegistryAccess registryAccess,
                                           net.minecraft.world.level.chunk.ChunkGeneratorStructureState state,
                                           StructureManager structureManager, ChunkAccess centerChunk,
                                           net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplateManager templates,
                                           net.minecraft.resources.ResourceKey<net.minecraft.world.level.Level> level, CallbackInfo ci) {
        if (VesselShipyard.isShipyardChunk(centerChunk.getPos().x(), centerChunk.getPos().z())) {
            ci.cancel();
        }
    }
}
