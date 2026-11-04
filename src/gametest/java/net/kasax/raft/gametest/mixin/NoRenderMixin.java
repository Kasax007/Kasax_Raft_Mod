package net.kasax.raft.gametest.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import net.kasax.raft.gametest.BcScript;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientChunkCache;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.state.GameRenderState;
import net.minecraft.client.renderer.state.level.ChunkLoadingRenderState;
import net.minecraft.client.renderer.state.level.LevelRenderState;
import net.minecraft.client.renderer.state.level.SectionUpdateRenderState;
import net.minecraft.core.SectionPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * The client draws a frame on every tick. While a clip is recorded (or during "wait"), only the screenshots are needed:
 * skip drawing the per-tick frames (the screenshot renders its own frame), which halves the time per recorded frame
 * on the software renderer. The frame is still extracted (camera, picking, chunk sections), only not drawn.
 *
 * <p>The extraction hands over work that only the drawing does: the chunks that were loaded or unloaded since the last
 * frame and the sections to rebuild (changed blocks). A skipped frame passes them on to the next frame, else they are
 * lost: chunks that arrive while frames are skipped (e.g. a whole dimension after a teleport) would never be drawn and
 * blocks changed meanwhile would keep their old look.
 */
@Mixin(Minecraft.class)
public class NoRenderMixin {
    private static long bctest$last;

    @WrapOperation(method = "renderFrame", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/GameRenderer;render()V"))
    private void bctest$skipFrame(GameRenderer renderer, Operation<Void> original) {
        boolean skip = BcScript.skipFrames;
        if (!skip && BcScript.throttleMs > 0) {
            long now = System.currentTimeMillis();
            if (now - bctest$last < BcScript.throttleMs) {
                skip = true;
            } else {
                bctest$last = now;
            }
        }
        if (skip) {
            bctest$keepLevelUpdates((Minecraft) (Object) this, renderer.gameRenderState());
            return;
        }
        original.call(renderer);
    }

    private static void bctest$keepLevelUpdates(Minecraft mc, GameRenderState game) {
        if (!game.shouldRenderLevel || mc.level == null) {
            return;
        }
        LevelRenderState level = game.levelRenderState;
        for (SectionUpdateRenderState update : level.sectionUpdateRenderStates) {
            long node = update.sectionNode();
            mc.levelExtractor.setSectionDirty(SectionPos.x(node), SectionPos.y(node), SectionPos.z(node));
        }
        level.sectionUpdateRenderStates.clear();
        ChunkLoadingRenderState loading = level.chunkLoadingRenderState;
        ClientChunkCache cache = mc.level.getChunkSource();
        bctest$merge(loading.addedLoadedChunks, loading.removedLoadedChunks, cache.addedLoadedChunks(), cache.removedLoadedChunks());
        bctest$merge(loading.addedEmptySections, loading.removedEmptySections, cache.addedEmptySections(), cache.removedEmptySections());
        // the sets belong to the chunk cache, which clears them on its next flip
        loading.addedLoadedChunks = new LongOpenHashSet();
        loading.removedLoadedChunks = new LongOpenHashSet();
        loading.addedEmptySections = new LongOpenHashSet();
        loading.removedEmptySections = new LongOpenHashSet();
    }

    /** Older changes go under the newer ones: a chunk added then removed again stays removed, and the other way round. */
    private static void bctest$merge(LongOpenHashSet oldAdded, LongOpenHashSet oldRemoved, LongOpenHashSet added, LongOpenHashSet removed) {
        oldAdded.forEach((long node) -> {
            if (!removed.contains(node)) {
                added.add(node);
            }
        });
        oldRemoved.forEach((long node) -> {
            if (!added.contains(node)) {
                removed.add(node);
            }
        });
    }
}
