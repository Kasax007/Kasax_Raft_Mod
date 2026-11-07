package net.kasax.raft.content.decor.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.ItemInteractionResult;

/**
 * A two-block bamboo torch ({@code half}, {@code lit}). The flame sits on the upper half (light 14 there when lit).
 * Flint and steel or a fire charge lights it, a shovel douses it.
 */
public class TikiTorchBlock extends TallDecorBlock {
    public static final BooleanProperty LIT = BlockStateProperties.LIT;

    public TikiTorchBlock(Properties properties) {
        super(properties, "tiki_torch", false);
    }

    @Override
    protected BlockState defaultState(BlockState state) {
        return state.setValue(LIT, true);
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(LIT);
    }

    public static int light(BlockState state) {
        return state.getValue(LIT) && state.getValue(HALF) == DoubleBlockHalf.UPPER ? 14 : 0;
    }

    @Override
    protected net.minecraft.world.ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        boolean lit = state.getValue(LIT);
        if (!lit && FireHelper.isIgniter(stack)) {
            FireHelper.ignite(level, pos, player, hand, stack);
            setLit(level, pos, state, true);
            return ItemInteractionResult.SUCCESS;
        }
        if (lit && FireHelper.isDouser(stack)) {
            BlockPos top = state.getValue(HALF) == DoubleBlockHalf.UPPER ? pos : pos.above();
            FireHelper.douse(level, top, player, hand, stack, 0.8);
            setLit(level, pos, state, false);
            return ItemInteractionResult.SUCCESS;
        }
        return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }

    private static void setLit(Level level, BlockPos pos, BlockState state, boolean lit) {
        if (level.isClientSide()) return;
        BlockPos other = state.getValue(HALF) == DoubleBlockHalf.LOWER ? pos.above() : pos.below();
        level.setBlock(pos, state.setValue(LIT, lit), Block.UPDATE_ALL);
        BlockState otherState = level.getBlockState(other);
        if (otherState.is(state.getBlock())) {
            level.setBlock(other, otherState.setValue(LIT, lit), Block.UPDATE_ALL);
        }
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (!state.getValue(LIT) || state.getValue(HALF) != DoubleBlockHalf.UPPER) return;
        double x = pos.getX() + 0.5 + (random.nextDouble() - 0.5) * 0.2;
        double y = pos.getY() + 0.85 + random.nextDouble() * 0.15;
        double z = pos.getZ() + 0.5 + (random.nextDouble() - 0.5) * 0.2;
        level.addParticle(ParticleTypes.FLAME, x, y, z, 0, 0.01, 0);
        if (random.nextInt(3) == 0) {
            level.addParticle(ParticleTypes.SMOKE, x, y + 0.35, z, 0, 0.03, 0);
        }
        if (random.nextInt(6) == 0) {
            level.addParticle(ParticleTypes.SMALL_FLAME, x, y + 0.1, z, (random.nextDouble() - 0.5) * 0.02, 0.03, (random.nextDouble() - 0.5) * 0.02);
        }
        if (random.nextInt(20) == 0) {
            level.playLocalSound(pos.getX() + 0.5, pos.getY() + 0.8, pos.getZ() + 0.5, SoundEvents.CAMPFIRE_CRACKLE,
                    SoundSource.BLOCKS, 0.4F + random.nextFloat() * 0.3F, random.nextFloat() * 0.7F + 0.6F, false);
        }
    }
}
