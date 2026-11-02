package net.kasax.raft.world.gen;

import net.kasax.raft.Raft;
import net.kasax.raft.block.ModBlocks;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.BlockTags;
import net.minecraft.core.Holder;
import net.minecraft.util.valueproviders.ConstantInt;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.feature.BlockReplacement;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.OreFeature;
import net.minecraft.world.level.levelgen.feature.TreeFeature;
import net.minecraft.world.level.levelgen.feature.featuresize.TwoLayersFeatureSize;
import net.minecraft.world.level.levelgen.feature.foliageplacers.AcaciaFoliagePlacer;
import net.minecraft.world.level.levelgen.feature.foliageplacers.BlobFoliagePlacer;
import net.minecraft.world.level.levelgen.feature.stateproviders.BlockStateProvider;
import net.minecraft.world.level.levelgen.feature.trunkplacers.BendingTrunkPlacer;
import net.minecraft.world.level.levelgen.feature.trunkplacers.StraightTrunkPlacer;
import net.minecraft.world.level.levelgen.structure.templatesystem.RuleTest;
import net.minecraft.world.level.levelgen.structure.templatesystem.TagMatchTest;

import java.util.List;

/** The configured features (called just "features" in 26.x) of the mod: titanium ore and the two trees. */
public class ModConfiguredFeatures {
    public static final ResourceKey<Feature> TITANIUM_ORE_KEY = registerKey("titanium_ore");

    public static final ResourceKey<Feature> DRIFTWOOD_KEY = registerKey("driftwood");
    public static final ResourceKey<Feature> PALMWOOD_KEY = registerKey("palmwood");

    public static void bootstrap(BootstrapContext<Feature> context) {
        RuleTest stoneReplaceables = new TagMatchTest(BlockTags.STONE_ORE_REPLACEABLES);
        RuleTest deepslateReplaceables = new TagMatchTest(BlockTags.DEEPSLATE_ORE_REPLACEABLES);

        List<BlockReplacement> overworldTitaniumOres = List.of(
                BlockReplacement.replace(stoneReplaceables, ModBlocks.TITANIUM_ORE.defaultBlockState()),
                BlockReplacement.replace(deepslateReplaceables, ModBlocks.DEEPSLATE_TITANIUM_ORE.defaultBlockState()));

        context.register(TITANIUM_ORE_KEY, new OreFeature(overworldTitaniumOres, 8));

        // Trees grow on sand; the block below the trunk stays sand.
        Holder<BlockStateProvider> sand = BlockStateProvider.holderOf(Blocks.SAND);

        context.register(DRIFTWOOD_KEY, new TreeFeature.Builder(
                BlockStateProvider.of(ModBlocks.DRIFTWOOD_LOG),
                new StraightTrunkPlacer(1, 1, 1),
                BlockStateProvider.of(ModBlocks.DRIFTWOOD_LEAVES),
                new BlobFoliagePlacer(ConstantInt.of(0), ConstantInt.of(0), 0),
                new TwoLayersFeatureSize(1, 0, 2),
                sand).build());

        context.register(PALMWOOD_KEY, new TreeFeature.Builder(
                BlockStateProvider.of(ModBlocks.PALM_LOG),
                new BendingTrunkPlacer(8, 3, 0, 10, ConstantInt.of(1)),
                new RandomizedBlockStateProvider(
                        BlockStateProvider.of(ModBlocks.PALM_LEAVES),
                        BlockStateProvider.of(ModBlocks.PALM_COCONUT_LEAVES),
                        0.05f // 5% chance to select PALM_COCONUT_LEAVES
                ),
                new AcaciaFoliagePlacer(ConstantInt.of(3), ConstantInt.of(1)),
                new TwoLayersFeatureSize(1, 0, 2),
                sand).build());
    }

    public static ResourceKey<Feature> registerKey(String name) {
        return ResourceKey.create(Registries.FEATURE, Raft.id(name));
    }
}
