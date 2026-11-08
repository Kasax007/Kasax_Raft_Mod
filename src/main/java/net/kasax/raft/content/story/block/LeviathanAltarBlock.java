package net.kasax.raft.content.story.block;

import net.kasax.raft.Raft;
import net.kasax.raft.content.audio.AudioSounds;
import net.kasax.raft.content.story.StoryContent;
import net.kasax.raft.content.story.StoryUtil;
import net.kasax.raft.item.ModItems;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.Optional;

/**
 * The Leviathan altar at the rim of the sanctum's pool. <b>Contract</b> (docs/design/beachcraft3.md, 9.2): it stands
 * on the arena floor, {@code facing} points to the pool centre {@value #POOL_DISTANCE} blocks away, and the boss
 * spawns there {@value #POOL_DEPTH} blocks below the altar. Use with the Tidewarden Key: summons {@code raft:leviathan}
 * (looked up by id at runtime), not while one is alive within {@value #ALIVE_RADIUS} blocks and not when the pool is
 * not there; every call spends the key (3.2; not in creative mode). {@code active} is true while the Leviathan is awake.
 */
public class LeviathanAltarBlock extends BaseEntityBlock {
    public static final EnumProperty<Direction> FACING = HorizontalDirectionalBlock.FACING;
    public static final BooleanProperty ACTIVE = BooleanProperty.create("active");
    public static final int POOL_DISTANCE = 9;
    public static final int POOL_DEPTH = 4;
    public static final double ALIVE_RADIUS = 48.0;
    private static final VoxelShape SHAPE = Shapes.or(Block.box(1, 0, 1, 15, 2, 15), Block.box(2, 2, 2, 14, 12, 14),
            Block.box(1, 12, 1, 15, 14, 15));

    public LeviathanAltarBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(ACTIVE, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, ACTIVE);
    }

    /** Facing = the direction the player looks while placing it (towards the pool); the carved face looks at the player. */
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(FACING, context.getHorizontalDirection());
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    protected boolean isPathfindable(BlockState state, PathComputationType type) {
        return false;
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new LeviathanAltarBlockEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (level.isClientSide()) return null;
        return createTickerHelper(type, StoryContent.LEVIATHAN_ALTAR_BE,
                (lvl, pos, st, be) -> LeviathanAltarBlockEntity.serverTick((ServerLevel) lvl, pos, st, be));
    }

    public static Optional<EntityType<?>> leviathanType() {
        return BuiltInRegistries.ENTITY_TYPE.getOptional(Raft.id("leviathan"));
    }

    /** A living Leviathan within {@link #ALIVE_RADIUS} blocks of the altar, or null. */
    public static @Nullable Entity findLeviathan(ServerLevel level, BlockPos pos) {
        Optional<EntityType<?>> type = leviathanType();
        if (type.isEmpty()) return null;
        List<? extends Entity> found = level.getEntities(type.get(), new AABB(pos).inflate(ALIVE_RADIUS), Entity::isAlive);
        return found.isEmpty() ? null : found.getFirst();
    }

    /** Where the boss appears: the pool centre, {@link #POOL_DISTANCE} blocks along facing, {@link #POOL_DEPTH} below. */
    public static BlockPos spawnPos(BlockPos altar, Direction facing) {
        return altar.relative(facing, POOL_DISTANCE).below(POOL_DEPTH);
    }

    @Override
    protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player,
                                          InteractionHand hand, BlockHitResult hit) {
        if (!stack.is(StoryContent.TIDEWARDEN_KEY)) {
            return InteractionResult.TRY_WITH_EMPTY_HAND;
        }
        if (level instanceof ServerLevel server && summon(server, pos, state, player)) {
            // 3.2: the key is spent on the call (another fight needs a new key: four new sigils); not in creative
            stack.consume(1, player);
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (level instanceof ServerLevel server) {
            StoryUtil.tell(player, findLeviathan(server, pos) != null ? "leviathan_altar.alive" : "leviathan_altar.need_key");
            server.playSound(null, pos, SoundEvents.CONDUIT_AMBIENT, SoundSource.BLOCKS, 0.8F, 0.6F);
        }
        return InteractionResult.SUCCESS;
    }

    /** Calls the Leviathan; true if it came (then the key is spent). */
    private boolean summon(ServerLevel level, BlockPos pos, BlockState state, Player player) {
        Optional<EntityType<?>> type = leviathanType();
        if (type.isEmpty()) {
            StoryUtil.tell(player, "leviathan_altar.silent");
            return false;
        }
        if (findLeviathan(level, pos) != null) {
            StoryUtil.tell(player, "leviathan_altar.alive");
            return false;
        }
        Direction facing = state.getValue(FACING);
        BlockPos at = spawnPos(pos, facing);
        if (!arenaReady(level, at)) {
            StoryUtil.tell(player, "leviathan_altar.not_ready");
            level.playSound(null, pos, SoundEvents.BEACON_DEACTIVATE, SoundSource.BLOCKS, 1.0F, 0.7F);
            return false;
        }
        LeviathanAltarBlockEntity be = level.getBlockEntity(pos) instanceof LeviathanAltarBlockEntity b ? b : null;
        Entity boss = type.get().create(level, EntitySpawnReason.TRIGGERED);
        if (boss == null) {
            StoryUtil.tell(player, "leviathan_altar.silent");
            return false;
        }
        boss.snapTo(at.getX() + 0.5, at.getY(), at.getZ() + 0.5, facing.getOpposite().toYRot(), 0.0F);
        if (boss instanceof Mob mob) {
            mob.finalizeSpawn(level, level.getCurrentDifficultyAt(at), EntitySpawnReason.TRIGGERED, null);
            mob.setPersistenceRequired();
        }
        level.addFreshEntity(boss);
        if (be != null) be.addSummon();
        level.setBlock(pos, state.setValue(ACTIVE, true), Block.UPDATE_ALL);

        double cx = at.getX() + 0.5, cy = at.getY() + 4.0, cz = at.getZ() + 0.5;
        level.sendParticles(ParticleTypes.BUBBLE_COLUMN_UP, cx, at.getY() + 1.0, cz, 120, 2.5, 2.0, 2.5, 0.2);
        level.sendParticles(ParticleTypes.SPLASH, cx, cy, cz, 200, 3.0, 0.3, 3.0, 0.4);
        level.sendParticles(ParticleTypes.NAUTILUS, pos.getX() + 0.5, pos.getY() + 1.2, pos.getZ() + 0.5, 60, 0.3, 0.3, 0.3, 1.0);
        level.playSound(null, at, AudioSounds.LEVIATHAN_SUMMON, SoundSource.HOSTILE, 4.0F, 1.0F);
        for (ServerPlayer p : level.getEntitiesOfClass(ServerPlayer.class, new AABB(pos).inflate(ALIVE_RADIUS))) {
            p.sendSystemMessage(Component.translatable("message.raft.leviathan_altar.summon")
                    .withStyle(ChatFormatting.DARK_AQUA, ChatFormatting.BOLD));
        }
        return true;
    }

    /** The pool must be there: water at the spawn point and in a 5 x 5 x 4 column around it (no drained or broken arena). */
    public static boolean arenaReady(ServerLevel level, BlockPos at) {
        int water = 0;
        for (BlockPos p : BlockPos.betweenClosed(at.offset(-2, 0, -2), at.offset(2, 3, 2))) {
            if (level.getFluidState(p).is(net.minecraft.tags.FluidTags.WATER)) {
                water++;
            }
        }
        return water >= 5 * 5 * 4 * 3 / 4;
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (state.getValue(ACTIVE)) {
            if (random.nextInt(2) == 0) {
                level.addParticle(ParticleTypes.NAUTILUS, pos.getX() + 0.5, pos.getY() + 1.5, pos.getZ() + 0.5,
                        (random.nextDouble() - 0.5) * 2, -random.nextDouble(), (random.nextDouble() - 0.5) * 2);
            }
        } else if (random.nextInt(10) == 0) {
            level.addParticle(ParticleTypes.GLOW, pos.getX() + 0.3 + random.nextDouble() * 0.4, pos.getY() + 0.95,
                    pos.getZ() + 0.3 + random.nextDouble() * 0.4, 0.0, 0.01, 0.0);
        }
    }

    /** Structures are placed rotated and mirrored: the facing must turn with them (the boss contract depends on it). */
    @Override
    protected BlockState rotate(BlockState state, Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    @Override
    protected BlockState mirror(BlockState state, Mirror mirror) {
        return state.rotate(mirror.getRotation(state.getValue(FACING)));
    }
}
