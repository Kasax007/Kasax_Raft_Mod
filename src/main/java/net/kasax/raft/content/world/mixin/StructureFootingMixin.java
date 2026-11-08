package net.kasax.raft.content.world.mixin;

import net.kasax.raft.content.world.StructureFoundations;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.pieces.PiecesContainer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Footings and the rock skirt of Beach Craft structures, after their pieces are placed in a chunk (StructureFoundations). */
@Mixin(Structure.class)
public abstract class StructureFootingMixin {
    @Inject(method = "afterPlace", at = @At("TAIL"))
    private void raft$footings(WorldGenLevel level, StructureManager structureManager, ChunkGenerator generator,
                               RandomSource random, BoundingBox chunkBox, ChunkPos chunkPos, PiecesContainer pieces,
                               CallbackInfo ci) {
        StructureFoundations.place((Structure) (Object) this, level, chunkBox, pieces);
    }
}
