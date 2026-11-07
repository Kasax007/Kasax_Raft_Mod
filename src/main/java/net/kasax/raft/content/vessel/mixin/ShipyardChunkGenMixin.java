package net.kasax.raft.content.vessel.mixin;

import net.kasax.raft.content.vessel.VesselShipyard;
import net.minecraft.core.Holder;
import net.minecraft.server.level.WorldGenRegion;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeManager;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.levelgen.NoiseBasedChunkGenerator;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.blending.Blender;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Set;
import java.util.concurrent.CompletableFuture;

/** Shipyard chunks get no terrain (noise, surface, carvers) and no mobs: see {@link VesselShipyard#isShipyardChunk}. */
@Mixin(NoiseBasedChunkGenerator.class)
public class ShipyardChunkGenMixin {
    @Inject(method = "buildTerrain", at = @At("HEAD"), cancellable = true)
    private void raft$emptyShipyard(ChunkAccess chunk, Blender blender, RandomState randomState, StructureManager structureManager,
                                    BiomeManager biomeManager, WorldGenRegion carverBiomeRegion, Set<Holder<Biome>> possibleBiomes,
                                    CallbackInfoReturnable<CompletableFuture<ChunkAccess>> cir) {
        if (VesselShipyard.isShipyardChunk(chunk.getPos().x(), chunk.getPos().z())) {
            cir.setReturnValue(CompletableFuture.completedFuture(chunk));
        }
    }

    @Inject(method = "spawnOriginalMobs", at = @At("HEAD"), cancellable = true)
    private void raft$noShipyardMobs(WorldGenRegion region, CallbackInfo ci) {
        if (VesselShipyard.isShipyardChunk(region.getCenter().x(), region.getCenter().z())) {
            ci.cancel();
        }
    }
}
