package net.kasax.raft.gametest.neo;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.serialization.JsonOps;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.TreeMap;
import net.kasax.raft.platform.BlockTransformerHelper;
import net.kasax.raft.platform.FlammableBlockRegistry;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.RegistryOps;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.component.BlockTransformers;
import net.minecraft.core.component.BlockTransformer;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.levelgen.blockpredicates.BlockPredicate;
import net.minecraft.world.level.levelgen.feature.stateproviders.RuleBasedStateProvider;
import net.neoforged.neoforge.registries.datamaps.builtin.Flammable;
import net.neoforged.neoforge.registries.datamaps.builtin.Transformable;

/**
 * Writes the NeoForge data maps {@code neoforge:flammable} and {@code neoforge:transformables} (axe stripping) from what
 * the mod's code registers (Fabric's registries in the loader layer), for
 * {@code src/main/resources/data/neoforge/data_maps/block}.
 */
public final class DataMapExport {
    private DataMapExport() {
    }

    /** The axe transform Fabric's stripping helper adds for a block: the block becomes what the provider gives. */
    public static Transformable stripping(Block block, net.minecraft.world.level.levelgen.feature.stateproviders.BlockStateProvider stripped) {
        RuleBasedStateProvider provider = RuleBasedStateProvider.builder()
                .ifTrueThenProvide(BlockPredicate.matchesBlocks(block), stripped)
                .build();
        return new Transformable(BlockTransformers.AXE, BlockTransformer.BlockTransformData.builder(provider).sound(SoundEvents.AXE_STRIP).build());
    }

    public static void write(Path dir, HolderLookup.Provider registries) throws IOException {
        Files.createDirectories(dir);
        Map<String, JsonElement> flammable = new TreeMap<>();
        for (Map.Entry<Block, int[]> e : FlammableBlockRegistry.getDefaultInstance().registered().entrySet()) {
            flammable.put(BuiltInRegistries.BLOCK.getKey(e.getKey()).toString(),
                    Flammable.CODEC.encodeStart(JsonOps.INSTANCE, new Flammable(e.getValue()[0], e.getValue()[1])).getOrThrow());
        }
        RegistryOps<JsonElement> ops = RegistryOps.create(JsonOps.INSTANCE, registries);
        Map<String, JsonElement> transformables = new TreeMap<>();
        BlockTransformerHelper.stripping().forEach((block, provider) -> transformables.put(BuiltInRegistries.BLOCK.getKey(block).toString(),
                Transformable.CODEC.encodeStart(ops, stripping(block, provider)).getOrThrow()));
        writeMap(dir.resolve("flammable.json"), flammable);
        writeMap(dir.resolve("transformables.json"), transformables);
    }

    private static void writeMap(Path file, Map<String, JsonElement> values) throws IOException {
        JsonObject root = new JsonObject();
        JsonObject v = new JsonObject();
        values.forEach(v::add);
        root.add("values", v);
        Files.writeString(file, new GsonBuilder().setPrettyPrinting().create().toJson(root) + "\n", StandardCharsets.UTF_8);
    }
}
