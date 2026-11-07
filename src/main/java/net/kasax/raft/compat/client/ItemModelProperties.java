package net.kasax.raft.compat.client;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.kasax.raft.Raft;
import net.kasax.raft.content.diving.DivingContent;
import net.kasax.raft.item.ModItems;
import net.minecraft.client.renderer.item.ItemProperties;
import net.minecraft.core.Holder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.biome.Biome;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/**
 * 1.21.1 port: what the newer game's item model definitions decide is done with item properties here (the resource
 * converter turned the definitions into overrides): raft:lamp (diving helmet with its lamp), raft:cooldown (harpoon
 * gun reloading) and raft:gui (energy staff: flat in GUIs, 3D in the hand; set by ItemRendererMixin). Also the water
 * fog distance per biome that the newer game keeps in biome attributes (assets/raft/port1211/water_fog.json).
 */
@Environment(EnvType.CLIENT)
public final class ItemModelProperties {
    /** True while the item renderer asks for the GUI look of an item. */
    public static final ThreadLocal<Boolean> GUI = ThreadLocal.withInitial(() -> false);
    private static Map<String, Float> waterFog;

    private ItemModelProperties() {
    }

    public static void init() {
        ItemProperties.register(DivingContent.DIVING_HELMET, Raft.id("lamp"),
                (stack, level, entity, seed) -> stack.has(DivingContent.DIVING_LAMP) ? 1.0F : 0.0F);
        ItemProperties.register(DivingContent.HARPOON_GUN, Raft.id("cooldown"),
                (stack, level, entity, seed) -> entity instanceof Player p ? p.getCooldowns().getCooldownPercent(stack.getItem(), 0.0F) : 0.0F);
        ItemProperties.register(ModItems.ENERGY_STAFF, Raft.id("gui"), (stack, level, entity, seed) -> GUI.get() ? 1.0F : 0.0F);
    }

    /** Water fog distance factor of a biome (1 = vanilla). */
    public static float waterFogFactor(Holder<Biome> biome) {
        if (waterFog == null) {
            Map<String, Float> m = new HashMap<>();
            try (InputStream in = ItemModelProperties.class.getResourceAsStream("/assets/raft/port1211/water_fog.json")) {
                if (in != null) {
                    JsonObject o = JsonParser.parseReader(new InputStreamReader(in, StandardCharsets.UTF_8)).getAsJsonObject();
                    o.entrySet().forEach(e -> m.put(e.getKey(), e.getValue().getAsFloat()));
                }
            } catch (Exception e) {
                Raft.LOGGER.warn("water fog distances not read: {}", e.toString());
            }
            waterFog = m;
        }
        return biome.unwrapKey().map(k -> waterFog.getOrDefault(k.location().toString(), 1.0F)).orElse(1.0F);
    }
}
