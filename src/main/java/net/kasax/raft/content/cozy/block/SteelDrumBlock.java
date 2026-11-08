package net.kasax.raft.content.cozy.block;

import net.kasax.raft.content.cozy.CozyContent;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;

/**
 * Steel drum: every right-click (or redstone pulse) plays the next note of a short island tune. The tune position is
 * the {@code note} state, so a row of drums can play in turns.
 */
public class SteelDrumBlock extends CozyBlock {
    public static final IntegerProperty NOTE = IntegerProperty.create("note", 0, 15);
    public static final BooleanProperty POWERED = BlockStateProperties.POWERED;
    /** Semitones above F#3 (pitch 0.5 = F#3 ... 2.0 = F#5): a little calypso phrase. */
    static final int[] TUNE = {12, 14, 16, 12, 19, 16, 14, 12, 14, 16, 17, 16, 14, 11, 12, 7};

    public SteelDrumBlock(Properties properties, Config config) {
        super(properties, config);
    }

    @Override
    protected BlockState defaultState(BlockState state) {
        return state.setValue(NOTE, 0).setValue(POWERED, false);
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(NOTE, POWERED);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!level.isClientSide) play(state, level, pos);
        return InteractionResult.SUCCESS;
    }

    @Override
    protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block block, BlockPos fromPos,
                                   boolean movedByPiston) {
        boolean powered = level.hasNeighborSignal(pos);
        if (powered != state.getValue(POWERED)) {
            if (powered) state = play(state, level, pos);
            level.setBlock(pos, state.setValue(POWERED, powered), Block.UPDATE_ALL);
        }
    }

    private BlockState play(BlockState state, Level level, BlockPos pos) {
        int note = state.getValue(NOTE);
        float pitch = (float) Math.pow(2.0, (TUNE[note] - 12) / 12.0);
        level.playSound(null, pos, CozyContent.STEEL_DRUM_NOTE, SoundSource.RECORDS, 1.0F, pitch);
        if (level instanceof ServerLevel server) {
            server.sendParticles(ParticleTypes.NOTE, pos.getX() + 0.5, pos.getY() + 1.1, pos.getZ() + 0.5, 0,
                    TUNE[note] / 24.0, 0, 0, 1);
        }
        BlockState next = state.setValue(NOTE, (note + 1) % TUNE.length);
        level.setBlock(pos, next, Block.UPDATE_CLIENTS);
        return next;
    }
}
