package net.kasax.raft.content.world.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.FaceAttachedHorizontalDirectionalBlock;
import net.minecraft.world.level.block.SimpleWaterloggedBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.AttachFace;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import net.kasax.raft.compat.VShapes;

/** A starfish lying flat on a floor, wall or ceiling (attaches like a button), in or out of water. */
public class StarfishBlock extends FaceAttachedHorizontalDirectionalBlock implements SimpleWaterloggedBlock {
    @Override
    protected com.mojang.serialization.MapCodec<? extends FaceAttachedHorizontalDirectionalBlock> codec() {
        return simpleCodec(StarfishBlock::new);
    }

    public static final BooleanProperty WATERLOGGED = BlockStateProperties.WATERLOGGED;
    private static final Map<AttachFace, Map<Direction, VoxelShape>> SHAPES =
            java.util.Map.of(
                    AttachFace.WALL, VShapes.rotateHorizontal(VShapes.boxZ(14.0, 14.0, 15.0, 16.0)),
                    AttachFace.FLOOR, VShapes.rotateHorizontal(Block.box(1.0, 0.0, 1.0, 15.0, 1.0, 15.0)),
                    AttachFace.CEILING, VShapes.rotateHorizontal(Block.box(1.0, 15.0, 1.0, 15.0, 16.0, 15.0)));

    public StarfishBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FACE, AttachFace.FLOOR).setValue(FACING, Direction.NORTH)
                .setValue(WATERLOGGED, false));
    }

    @Override
    public @Nullable BlockState getStateForPlacement(BlockPlaceContext context) {
        BlockState state = super.getStateForPlacement(context);
        if (state == null) {
            return null;
        }
        FluidState fluid = context.getLevel().getFluidState(context.getClickedPos());
        return state.setValue(WATERLOGGED, fluid.is(FluidTags.WATER) && fluid.isSource());
    }

    @Override
    protected BlockState updateShape(BlockState state, Direction direction, BlockState neighbourState, net.minecraft.world.level.LevelAccessor level, BlockPos pos, BlockPos neighbourPos) {
        if (state.getValue(WATERLOGGED)) {
            level.scheduleTick(pos, Fluids.WATER, Fluids.WATER.getTickDelay(level));
        }
        return super.updateShape(state, direction, neighbourState, level, pos, neighbourPos);
    }

    @Override
    protected FluidState getFluidState(BlockState state) {
        return state.getValue(WATERLOGGED) ? Fluids.WATER.getSource(false) : super.getFluidState(state);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPES.get(state.getValue(FACE)).get(state.getValue(FACING));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACE, FACING, WATERLOGGED);
    }
}
