package net.kasax.raft.block.custom;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.BonemealableBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.FarmlandBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

/**
 * Farmland that is always fully hydrated, can never be trampled and gives the crop planted on it
 * additional chances to grow.
 */
public class CropPlotBlock extends FarmlandBlock {
    /** Chance (1 in N) per random tick that the crop above gets one extra growth tick. */
    public static final int EXTRA_GROWTH_ONE_IN = 3;

    public CropPlotBlock(Properties properties) {
        super(Blocks.DIRT, properties);
        registerDefaultState(stateDefinition.any().setValue(MOISTURE, MAX_MOISTURE));
    }

    @Override
    public @Nullable BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState();
    }

    @Override
    protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        return true;
    }

    @Override
    protected boolean isRandomlyTicking(BlockState state) {
        return true;
    }

    @Override
    protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        // never turns into dirt
    }

    @Override
    protected void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (state.getValue(MOISTURE) < MAX_MOISTURE) {
            level.setBlock(pos, state.setValue(MOISTURE, MAX_MOISTURE), 2);
        }
        BlockPos abovePos = pos.above();
        BlockState above = level.getBlockState(abovePos);
        if (above.isRandomlyTicking() && (above.getBlock() instanceof BonemealableBlock) && random.nextInt(EXTRA_GROWTH_ONE_IN) == 0) {
            above.randomTick(level, abovePos, random);
        }
    }

    /** Trampling is disabled. */
    @Override
    public void turnToBaseBlock(@Nullable Entity sourceEntity, BlockState state, Level level, BlockPos pos) {
    }
}
