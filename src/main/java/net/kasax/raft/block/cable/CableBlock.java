package net.kasax.raft.block.cable;

/*
 * This file is inspired by TechReborn, licensed under the MIT License (MIT).
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

import net.kasax.raft.Raft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.RandomSource;
import net.minecraft.util.Util;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SimpleWaterloggedBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.redstone.Orientation;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by modmuss50 on 19/05/2017.
 */
public class CableBlock extends BaseEntityBlock implements SimpleWaterloggedBlock {

    public static final BooleanProperty EAST = BooleanProperty.create("east");
    public static final BooleanProperty WEST = BooleanProperty.create("west");
    public static final BooleanProperty NORTH = BooleanProperty.create("north");
    public static final BooleanProperty SOUTH = BooleanProperty.create("south");
    public static final BooleanProperty UP = BooleanProperty.create("up");
    public static final BooleanProperty DOWN = BooleanProperty.create("down");
    public static final BooleanProperty WATERLOGGED = BlockStateProperties.WATERLOGGED;
    public static final BooleanProperty COVERED = BooleanProperty.create("covered");

    public static final Map<Direction, BooleanProperty> PROPERTY_MAP = Util.make(new HashMap<>(), map -> {
        map.put(Direction.EAST, EAST);
        map.put(Direction.WEST, WEST);
        map.put(Direction.NORTH, NORTH);
        map.put(Direction.SOUTH, SOUTH);
        map.put(Direction.UP, UP);
        map.put(Direction.DOWN, DOWN);
    });

    public final RaftCable.Cables type;

    public CableBlock(RaftCable.Cables type) {
        super(BlockBehaviour.Properties.of()
                .setId(ResourceKey.create(Registries.BLOCK, Raft.id(type.name)))
                .sound(SoundType.METAL)
                .mapColor(MapColor.METAL)
                .noOcclusion()
                .strength(1f, 8f).requiresCorrectToolForDrops());
        this.type = type;
        registerDefaultState(this.stateDefinition.any().setValue(EAST, false).setValue(WEST, false).setValue(NORTH, false)
                .setValue(SOUTH, false).setValue(UP, false).setValue(DOWN, false).setValue(WATERLOGGED, false).setValue(COVERED, false));
    }

    // BaseEntityBlock
    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new CableBlockEntity(pos, state, type);
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (level.isClientSide()) {
            return null;
        }
        return (lvl, pos, st, blockEntity) -> {
            if (blockEntity instanceof CableBlockEntity cable) {
                cable.tick(lvl, pos, st, cable);
            }
        };
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(EAST, WEST, NORTH, SOUTH, UP, DOWN, WATERLOGGED, COVERED);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState()
                .setValue(WATERLOGGED, context.getLevel().getFluidState(context.getClickedPos()).getType() == Fluids.WATER);
    }

    @Override
    protected BlockState updateShape(BlockState ourState, LevelReader level, ScheduledTickAccess ticks, BlockPos ourPos,
                                     Direction directionToNeighbour, BlockPos otherPos, BlockState otherState, RandomSource random) {
        if (ourState.getValue(WATERLOGGED)) {
            ticks.scheduleTick(ourPos, Fluids.WATER, Fluids.WATER.getTickDelay(level));
        }
        return ourState;
    }

    @Override
    protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block block, @Nullable Orientation orientation, boolean movedByPiston) {
        if (level.getBlockEntity(pos) instanceof CableBlockEntity cable) {
            cable.neighborUpdate();
        }
        super.neighborChanged(state, level, pos, block, orientation, movedByPiston);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return CableShapeUtil.getShape(state);
    }

    @Override
    public boolean placeLiquid(LevelAccessor level, BlockPos pos, BlockState state, FluidState fluidState) {
        return !state.getValue(COVERED) && SimpleWaterloggedBlock.super.placeLiquid(level, pos, state, fluidState);
    }

    @Override
    public boolean canPlaceLiquid(@Nullable LivingEntity user, BlockGetter level, BlockPos pos, BlockState state, Fluid fluid) {
        return !state.getValue(COVERED) && SimpleWaterloggedBlock.super.canPlaceLiquid(user, level, pos, state, fluid);
    }

    @Override
    protected FluidState getFluidState(BlockState state) {
        return state.getValue(WATERLOGGED) ? Fluids.WATER.getSource(false) : super.getFluidState(state);
    }
}
