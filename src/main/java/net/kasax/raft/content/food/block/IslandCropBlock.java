package net.kasax.raft.content.food.block;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.function.Supplier;

/**
 * An island crop with four stages (sweet potatoes, chili, banana, rice): planted on farmland or a crop plot, grows
 * at about the speed of wheat (every second random tick is skipped, as for beetroots).
 */
public class IslandCropBlock extends CropBlock {
    public static final IntegerProperty AGE = BlockStateProperties.AGE_3;
    private final Supplier<ItemLike> seed;
    private final VoxelShape[] shapes = new VoxelShape[4];

    public IslandCropBlock(Properties properties, Supplier<ItemLike> seed, double height) {
        super(properties);
        this.seed = seed;
        for (int i = 0; i < 4; i++) shapes[i] = net.kasax.raft.compat.VShapes.column(14.0, 0.0, Math.max(3, height * (i + 1) / 4.0));
    }

    @Override
    protected IntegerProperty getAgeProperty() {
        return AGE;
    }

    @Override
    public int getMaxAge() {
        return 3;
    }

    @Override
    protected ItemLike getBaseSeedId() {
        return seed.get();
    }

    @Override
    protected void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (random.nextInt(2) != 0) super.randomTick(state, level, pos, random);
    }

    @Override
    protected int getBonemealAgeIncrease(Level level) {
        return Math.max(1, super.getBonemealAgeIncrease(level) / 2);
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(AGE);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return shapes[getAge(state)];
    }
}
