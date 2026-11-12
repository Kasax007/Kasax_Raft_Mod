package net.kasax.raft.platform;

import com.mojang.serialization.MapCodec;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Predicate;
import net.minecraft.core.Holder;
import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;
import net.neoforged.neoforge.common.world.BiomeModifier;
import net.neoforged.neoforge.common.world.ModifiableBiomeInfo;
import net.neoforged.neoforge.registries.NeoForgeRegistries;
import net.neoforged.neoforge.registries.RegisterEvent;

/**
 * Fabric's BiomeModifications on one NeoForge biome modifier: the modifications are recorded in code, the data file
 * {@code data/raft/neoforge/biome_modifier/biome_modifications.json} (type {@code raft:biome_modifications}) makes
 * NeoForge run them for every biome when a server starts.
 */
public final class BiomeModifications {
    private static final List<Modification> MODIFICATIONS = new ArrayList<>();
    public static final MapCodec<CodeModifier> CODEC = MapCodec.unit(CodeModifier.INSTANCE);

    private BiomeModifications() {
    }

    public static void addFeature(Predicate<BiomeSelectionContext> selector, GenerationStep.Decoration step, ResourceKey<PlacedFeature> placedFeatureKey) {
        create(placedFeatureKey.identifier()).add(ModificationPhase.ADDITIONS, selector,
                context -> context.getGenerationSettings().addFeature(step, placedFeatureKey));
    }

    public static BiomeModification create(Identifier id) {
        return new BiomeModification(id);
    }

    static void onRegister(RegisterEvent event) {
        event.register(NeoForgeRegistries.Keys.BIOME_MODIFIER_SERIALIZERS, net.kasax.raft.Raft.id("biome_modifications"), () -> CODEC);
    }

    public static final class BiomeModification {
        private final Identifier id;

        BiomeModification(Identifier id) {
            this.id = id;
        }

        public BiomeModification add(ModificationPhase phase, Predicate<BiomeSelectionContext> selector, java.util.function.Consumer<BiomeModificationContext> modifier) {
            return add(phase, selector, (selection, context) -> modifier.accept(context));
        }

        public BiomeModification add(ModificationPhase phase, Predicate<BiomeSelectionContext> selector, BiConsumer<BiomeSelectionContext, BiomeModificationContext> modifier) {
            synchronized (MODIFICATIONS) {
                MODIFICATIONS.add(new Modification(id, phase, selector, modifier, MODIFICATIONS.size()));
                sorted = null;
            }
            return this;
        }
    }

    /** Fabric's order: phase, then modification id, then registration order. */
    private record Modification(Identifier id, ModificationPhase phase, Predicate<BiomeSelectionContext> selector,
                                BiConsumer<BiomeSelectionContext, BiomeModificationContext> modifier, int order) {
    }

    private static List<Modification> sorted;

    private static List<Modification> sorted() {
        synchronized (MODIFICATIONS) {
            if (sorted == null) {
                List<Modification> list = new ArrayList<>(MODIFICATIONS);
                list.sort(java.util.Comparator.comparing(Modification::phase).thenComparing(Modification::id).thenComparingInt(Modification::order));
                sorted = List.copyOf(list);
            }
            return sorted;
        }
    }

    /** The NeoForge biome modifier that runs the recorded modifications. */
    public static final class CodeModifier implements BiomeModifier {
        static final CodeModifier INSTANCE = new CodeModifier();

        @Override
        public void modify(RegistryAccess registries, Holder<Biome> biome, Phase phase, ModifiableBiomeInfo.BiomeInfo.Builder builder) {
            if (biome.unwrapKey().isEmpty()) {
                return;
            }
            BiomeSelectionContext selection = new BiomeSelectionContext(biome);
            BiomeModificationContext context = new BiomeModificationContext(builder, registries);
            for (Modification m : sorted()) {
                if (m.phase.neo == phase && m.selector.test(selection)) {
                    m.modifier.accept(selection, context);
                }
            }
        }

        @Override
        public MapCodec<? extends BiomeModifier> codec() {
            return CODEC;
        }
    }
}
