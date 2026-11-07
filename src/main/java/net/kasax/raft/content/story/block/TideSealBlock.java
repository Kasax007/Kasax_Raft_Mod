package net.kasax.raft.content.story.block;

import net.kasax.raft.content.audio.AudioSounds;
import net.kasax.raft.content.story.StoryContent;
import net.kasax.raft.content.story.StoryUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.world.ItemInteractionResult;

/**
 * The Tide Seal: an unbreakable, shimmering barrier of the Abyssal Sanctum. Using the Tidewarden Key on it opens every
 * connected seal block (flood fill, at most {@value #MAX_OPEN}) in a ripple from the clicked block outwards. An open
 * seal has no collision and is invisible except for faint particles, but it still holds the sea back (water cannot
 * flow into it), so the dome behind it stays dry and players simply swim through the wall of light.
 */
public class TideSealBlock extends Block {
    public static final BooleanProperty OPEN = BlockStateProperties.OPEN;
    public static final int MAX_OPEN = 256;

    public TideSealBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(OPEN, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(OPEN);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return state.getValue(OPEN) ? Shapes.empty() : Shapes.block();
    }

    @Override
    protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return state.getValue(OPEN) ? Shapes.empty() : Shapes.block();
    }

    @Override
    protected VoxelShape getVisualShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return Shapes.empty();
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return state.getValue(OPEN) ? RenderShape.INVISIBLE : RenderShape.MODEL;
    }

    @Override
    protected boolean skipRendering(BlockState state, BlockState neighbour, Direction direction) {
        return neighbour.is(this) && !neighbour.getValue(OPEN) || super.skipRendering(state, neighbour, direction);
    }

    @Override
    protected float getShadeBrightness(BlockState state, BlockGetter level, BlockPos pos) {
        return 1.0F;
    }

    @Override
    protected boolean propagatesSkylightDown(BlockState state, net.minecraft.world.level.BlockGetter level121, BlockPos pos121) {
        return true;
    }

    @Override
    protected boolean isPathfindable(BlockState state, PathComputationType type) {
        return state.getValue(OPEN);
    }

    @Override
    protected net.minecraft.world.ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (state.getValue(OPEN) || !stack.is(StoryContent.TIDEWARDEN_KEY)) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        if (level instanceof ServerLevel server) {
            open(server, pos);
            StoryUtil.tell(player, "seal.open");
        }
        return ItemInteractionResult.SUCCESS;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (state.getValue(OPEN)) {
            return InteractionResult.PASS;
        }
        if (!level.isClientSide()) {
            StoryUtil.tell(player, "seal.locked");
            level.playSound(null, pos, AudioSounds.SEAL_LOCKED, SoundSource.BLOCKS, 1.0F, 1.0F);
        }
        return InteractionResult.SUCCESS;
    }

    /** Opens the connected seal blocks in a ripple: each one is opened by a scheduled tick, later the farther away it is. */
    public void open(ServerLevel level, BlockPos start) {
        Map<BlockPos, Integer> dist = new HashMap<>();
        ArrayDeque<BlockPos> queue = new ArrayDeque<>();
        dist.put(start, 0);
        queue.add(start);
        while (!queue.isEmpty() && dist.size() < MAX_OPEN) {
            BlockPos p = queue.poll();
            for (Direction d : Direction.values()) {
                BlockPos n = p.relative(d);
                if (!dist.containsKey(n) && dist.size() < MAX_OPEN) {
                    BlockState s = level.getBlockState(n);
                    if (s.is(this) && !s.getValue(OPEN)) {
                        dist.put(n, dist.get(p) + 1);
                        queue.add(n);
                    }
                }
            }
        }
        dist.forEach((p, d) -> level.scheduleTick(p, this, 1 + d * 3));
        level.playSound(null, start, AudioSounds.SEAL_OPEN, SoundSource.BLOCKS, 3.0F, 1.0F);
    }

    @Override
    protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (state.getValue(OPEN)) {
            return;
        }
        level.setBlock(pos, state.setValue(OPEN, true), Block.UPDATE_ALL);
        level.sendParticles(ParticleTypes.GLOW, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 6, 0.3, 0.3, 0.3, 0.02);
        level.sendParticles(ParticleTypes.END_ROD, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 2, 0.25, 0.25, 0.25, 0.01);
        if (random.nextInt(4) == 0) {
            level.playSound(null, pos, SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.BLOCKS, 0.7F, 0.6F + random.nextFloat() * 0.6F);
        }
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (state.getValue(OPEN)) {
            if (random.nextInt(5) == 0) {
                level.addParticle(ParticleTypes.NAUTILUS, pos.getX() + random.nextDouble(), pos.getY() + random.nextDouble(),
                        pos.getZ() + random.nextDouble(), 0.0, 0.02, 0.0);
            }
        } else if (random.nextInt(8) == 0) {
            Direction d = Direction.getRandom(random);
            double x = pos.getX() + 0.5 + d.getStepX() * 0.52 + (d.getStepX() == 0 ? random.nextDouble() - 0.5 : 0);
            double y = pos.getY() + 0.5 + d.getStepY() * 0.52 + (d.getStepY() == 0 ? random.nextDouble() - 0.5 : 0);
            double z = pos.getZ() + 0.5 + d.getStepZ() * 0.52 + (d.getStepZ() == 0 ? random.nextDouble() - 0.5 : 0);
            level.addParticle(ParticleTypes.GLOW, x, y, z, 0.0, 0.0, 0.0);
        }
    }
}
