package net.kasax.raft.compat.client;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.kasax.raft.platform.client.BlockRenderLayerMap;
import net.kasax.raft.Raft;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

/**
 * 1.21.1: blocks with clear (cutout) or half clear (translucent) pixels need their render layer registered; the newer
 * game finds it from the textures. The list is worked out from the textures by tools/port1211/layers.py.
 */
public final class RenderLayers {
    private RenderLayers() {
    }

    public static void init() {
        try (InputStream in = RenderLayers.class.getResourceAsStream("/assets/raft/port1211/render_layers.json")) {
            if (in == null) {
                Raft.LOGGER.warn("Beach Craft: no render layer list");
                return;
            }
            JsonObject j = JsonParser.parseReader(new InputStreamReader(in, StandardCharsets.UTF_8)).getAsJsonObject();
            int n = 0;
            for (var e : j.getAsJsonArray("cutout")) {
                var block = BuiltInRegistries.BLOCK.getOptional(ResourceLocation.parse(e.getAsString()));
                if (block.isPresent()) {
                    BlockRenderLayerMap.INSTANCE.putBlock(block.get(), RenderType.cutout());
                    n++;
                }
            }
            for (var e : j.getAsJsonArray("translucent")) {
                var block = BuiltInRegistries.BLOCK.getOptional(ResourceLocation.parse(e.getAsString()));
                if (block.isPresent()) {
                    BlockRenderLayerMap.INSTANCE.putBlock(block.get(), RenderType.translucent());
                    n++;
                }
            }
            Raft.LOGGER.info("Beach Craft: render layers for {} blocks", n);
        } catch (Exception e) {
            Raft.LOGGER.error("Beach Craft: render layer list unreadable", e);
        }
    }
}
