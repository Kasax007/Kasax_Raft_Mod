package net.kasax.raft.content.cozy.block;

import net.kasax.raft.content.decor.entity.SeatEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
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
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.SimpleWaterloggedBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

import java.util.IdentityHashMap;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.Function;

/**
 * The configurable block of the cozy module (Beach Craft 3.3). Depending on its {@link Config} it has a horizontal
 * {@code facing}, can be waterlogged, hangs on a wall or from the ceiling (or both: lanterns), is two blocks tall,
 * can be sat on, is a light that is switched on and off with a right-click ({@code lit}) or something that opens and
 * closes ({@code open}: blinds, baskets). Shapes come from {@link CozyShapes} (front facing north), named
 * {@code <shape>[_hanging][_open][_lower|_upper]}.
 */
public class CozyBlock extends Block implements SimpleWaterloggedBlock {
    public static final EnumProperty<Direction> FACING = BlockStateProperties.HORIZONTAL_FACING;
    public static final BooleanProperty WATERLOGGED = BlockStateProperties.WATERLOGGED;
    public static final BooleanProperty LIT = BlockStateProperties.LIT;
    public static final BooleanProperty HANGING = BlockStateProperties.HANGING;
    public static final BooleanProperty OPEN = BlockStateProperties.OPEN;
    public static final EnumProperty<DoubleBlockHalf> HALF = BlockStateProperties.DOUBLE_BLOCK_HALF;

    public enum Mount { FREE, WALL, CEILING, FLOOR_OR_CEILING }

    /** Fluent configuration; every setter returns a copy so shared bases can be reused. */
    public static final class Config {
        String shape;
        boolean facing, water, collision = true, tall, openable;
        Mount mount = Mount.FREE;
        double seat;
        int light;
        double[] flame;   // x, y, z of a flame particle (pixels) while lit

        private Config(String shape) {
            this.shape = shape;
        }

        public static Config of(String shape) {
            return new Config(shape);
        }

        private Config copy() {
            Config c = new Config(shape);
            c.facing = facing;
            c.water = water;
            c.collision = collision;
            c.tall = tall;
            c.openable = openable;
            c.mount = mount;
            c.seat = seat;
            c.light = light;
            c.flame = flame;
            return c;
        }

        public Config facing() {
            Config c = copy();
            c.facing = true;
            return c;
        }

        public Config water() {
            Config c = copy();
            c.water = true;
            return c;
        }

        public Config wall() {
            Config c = copy();
            c.facing = true;
            c.mount = Mount.WALL;
            return c;
        }

        public Config hangable() {
            Config c = copy();
            c.mount = Mount.FLOOR_OR_CEILING;
            return c;
        }

        public Config noCollision() {
            Config c = copy();
            c.collision = false;
            return c;
        }

        public Config tall() {
            Config c = copy();
            c.tall = true;
            return c;
        }

        public Config openable() {
            Config c = copy();
            c.openable = true;
            return c;
        }

        public Config seat(double height) {
            Config c = copy();
            c.seat = height;
            return c;
        }

        /** A light that is switched on and off with a right-click; placed lit. */
        public Config light(int level) {
            Config c = copy();
            c.light = level;
            return c;
        }

        public Config flame(double x, double y, double z) {
            Config c = copy();
            c.flame = new double[]{x, y, z};
            return c;
        }

        public int lightLevel() {
            return light;
        }
    }

    private static final ThreadLocal<Config> PENDING = new ThreadLocal<>();

    protected final Config config;
    private final Map<BlockState, VoxelShape> shapes = new IdentityHashMap<>();

    public static Function<Properties, CozyBlock> factory(Config config) {
        return create(config, CozyBlock::new);
    }

    /** Wraps a constructor so the state definition can read the config while the block is built. */
    public static <T extends CozyBlock> Function<Properties, T> create(Config config, BiFunction<Properties, Config, T> ctor) {
        return properties -> {
            PENDING.set(config);
            try {
                return ctor.apply(properties, config);
            } finally {
                PENDING.remove();
            }
        };
    }

    /** Light level function for the block properties: {@code lit} lights shine only when switched on. */
    public static java.util.function.ToIntFunction<BlockState> lightFn(int level) {
        return s -> !s.hasProperty(LIT) || s.getValue(LIT) ? level : 0;
    }

    protected CozyBlock(Properties properties, Config config) {
        super(properties);
        this.config = config;
        BlockState state = stateDefinition.any();
        if (config.facing) state = state.setValue(FACING, Direction.NORTH);
        if (config.water) state = state.setValue(WATERLOGGED, false);
        if (config.light > 0) state = state.setValue(LIT, true);
        if (config.mount == Mount.FLOOR_OR_CEILING) state = state.setValue(HANGING, false);
        if (config.openable) state = state.setValue(OPEN, false);
        if (config.tall) state = state.setValue(HALF, DoubleBlockHalf.LOWER);
        registerDefaultState(defaultState(state));
        for (BlockState s : stateDefinition.getPossibleStates()) {
            VoxelShape north = CozyShapes.shape(shapeName(s));
            Direction facing = config.facing ? s.getValue(FACING) : Direction.NORTH;
            shapes.put(s, facing == Direction.NORTH ? north : net.kasax.raft.compat.VShapes.rotateHorizontal(north).get(facing));
        }
    }

    protected BlockState defaultState(BlockState state) {
        return state;
    }

    protected Config config() {
        return config != null ? config : PENDING.get();
    }

    /** The shape name of a state (front facing north). */
    protected String shapeName(BlockState state) {
        Config c = config();
        String name = c.shape;
        if (c.mount == Mount.FLOOR_OR_CEILING && state.getValue(HANGING)) name += "_hanging";
        if (c.openable && state.getValue(OPEN)) name += "_open";
        if (c.tall) name += state.getValue(HALF) == DoubleBlockHalf.LOWER ? "_lower" : "_upper";
        return name;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        Config c = config();
        if (c.facing) builder.add(FACING);
        if (c.water) builder.add(WATERLOGGED);
        if (c.light > 0) builder.add(LIT);
        if (c.mount == Mount.FLOOR_OR_CEILING) builder.add(HANGING);
        if (c.openable) builder.add(OPEN);
        if (c.tall) builder.add(HALF);
    }

    public Config cozyConfig() {
        return config;
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        VoxelShape s = shapes.get(state);
        return s != null ? s : Shapes.block();
    }

    @Override
    protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return config.collision ? getShape(state, level, pos, context) : Shapes.empty();
    }

    // ------------------------------------------------------------------------------------------------ placement
    @Override
    public @Nullable BlockState getStateForPlacement(BlockPlaceContext context) {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        BlockState state = defaultBlockState();
        if (config.water) state = state.setValue(WATERLOGGED, level.getFluidState(pos).is(Fluids.WATER));
        if (config.tall && (pos.getY() >= (level.getMaxBuildHeight() - 1) || !level.getBlockState(pos.above()).canBeReplaced(context))) {
            return null;
        }
        if (config.mount == Mount.WALL) {
            Direction clicked = context.getClickedFace();
            if (clicked.getAxis().isHorizontal()) {
                BlockState s = state.setValue(FACING, clicked);
                if (s.canSurvive(level, pos)) return s;
            }
            for (Direction direction : context.getNearestLookingDirections()) {
                if (direction.getAxis().isHorizontal()) {
                    BlockState s = state.setValue(FACING, direction.getOpposite());
                    if (s.canSurvive(level, pos)) return s;
                }
            }
            return null;
        }
        if (config.facing) state = state.setValue(FACING, context.getHorizontalDirection().getOpposite());
        if (config.mount == Mount.FLOOR_OR_CEILING) {
            boolean hang = context.getClickedFace() == Direction.DOWN;
            BlockState s = state.setValue(HANGING, hang);
            if (s.canSurvive(level, pos)) return s;
            s = state.setValue(HANGING, !hang);
            return s.canSurvive(level, pos) ? s : null;
        }
        return state.canSurvive(level, pos) ? state : null;
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity by, ItemStack itemStack) {
        if (config.tall) {
            BlockState upper = state.setValue(HALF, DoubleBlockHalf.UPPER);
            if (config.water) upper = upper.setValue(WATERLOGGED, level.getFluidState(pos.above()).is(Fluids.WATER));
            level.setBlock(pos.above(), upper, Block.UPDATE_ALL);
        }
    }

    /** The neighbour that holds this block up, or null when it stands on its own. */
    protected @Nullable Direction supportSide(BlockState state) {
        if (config.tall && state.getValue(HALF) == DoubleBlockHalf.UPPER) return Direction.DOWN;
        return switch (config.mount) {
            case WALL -> state.getValue(FACING).getOpposite();
            case CEILING -> Direction.UP;
            case FLOOR_OR_CEILING -> state.getValue(HANGING) ? Direction.UP : Direction.DOWN;
            default -> null;
        };
    }

    @Override
    protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        if (config.tall && state.getValue(HALF) == DoubleBlockHalf.UPPER) {
            BlockState below = level.getBlockState(pos.below());
            return below.is(this) && below.getValue(HALF) == DoubleBlockHalf.LOWER;
        }
        Direction side = supportSide(state);
        if (side == null) return true;
        BlockPos support = pos.relative(side);
        BlockState supportState = level.getBlockState(support);
        if (config.mount == Mount.FLOOR_OR_CEILING && side == Direction.DOWN) {
            return Block.canSupportCenter(level, support, Direction.UP) || !supportState.getCollisionShape(level, support).isEmpty();
        }
        return supportState.isFaceSturdy(level, support, side.getOpposite())
                || !supportState.getCollisionShape(level, support).isEmpty()
                || supportState.getBlock() instanceof CozyBlock;
    }

    @Override
    protected BlockState updateShape(BlockState state, Direction directionToNeighbour, BlockState neighbourState, net.minecraft.world.level.LevelAccessor level, BlockPos pos, BlockPos neighbourPos) {
        if (config.water && state.getValue(WATERLOGGED)) {
            level.scheduleTick(pos, Fluids.WATER, Fluids.WATER.getTickDelay(level));
        }
        if (config.tall) {
            DoubleBlockHalf half = state.getValue(HALF);
            if (directionToNeighbour.getAxis() == Direction.Axis.Y && (half == DoubleBlockHalf.LOWER) == (directionToNeighbour == Direction.UP)) {
                if (!(neighbourState.is(this) && neighbourState.getValue(HALF) != half)) return empty(state);
            }
        }
        if (directionToNeighbour == supportSide(state) && !state.canSurvive(level, pos)) {
            return empty(state);
        }
        return super.updateShape(state, directionToNeighbour, neighbourState, level, pos, neighbourPos);
    }

    private BlockState empty(BlockState state) {
        return config.water && state.getValue(WATERLOGGED) ? Blocks.WATER.defaultBlockState() : Blocks.AIR.defaultBlockState();
    }

    @Override
    public BlockState playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
        if (config.tall && !level.isClientSide && player.isCreative() && state.getValue(HALF) == DoubleBlockHalf.UPPER) {
            BlockPos below = pos.below();
            BlockState belowState = level.getBlockState(below);
            if (belowState.is(this) && belowState.getValue(HALF) == DoubleBlockHalf.LOWER) {
                level.setBlock(below, empty(belowState), Block.UPDATE_ALL | Block.UPDATE_SUPPRESS_DROPS);
                level.levelEvent(player, 2001, below, Block.getId(belowState));
            }
        }
        return super.playerWillDestroy(level, pos, state, player);
    }

    // ------------------------------------------------------------------------------------------------ water
    @Override
    protected FluidState getFluidState(BlockState state) {
        return config.water && state.getValue(WATERLOGGED) ? Fluids.WATER.getSource(false) : super.getFluidState(state);
    }

    @Override
    public boolean canPlaceLiquid(@org.jetbrains.annotations.Nullable net.minecraft.world.entity.player.Player user, BlockGetter level, BlockPos pos, BlockState state, Fluid fluid) {
        return config.water && SimpleWaterloggedBlock.super.canPlaceLiquid(user, level, pos, state, fluid);
    }

    @Override
    public boolean placeLiquid(LevelAccessor level, BlockPos pos, BlockState state, FluidState fluidState) {
        return config.water && SimpleWaterloggedBlock.super.placeLiquid(level, pos, state, fluidState);
    }

    @Override
    public ItemStack pickupBlock(@org.jetbrains.annotations.Nullable net.minecraft.world.entity.player.Player user, LevelAccessor level, BlockPos pos, BlockState state) {
        return config.water ? SimpleWaterloggedBlock.super.pickupBlock(user, level, pos, state) : ItemStack.EMPTY;
    }

    @Override
    protected boolean propagatesSkylightDown(BlockState state, net.minecraft.world.level.BlockGetter level121, BlockPos pos121) {
        return !(config.water && state.getValue(WATERLOGGED));
    }

    @Override
    protected boolean isPathfindable(BlockState state, PathComputationType type) {
        return false;
    }

    // ------------------------------------------------------------------------------------------------ use
    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (config.seat > 0) {
            return SeatEntity.sit(level, pos, player, config.seat) ? InteractionResult.SUCCESS : InteractionResult.PASS;
        }
        if (config.light > 0) {
            boolean on = !state.getValue(LIT);
            level.setBlock(pos, state.setValue(LIT, on), Block.UPDATE_ALL);
            playToggle(level, pos, on ? SoundEvents.COPPER_BULB_TURN_ON : SoundEvents.COPPER_BULB_TURN_OFF, on ? 1.2F : 1.0F);
            return InteractionResult.SUCCESS;
        }
        if (config.openable) {
            boolean open = !state.getValue(OPEN);
            level.setBlock(pos, state.setValue(OPEN, open), Block.UPDATE_ALL);
            playToggle(level, pos, open ? SoundEvents.BAMBOO_WOOD_TRAPDOOR_OPEN : SoundEvents.BAMBOO_WOOD_TRAPDOOR_CLOSE, 1.0F);
            return InteractionResult.SUCCESS;
        }
        return super.useWithoutItem(state, level, pos, player, hit);
    }

    protected static void playToggle(Level level, BlockPos pos, SoundEvent sound, float pitch) {
        level.playSound(null, pos, sound, SoundSource.BLOCKS, 0.6F, pitch);
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (config.flame == null || config.light <= 0 || !state.getValue(LIT)) return;
        if (config.water && state.getValue(WATERLOGGED)) return;
        double x = config.flame[0] / 16.0 - 0.5, z = config.flame[2] / 16.0 - 0.5;
        if (config.facing) {
            // rotate the flame position with the block (models face north)
            Direction f = state.getValue(FACING);
            for (int i = 0; i < (f.get2DDataValue() + 2) % 4; i++) {
                double t = x;
                x = -z;
                z = t;
            }
        }
        double px = pos.getX() + 0.5 + x, py = pos.getY() + config.flame[1] / 16.0, pz = pos.getZ() + 0.5 + z;
        if (random.nextInt(3) == 0) level.addParticle(ParticleTypes.SMALL_FLAME, px, py, pz, 0, 0, 0);
        if (random.nextInt(8) == 0) level.addParticle(ParticleTypes.SMOKE, px, py + 0.05, pz, 0, 0.01, 0);
    }

    @Override
    protected BlockState rotate(BlockState state, Rotation rotation) {
        return config.facing ? state.setValue(FACING, rotation.rotate(state.getValue(FACING))) : state;
    }

    @Override
    protected BlockState mirror(BlockState state, Mirror mirror) {
        return config.facing ? state.rotate(mirror.getRotation(state.getValue(FACING))) : state;
    }

    @Override
    protected long getSeed(BlockState state, BlockPos pos) {
        if (!config.tall) return super.getSeed(state, pos);
        return net.minecraft.util.Mth.getSeed(pos.getX(), pos.below(state.getValue(HALF) == DoubleBlockHalf.LOWER ? 0 : 1).getY(), pos.getZ());
    }

    /** Server helper for subclasses. */
    protected static boolean server(Level level) {
        return level instanceof ServerLevel;
    }
}
