package net.kasax.raft.content.world.mixin;

import net.kasax.raft.content.world.StructureFoundations;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.PoolElementStructurePiece;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Footings under the pieces of Beach Craft structures (StructureFoundations). */
@Mixin(PoolElementStructurePiece.class)
public abstract class PoolPieceFoundationMixin {
    @Inject(method = "place", at = @At("TAIL"))
    private void raft$footings(WorldGenLevel level, StructureManager structureManager, ChunkGenerator generator,
                               RandomSource random, BoundingBox chunkBox, BlockPos referencePos, boolean keepJigsaws,
                               CallbackInfo ci) {
        StructureFoundations.place((PoolElementStructurePiece) (Object) this, level, chunkBox);
    }
}
