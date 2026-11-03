package net.kasax.raft.content.decor.block;

import net.kasax.raft.content.decor.entity.SeatEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.SimpleWaterloggedBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;

import java.util.EnumMap;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.Function;

/**
 * The configurable decoration block of the decor module: optional horizontal {@code facing}, optional
 * {@code waterlogged}, how it is mounted (free, on a wall behind it, hanging from the block above), the shape of its
 * model (from {@link DecorShapeData}, front facing north) and an optional seat height (players sit on it with a
 * right-click). Subclasses add behaviour (bell, wind chime).
 */
public class DecorBlock extends Block implements SimpleWaterloggedBlock {
    public static final EnumProperty<Direction> FACING = BlockStateProperties.HORIZONTAL_FACING;
    public static final BooleanProperty WATERLOGGED = BlockStateProperties.WATERLOGGED;

    public enum Mount { FREE, WALL, CEILING }

    /**
     * @param facing    has a horizontal facing (the front points to it; wall blocks hang on the block behind)
     * @param water     can be waterlogged
     * @param mount     what it needs to stay in place
     * @param shape     name of the shape in {@link DecorShapeData}
     * @param collision false: no collision (garlands, posters)
     * @param seat      seat height in blocks (0 = not sittable)
     */
    public record Config(boolean hasFacing, boolean hasWater, Mount mount, String shape, boolean collision, double seat) {
        public static Config of(String shape) {
            return new Config(false, false, Mount.FREE, shape, true, 0);
        }

        public Config facing() {
            return new Config(true, hasWater, mount, shape, collision, seat);
        }

        public Config water() {
            return new Config(hasFacing, true, mount, shape, collision, seat);
        }

        public Config wall() {
            return new Config(true, hasWater, Mount.WALL, shape, collision, seat);
        }

        public Config ceiling() {
            return new Config(hasFacing, hasWater, Mount.CEILING, shape, collision, seat);
        }

        public Config noCollision() {
            return new Config(hasFacing, hasWater, mount, shape, false, seat);
        }

        public Config seat(double height) {
            return new Config(hasFacing, hasWater, mount, shape, collision, height);
        }
    }

    private static final ThreadLocal<Config> PENDING = new ThreadLocal<>();

    protected final Config config;
    private final Map<Direction, VoxelShape> shapes;

    /** A factory for {@code BcReg.block(name, factory, properties)}. */
    public static Function<Properties, DecorBlock> factory(Config config) {
        return create(config, DecorBlock::new);
    }

    /** Wraps a subclass constructor so the state definition can read the config while the block is built. */
    public static <T extends DecorBlock> Function<Properties, T> create(Config config, BiFunction<Properties, Config, T> ctor) {
        return properties -> {
            PENDING.set(config);
            try {
                return ctor.apply(properties, config);
            } finally {
                PENDING.remove();
            }
        };
    }

    protected DecorBlock(Properties properties, Config config) {
        super(properties);
        this.config = config;
        this.shapes = shapes(config.shape(), config.hasFacing());
        BlockState state = stateDefinition.any();
        if (config.hasFacing()) state = state.setValue(FACING, Direction.NORTH);
        if (config.hasWater()) state = state.setValue(WATERLOGGED, false);
        registerDefaultState(defaultState(state));
    }

    /** Lets subclasses set defaults of their extra properties. */
    protected BlockState defaultState(BlockState state) {
        return state;
    }

    protected Config config() {
        return config != null ? config : PENDING.get();
    }

    /** The shape of a model for every horizontal facing (north = as modelled). */
    public static Map<Direction, VoxelShape> shapes(String name, boolean rotate) {
        VoxelShape north = shape(name);
        Map<Direction, VoxelShape> map = new EnumMap<>(Direction.class);
        if (rotate) {
            map.putAll(Shapes.rotateHorizontal(north));
        } else {
            for (Direction d : Direction.Plane.HORIZONTAL) map.put(d, north);
        }
        return map;
    }

    public static VoxelShape shape(String name) {
        VoxelShape shape = Shapes.empty();
        for (double[] b : DecorShapeData.get(name)) {
            shape = Shapes.or(shape, Block.box(b[0], b[1], b[2], b[3], b[4], b[5]));
        }
        return shape.optimize();
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        Config c = config();
        if (c.hasFacing()) builder.add(FACING);
        if (c.hasWater()) builder.add(WATERLOGGED);
    }

    protected Direction facingOf(BlockState state) {
        return config.hasFacing() ? state.getValue(FACING) : Direction.NORTH;
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return shapes.get(facingOf(state));
    }

    @Override
    protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return config.collision() ? getShape(state, level, pos, context) : Shapes.empty();
    }

    @Override
    public @Nullable BlockState getStateForPlacement(BlockPlaceContext context) {
        BlockState state = defaultBlockState();
        if (config.hasWater()) {
            state = state.setValue(WATERLOGGED, context.getLevel().getFluidState(context.getClickedPos()).is(Fluids.WATER));
        }
        if (config.mount() == Mount.WALL) {
            Direction clicked = context.getClickedFace();
            if (clicked.getAxis().isHorizontal()) {
                BlockState s = state.setValue(FACING, clicked);
                if (s.canSurvive(context.getLevel(), context.getClickedPos())) return s;
            }
            for (Direction direction : context.getNearestLookingDirections()) {
                if (direction.getAxis().isHorizontal()) {
                    BlockState s = state.setValue(FACING, direction.getOpposite());
                    if (s.canSurvive(context.getLevel(), context.getClickedPos())) return s;
                }
            }
            return null;
        }
        if (config.hasFacing()) {
            state = state.setValue(FACING, context.getHorizontalDirection().getOpposite());
        }
        return state.canSurvive(context.getLevel(), context.getClickedPos()) ? state : null;
    }

    /** The neighbour that holds this block up, or null when it stands on its own. */
    protected @Nullable Direction supportSide(BlockState state) {
        return switch (config.mount()) {
            case WALL -> state.getValue(FACING).getOpposite();
            case CEILING -> Direction.UP;
            default -> null;
        };
    }

    @Override
    protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        Direction side = supportSide(state);
        if (side == null) return true;
        BlockPos support = pos.relative(side);
        BlockState supportState = level.getBlockState(support);
        // any solid neighbour holds it: walls, fences, beams, glass, other decorations
        return supportState.isFaceSturdy(level, support, side.getOpposite())
                || !supportState.getCollisionShape(level, support).isEmpty()
                || supportState.getBlock() instanceof DecorBlock;
    }

    @Override
    protected BlockState updateShape(BlockState state, LevelReader level, ScheduledTickAccess ticks, BlockPos pos,
                                     Direction directionToNeighbour, BlockPos neighbourPos, BlockState neighbourState,
                                     RandomSource random) {
        if (config.hasWater() && state.getValue(WATERLOGGED)) {
            ticks.scheduleTick(pos, Fluids.WATER, Fluids.WATER.getTickDelay(level));
        }
        if (directionToNeighbour == supportSide(state) && !state.canSurvive(level, pos)) {
            return config.hasWater() && state.getValue(WATERLOGGED) ? Blocks.WATER.defaultBlockState() : Blocks.AIR.defaultBlockState();
        }
        return super.updateShape(state, level, ticks, pos, directionToNeighbour, neighbourPos, neighbourState, random);
    }

    @Override
    protected FluidState getFluidState(BlockState state) {
        return config.hasWater() && state.getValue(WATERLOGGED) ? Fluids.WATER.getSource(false) : super.getFluidState(state);
    }

    @Override
    public boolean canPlaceLiquid(@Nullable LivingEntity user, BlockGetter level, BlockPos pos, BlockState state, Fluid fluid) {
        return config.hasWater() && SimpleWaterloggedBlock.super.canPlaceLiquid(user, level, pos, state, fluid);
    }

    @Override
    public boolean placeLiquid(LevelAccessor level, BlockPos pos, BlockState state, FluidState fluidState) {
        return config.hasWater() && SimpleWaterloggedBlock.super.placeLiquid(level, pos, state, fluidState);
    }

    @Override
    public ItemStack pickupBlock(@Nullable LivingEntity user, LevelAccessor level, BlockPos pos, BlockState state) {
        return config.hasWater() ? SimpleWaterloggedBlock.super.pickupBlock(user, level, pos, state) : ItemStack.EMPTY;
    }

    @Override
    protected boolean propagatesSkylightDown(BlockState state) {
        return !(config.hasWater() && state.getValue(WATERLOGGED));
    }

    @Override
    protected boolean isPathfindable(BlockState state, PathComputationType type) {
        return false;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (config.seat() > 0) {
            return SeatEntity.sit(level, pos, player, config.seat()) ? InteractionResult.SUCCESS : InteractionResult.PASS;
        }
        return super.useWithoutItem(state, level, pos, player, hit);
    }

    @Override
    protected BlockState rotate(BlockState state, Rotation rotation) {
        return config.hasFacing() ? state.setValue(FACING, rotation.rotate(state.getValue(FACING))) : state;
    }

    @Override
    protected BlockState mirror(BlockState state, Mirror mirror) {
        return config.hasFacing() ? state.rotate(mirror.getRotation(state.getValue(FACING))) : state;
    }
}
