package net.kasax.raft.world.tree;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.kasax.raft.Raft;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.intprovider.IntProvider;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.TestableWorld;
import net.minecraft.world.gen.feature.TreeFeatureConfig;
import net.minecraft.world.gen.foliage.FoliagePlacer;
import net.minecraft.world.gen.foliage.FoliagePlacerType;

public class StarFoliagePlacer extends FoliagePlacer {

    private final int initialRadius;
    private final int maxLength;
    public StarFoliagePlacer(IntProvider radius, IntProvider offset, int initialRadius, int maxLength) {
        super(radius, offset);
        this.initialRadius = initialRadius;
        this.maxLength = maxLength;
    }

    public static final MapCodec<StarFoliagePlacer> CODEC = RecordCodecBuilder.mapCodec(instance ->
            instance.group(
                    IntProvider.VALUE_CODEC.fieldOf("radius").forGetter(placer -> placer.radius),
                    IntProvider.VALUE_CODEC.fieldOf("offset").forGetter(placer -> placer.offset),
                    Codec.INT.fieldOf("initialRadius").forGetter(placer -> placer.initialRadius),
                    Codec.INT.fieldOf("maxLength").forGetter(placer -> placer.maxLength)
            ).apply(instance, StarFoliagePlacer::new)
    );

    @Override
    protected FoliagePlacerType<?> getType() {
        return Raft.STAR_FOLIAGE_PLACER;
    }

    @Override
    protected void generate(TestableWorld world, BlockPlacer placer, Random random, TreeFeatureConfig config, int trunkHeight, TreeNode treeNode, int foliageHeight, int radius, int offset) {
        BlockPos pos = treeNode.getCenter().up();

        // First layer (3x3)
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                placeFoliageBlock(world, placer, random, config, pos.add(dx, 0, dz));
            }
        }

        // Second layer (5x5 star pattern)
        int[][] starPattern = {
                {0, 1}, {0, 2}, {0, -1}, {0, -2},
                {1, 0}, {2, 0}, {-1, 0}, {-2, 0},
                {1, 1}, {-1, 1}, {1, -1}, {-1, -1}
        };

        for (int[] offsetPos : starPattern) {
            placeFoliageBlock(world, placer, random, config, pos.add(offsetPos[0], 1, offsetPos[1]));
        }
    }


    @Override
    public int getRandomHeight(Random random, int trunkHeight, TreeFeatureConfig config) {
        // Palm trees typically have a flat foliage, so a constant height might be sufficient
        return 2;
    }

    @Override
    protected boolean isInvalidForLeaves(Random random, int dx, int y, int dz, int radius, boolean giantTrunk) {
        return false;
    }
}
