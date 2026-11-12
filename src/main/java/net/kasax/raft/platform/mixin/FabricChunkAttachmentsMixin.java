package net.kasax.raft.platform.mixin;

import net.kasax.raft.platform.AttachmentRegistry;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.chunk.PalettedContainerFactory;
import net.minecraft.world.level.chunk.storage.SerializableChunkData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Chunks saved by the Fabric build: their Beach Craft attachments are taken over (see AttachmentRegistry). */
@Mixin(SerializableChunkData.class)
abstract class FabricChunkAttachmentsMixin {
    @Inject(method = "parse", at = @At("HEAD"))
    private static void raft$fabricAttachments(LevelHeightAccessor levelHeight, PalettedContainerFactory containerFactory, CompoundTag chunkData,
                                               CallbackInfoReturnable<SerializableChunkData> cir) {
        AttachmentRegistry.migrateFabricChunkData(chunkData);
    }
}
