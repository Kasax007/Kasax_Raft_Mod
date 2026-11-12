package net.kasax.raft.gametest.neo;

import com.google.gson.GsonBuilder;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;

/**
 * Loader-independent registry dump (only vanilla classes): every id of Beach Craft's namespaces in every registry the
 * server knows (static, world generation, dimensions, reloadable: loot tables), recipes, advancements, and every tag
 * with Beach Craft members. The same class runs on the Fabric and the NeoForge build, the two files must be equal.
 */
public final class RegistryDump {
    private static final Set<String> NAMESPACES = Set.of("raft", "team_reborn_energy");

    private RegistryDump() {
    }

    private static boolean ours(Identifier id) {
        return id != null && NAMESPACES.contains(id.getNamespace());
    }

    public static void dump(MinecraftServer server, Path out) throws IOException {
        Map<String, Object> root = new TreeMap<>();
        Map<String, Set<String>> registries = new TreeMap<>();
        Map<String, Map<String, Set<String>>> tags = new TreeMap<>();
        collect(server.registryAccess(), registries, tags);
        server.reloadableRegistries().lookup().listRegistryKeys().forEach(key -> collectLookup(key.identifier().toString(), server.reloadableRegistries().lookup().lookupOrThrow(key), registries));
        root.put("registries", registries);
        root.put("tags", tags);
        Set<String> recipes = new TreeSet<>();
        server.getRecipeManager().getRecipes().forEach(h -> {
            if (ours(h.id().identifier())) recipes.add(h.id().identifier().toString());
        });
        root.put("recipes", recipes);
        Set<String> advancements = new TreeSet<>();
        server.getAdvancements().getAllAdvancements().forEach(h -> {
            if (ours(h.id())) advancements.add(h.id().toString());
        });
        root.put("advancements", advancements);
        Files.createDirectories(out.toAbsolutePath().getParent());
        Files.writeString(out, new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create().toJson(root), StandardCharsets.UTF_8);
    }

    private static void collect(RegistryAccess access, Map<String, Set<String>> registries, Map<String, Map<String, Set<String>>> tags) {
        access.registries().forEach(entry -> collect(entry.key().identifier().toString(), entry.value(), registries, tags));
    }

    private static <T> void collectLookup(String name, net.minecraft.core.HolderLookup.RegistryLookup<T> lookup, Map<String, Set<String>> registries) {
        Set<String> ids = new TreeSet<>();
        lookup.listElementIds().forEach(key -> {
            if (ours(key.identifier())) ids.add(key.identifier().toString());
        });
        if (!ids.isEmpty()) registries.computeIfAbsent(name, k -> new TreeSet<>()).addAll(ids);
    }

    private static <T> void collect(String name, Registry<T> registry, Map<String, Set<String>> registries,
                                    Map<String, Map<String, Set<String>>> tags) {
        Set<String> ids = new TreeSet<>();
        for (Identifier id : registry.keySet()) {
            if (ours(id)) ids.add(id.toString());
        }
        if (!ids.isEmpty()) registries.computeIfAbsent(name, k -> new TreeSet<>()).addAll(ids);
        registry.getTags().forEach(named -> {
            Identifier tag = named.key().location();
            Set<String> members = new TreeSet<>();
            named.forEach(holder -> holder.unwrapKey().ifPresent(key -> {
                if (ours(tag) || ours(key.identifier())) members.add(key.identifier().toString());
            }));
            if (!members.isEmpty() || ours(tag)) {
                tags.computeIfAbsent(name, k -> new TreeMap<>()).put(tag.toString(), members);
            }
        });
    }
}
