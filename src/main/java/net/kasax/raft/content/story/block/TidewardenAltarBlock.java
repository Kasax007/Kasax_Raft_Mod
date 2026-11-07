package net.kasax.raft.content.story.block;

import net.kasax.raft.content.audio.AudioSounds;
import net.kasax.raft.content.story.StoryContent;
import net.kasax.raft.content.story.StoryUtil;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.SimpleWaterloggedBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.world.ItemInteractionResult;

/**
 * The Tidewarden altar of the Coral Temple. Use a Tide Sigil on it to set it into its hollow (shown as a glowing gem).
 * When all four rest in it, the altar blazes up and after a few seconds forges the Tidewarden Key, which rises from
 * the light; the hollows are empty again (another key needs four new sigils). Breaking the altar gives the sigils back.
 */
public class TidewardenAltarBlock extends HorizontalDirectionalBlock implements SimpleWaterloggedBlock {
    @Override
    protected com.mojang.serialization.MapCodec<? extends HorizontalDirectionalBlock> codec() {
        return simpleCodec(TidewardenAltarBlock::new);
    }

    public static final BooleanProperty CORAL = BooleanProperty.create("coral");
    public static final BooleanProperty PEARL = BooleanProperty.create("pearl");
    public static final BooleanProperty STORM = BooleanProperty.create("storm");
    public static final BooleanProperty EMBER = BooleanProperty.create("ember");
    /** The Coral Temple is a flooded ruin: the altar stands in water (no dry cell around it). */
    public static final BooleanProperty WATERLOGGED = BlockStateProperties.WATERLOGGED;
    public static final String[] SLOTS = {"coral", "pearl", "storm", "ember"};
    public static final int FORGE_TICKS = 60;
    private static final VoxelShape SHAPE = Shapes.or(Block.box(0, 0, 0, 16, 3, 16), Block.box(2, 3, 2, 14, 11, 14),
            Block.box(0, 11, 0, 16, 14, 16));

    public TidewardenAltarBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(CORAL, false)
                .setValue(PEARL, false).setValue(STORM, false).setValue(EMBER, false).setValue(WATERLOGGED, false));
    }

    public static BooleanProperty property(String slot) {
        return switch (slot) {
            case "coral" -> CORAL;
            case "pearl" -> PEARL;
            case "storm" -> STORM;
            default -> EMBER;
        };
    }

    public static int count(BlockState state) {
        int n = 0;
        for (String s : SLOTS) {
            if (state.getValue(property(s))) n++;
        }
        return n;
    }

    public static int light(BlockState state) {
        return 4 + count(state) * 2;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, CORAL, PEARL, STORM, EMBER, WATERLOGGED);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite())
                .setValue(WATERLOGGED, context.getLevel().getFluidState(context.getClickedPos()).is(Fluids.WATER));
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
        return SHAPE;
    }

    @Override
    protected boolean isPathfindable(BlockState state, PathComputationType type) {
        return false;
    }

    private static String slotOf(ItemStack stack) {
        for (String s : SLOTS) {
            if (stack.is(StoryContent.sigil(s))) return s;
        }
        return null;
    }

    @Override
    protected net.minecraft.world.ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        String slot = slotOf(stack);
        if (slot == null) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        BooleanProperty prop = property(slot);
        if (state.getValue(prop)) {
            if (!level.isClientSide()) StoryUtil.tell(player, "altar.has");
            return ItemInteractionResult.SUCCESS;
        }
        if (level instanceof ServerLevel server) {
            BlockState next = state.setValue(prop, true);
            server.setBlock(pos, next, Block.UPDATE_ALL);
            Component name = stack.getHoverName();
            stack.consume(1, player);
            server.playSound(null, pos, SoundEvents.END_PORTAL_FRAME_FILL, SoundSource.BLOCKS, 0.6F, 1.0F);
            // the sigil's chime rises with every sigil in the altar
            server.playSound(null, pos, AudioSounds.ALTAR_INSERT, SoundSource.BLOCKS, 1.0F, 0.85F + count(next) * 0.1F);
            server.sendParticles(ParticleTypes.GLOW, pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5, 12, 0.3, 0.1, 0.3, 0.02);
            StoryUtil.tell(player, "altar.inserted", name);
            if (count(next) == 4) {
                server.scheduleTick(pos, this, FORGE_TICKS);
                server.playSound(null, pos, SoundEvents.BEACON_ACTIVATE, SoundSource.BLOCKS, 1.5F, 1.2F);
                server.playSound(null, pos, SoundEvents.CONDUIT_ACTIVATE, SoundSource.BLOCKS, 1.5F, 1.0F);
                for (ServerPlayer p : server.getEntitiesOfClass(ServerPlayer.class, new AABB(pos).inflate(24))) {
                    p.sendSystemMessage(Component.translatable("message.raft.altar.awake").withStyle(ChatFormatting.AQUA, ChatFormatting.ITALIC));
                }
            }
        }
        return ItemInteractionResult.SUCCESS;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!level.isClientSide()) {
            int n = count(state);
            if (n == 0) {
                StoryUtil.tell(player, "altar.empty");
            } else if (n == 4) {
                StoryUtil.tell(player, "altar.awake");
            } else {
                List<Component> missing = new ArrayList<>();
                for (String s : SLOTS) {
                    if (!state.getValue(property(s))) missing.add(StoryContent.sigil(s).getName(StoryContent.sigil(s).getDefaultInstance()));
                }
                StoryUtil.tell(player, "altar.status", n, net.minecraft.network.chat.ComponentUtils.formatList(missing, Component.literal(", ")));
            }
            level.playSound(null, pos, SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.BLOCKS, 0.6F, 1.0F);
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (count(state) < 4) {
            return;
        }
        level.setBlock(pos, state.setValue(CORAL, false).setValue(PEARL, false).setValue(STORM, false).setValue(EMBER, false),
                Block.UPDATE_ALL);
        double x = pos.getX() + 0.5, y = pos.getY() + 1.1, z = pos.getZ() + 0.5;
        ItemEntity key = new ItemEntity(level, x, y, z, new ItemStack(StoryContent.TIDEWARDEN_KEY), 0.0, 0.28, 0.0);
        key.setPickUpDelay(20);
        key.setGlowingTag(true);
        level.addFreshEntity(key);
        level.sendParticles(ParticleTypes.END_ROD, x, y + 0.3, z, 40, 0.2, 0.6, 0.2, 0.12);
        level.sendParticles(ParticleTypes.GLOW, x, y, z, 30, 0.6, 0.4, 0.6, 0.05);
        level.sendParticles(ParticleTypes.TOTEM_OF_UNDYING, x, y + 0.5, z, 30, 0.3, 0.3, 0.3, 0.3);
        level.playSound(null, pos, AudioSounds.ALTAR_FORGE, SoundSource.BLOCKS, 2.0F, 1.0F);
        for (ServerPlayer p : level.getEntitiesOfClass(ServerPlayer.class, new AABB(pos).inflate(24))) {
            p.sendSystemMessage(Component.translatable("message.raft.altar.forged").withStyle(ChatFormatting.GOLD, ChatFormatting.ITALIC));
        }
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        int n = count(state);
        if (n == 0) {
            if (random.nextInt(6) == 0) {
                level.addParticle(ParticleTypes.GLOW, pos.getX() + 0.2 + random.nextDouble() * 0.6, pos.getY() + 0.9,
                        pos.getZ() + 0.2 + random.nextDouble() * 0.6, 0.0, 0.02, 0.0);
            }
            return;
        }
        double cx = pos.getX() + 0.5, cy = pos.getY() + 0.9, cz = pos.getZ() + 0.5;
        if (n == 4) {
            // a spiral of light rising out of the four gems while the key is forged
            for (int i = 0; i < 4; i++) {
                double a = (level.getGameTime() * 0.25) + i * Math.PI / 2;
                level.addParticle(ParticleTypes.END_ROD, cx + Math.cos(a) * 0.35, cy + 0.1, cz + Math.sin(a) * 0.35,
                        -Math.cos(a) * 0.02, 0.06, -Math.sin(a) * 0.02);
            }
            level.addParticle(ParticleTypes.ENCHANT, cx, cy + 1.2, cz, (random.nextDouble() - 0.5) * 2, -1.0, (random.nextDouble() - 0.5) * 2);
        } else if (random.nextInt(4 - Math.min(3, n - 1)) == 0) {
            level.addParticle(ParticleTypes.GLOW, cx + (random.nextDouble() - 0.5) * 0.7, cy + 0.1, cz + (random.nextDouble() - 0.5) * 0.7,
                    0.0, 0.03, 0.0);
        }
    }
}
