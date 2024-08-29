package net.kasax.raft.world.tree;

import com.mojang.serialization.Codec;
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

    public static final Codec<StarFoliagePlacer> CODEC = RecordCodecBuilder.create(instance ->
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
        BlockPos pos = treeNode.getCenter().up();  // Start placing leaves just above the top of the trunk

        double coneAngle = Math.toRadians(45); // 45 degrees downward

        // Create a small point at the top of the tree
        //placeLeavesRow(world, placer, random, config, pos.up(), 1); // A small 3x3 leaf area above the trunk
        placeFoliageBlock(world, placer, random, config, pos.up(1)); // A single leaf block at the very top

        // Create 4 cone-shaped leaves around the trunk
        for (int i = 0; i < 4; i++) {
            double horizontalAngle = (2 * Math.PI / 4) * i; // 90 degrees apart for each cone

            BlockPos lastPos = pos; // Track the last position for the tip

            for (int distance = 0; distance <= maxLength; distance++) {
                // Calculate the current position along the cone's direction
                int dx = (int) (distance * Math.cos(horizontalAngle) * Math.cos(coneAngle));
                int dz = (int) (distance * Math.sin(horizontalAngle) * Math.cos(coneAngle));
                int dy = (int) (-distance * Math.sin(coneAngle)); // Downward direction

                BlockPos foliagePos = pos.add(dx, dy, dz);

                // Calculate the current radius at this distance from the start
                int currentRadius = Math.max(initialRadius - (int)(Math.pow(distance, 1.5) * initialRadius / (maxLength * maxLength)), 0);

                // Place leaves at the calculated position
                placeLeavesRow(world, placer, random, config, foliagePos, currentRadius);

                // Occasionally place a log block along the cone to maintain leaf connection
                if (distance % 3 == 0 || distance == maxLength) {
                    placeLogBlock(world, placer, random, config, foliagePos);
                }

                // Update last position for the tip
                lastPos = foliagePos;
            }
            // Place a leaf block at the tip of the cone
            placeFoliageBlock(world, placer, random, config, lastPos);
            // Place a leaf block at the tip of the cone
            placeFoliageBlock(world, placer, random, config, lastPos.down());
        }
    }

    private void placeLogBlock(TestableWorld world, BlockPlacer placer, Random random, TreeFeatureConfig config, BlockPos pos) {
        placer.placeBlock(pos, config.trunkProvider.get(random, pos));
    }

    private void placeLeavesRow(TestableWorld world, BlockPlacer placer, Random random, TreeFeatureConfig config, BlockPos pos, int radius) {
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                int i = Math.abs(dx) + Math.abs(dz);
                if (i <= radius) {
                    BlockPos foliagePos = pos.add(dx, 0, dz);
                    placeFoliageBlock(world, placer, random, config, foliagePos);

                    // Add two additional leaf blocks below the middle of the pattern
                    if (i == radius) {
                        BlockPos belowPos = foliagePos.down();
                        placeFoliageBlock(world, placer, random, config, belowPos);
                    }
                }
            }
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
