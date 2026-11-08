package net.kasax.raft.gametest.mixin;

import net.kasax.raft.content.world.StructureFoundations;
import net.kasax.raft.gametest.BcScript;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.PoolElementStructurePiece;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Script command "footings off": structures generate without their footings (to compare before and after). */
@Mixin(StructureFoundations.class)
public abstract class FootingsToggleMixin {
    @Inject(method = "place", at = @At("HEAD"), cancellable = true, remap = false)
    private static void bc$toggle(PoolElementStructurePiece piece, WorldGenLevel level, BoundingBox chunkBox, CallbackInfo ci) {
        if (!BcScript.footings) {
            ci.cancel();
        }
    }
}
