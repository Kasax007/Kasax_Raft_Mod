package net.kasax.raft.platform.mixin;

import net.kasax.raft.platform.AttachmentRegistry;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ai.village.poi.PoiManager;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.ProtoChunk;
import net.minecraft.world.level.chunk.storage.ChunkSerializer;
import net.minecraft.world.level.chunk.storage.RegionStorageInfo;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Chunks saved by the Fabric build: their Beach Craft attachments are taken over (see AttachmentRegistry). */
@Mixin(ChunkSerializer.class)
abstract class FabricChunkAttachmentsMixin {
    @Inject(method = "read", at = @At("RETURN"))
    private static void raft$fabricAttachments(ServerLevel level, PoiManager poi, RegionStorageInfo info, ChunkPos pos, CompoundTag tag,
                                               CallbackInfoReturnable<ProtoChunk> cir) {
        AttachmentRegistry.migrateFabricData(cir.getReturnValue(), tag, level.registryAccess());
    }
}
