package net.kasax.raft.gametest.neo;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.TreeMap;
import net.kasax.raft.platform.CompostingChanceRegistry;
import net.kasax.raft.platform.FuelRegistry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.Item;

/**
 * Writes the NeoForge data maps {@code neoforge:compostables} and {@code neoforge:furnace_fuels} from what the mod's
 * code registers (Fabric's registries in the loader layer), for {@code src/main/resources/data/neoforge/data_maps/item}.
 */
public final class DataMapExport {
    private DataMapExport() {
    }

    public static void write(Path dir) throws IOException {
        Files.createDirectories(dir);
        Map<String, JsonObject> compost = new TreeMap<>();
        for (Map.Entry<Item, Float> e : CompostingChanceRegistry.INSTANCE.registered().entrySet()) {
            JsonObject v = new JsonObject();
            v.addProperty("chance", e.getValue());
            compost.put(BuiltInRegistries.ITEM.getKey(e.getKey()).toString(), v);
        }
        Map<String, JsonObject> fuels = new TreeMap<>();
        for (Map.Entry<Item, Integer> e : FuelRegistry.INSTANCE.registered().entrySet()) {
            JsonObject v = new JsonObject();
            v.addProperty("burn_time", e.getValue());
            fuels.put(BuiltInRegistries.ITEM.getKey(e.getKey()).toString(), v);
        }
        writeMap(dir.resolve("compostables.json"), compost);
        writeMap(dir.resolve("furnace_fuels.json"), fuels);
    }

    private static void writeMap(Path file, Map<String, JsonObject> values) throws IOException {
        JsonObject root = new JsonObject();
        JsonObject v = new JsonObject();
        values.forEach(v::add);
        root.add("values", v);
        Files.writeString(file, new GsonBuilder().setPrettyPrinting().create().toJson(root) + "\n", StandardCharsets.UTF_8);
    }
}
