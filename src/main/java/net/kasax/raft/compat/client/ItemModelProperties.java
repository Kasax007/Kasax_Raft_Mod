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
 * gun reloading), raft:gui (energy staff, captain's spyglass, shark tooth spear: flat in GUIs, on the ground, in frames
 * and on shelves, another model in the hand; set by ItemRendererGuiModelMixin) and raft:angle (wreck compass needle). Also the water
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
        for (net.minecraft.world.item.Item item : GUI_MODEL_ITEMS.get()) {
            ItemProperties.register(item, Raft.id("gui"), (stack, level, entity, seed) -> GUI.get() ? 1.0F : 0.0F);
        }
        // wreck compass: the needle turns to the found wreck (lodestone tracker), else to the world spawn like a compass
        ItemProperties.register(net.kasax.raft.content.gear.GearContent.WRECK_COMPASS, Raft.id("angle"),
                new net.minecraft.client.renderer.item.CompassItemPropertyFunction((level, stack, entity) -> {
                    net.minecraft.world.item.component.LodestoneTracker tracker = stack.get(net.minecraft.core.component.DataComponents.LODESTONE_TRACKER);
                    return tracker != null ? tracker.target().orElse(null) : net.minecraft.world.item.CompassItem.getSpawnPosition(level);
                }));
        // Driftwood Fishing Rod: the vanilla 'cast' property (1.21.1 registers it for the vanilla rod only)
        ItemProperties.register(net.kasax.raft.content.gear.GearContent.DRIFTWOOD_FISHING_ROD,
                net.minecraft.resources.ResourceLocation.withDefaultNamespace("cast"), (stack, level, entity, seed) -> {
                    if (!(entity instanceof Player p) || p.fishing == null) return 0.0F;
                    boolean main = entity.getMainHandItem() == stack;
                    boolean off = entity.getOffhandItem() == stack;
                    if (entity.getMainHandItem().getItem() instanceof net.minecraft.world.item.FishingRodItem) off = false;
                    return main || off ? 1.0F : 0.0F;
                });
    }

    /** Items with a flat GUI model and a different model in the hand (the newer game's display context select). */
    public static final java.util.function.Supplier<java.util.Set<net.minecraft.world.item.Item>> GUI_MODEL_ITEMS =
            com.google.common.base.Suppliers.memoize(() -> java.util.Set.of(ModItems.ENERGY_STAFF,
                    net.kasax.raft.content.gear.GearContent.CAPTAINS_SPYGLASS, net.kasax.raft.content.gear.GearContent.SHARK_TOOTH_SPEAR));

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
