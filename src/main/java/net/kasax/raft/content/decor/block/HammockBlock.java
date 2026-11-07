package net.kasax.raft.content.decor.block;

import net.kasax.raft.content.audio.AudioSounds;
import net.kasax.raft.content.decor.entity.SeatEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.item.DyeColor;
import org.jetbrains.annotations.Nullable;
import net.minecraft.world.level.block.state.properties.BedPart;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.Map;

/**
 * A hammock two blocks long ({@code facing}, {@code part=head/foot}, like a bed). Right-click: at night you lie down
 * and sleep like in a bed; by day you settle in and rest. Either way it sets your respawn point.
 */
public class HammockBlock extends BedBlock {
    /** Height of the fabric where the player lies or sits. */
    public static final double REST_HEIGHT = 0.3;

    private final Map<Direction, VoxelShape> footShapes = DecorBlock.shapes("hammock_foot", true);
    private final Map<Direction, VoxelShape> headShapes = DecorBlock.shapes("hammock_head", true);

    public HammockBlock(Properties properties) {
        super(DyeColor.ORANGE, properties);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(PART, BedPart.FOOT)
                .setValue(OCCUPIED, false));
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return (state.getValue(PART) == BedPart.HEAD ? headShapes : footShapes).get(state.getValue(FACING));
    }

    /** The hammock is an ordinary block model (a bed is drawn by its block entity). */
    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return null;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (level.isClientSide()) return InteractionResult.SUCCESS;
        BlockPos head = state.getValue(PART) == BedPart.HEAD ? pos : pos.relative(state.getValue(FACING));
        BlockState headState = level.getBlockState(head);
        if (!headState.is(this)) return InteractionResult.CONSUME;
        boolean canSetSpawn = BedBlock.canSetSpawn(level);
        if (canSetSpawn && !level.isDay()) {
            // night: sleep like in a bed (sets the spawn point too)
            level.playSound(null, pos, AudioSounds.HAMMOCK_CREAK, SoundSource.BLOCKS, 0.8F, 0.9F + level.getRandom().nextFloat() * 0.2F);
            return super.useWithoutItem(state, level, pos, player, hit);
        }
        // a hammock never explodes; by day (or where beds do not work) you settle in and rest
        if (canSetSpawn && player instanceof ServerPlayer serverPlayer && level instanceof ServerLevel serverLevel) {
            boolean same = head.equals(serverPlayer.getRespawnPosition()) && serverLevel.dimension() == serverPlayer.getRespawnDimension();
            serverPlayer.setRespawnPosition(serverLevel.dimension(), head, player.getYRot(), false, !same);
        }
        if (SeatEntity.sit(level, pos, player, REST_HEIGHT)) {
            level.playSound(null, pos, AudioSounds.HAMMOCK_CREAK, SoundSource.BLOCKS, 0.8F, 0.9F + level.getRandom().nextFloat() * 0.2F);
            player.displayClientMessage(Component.translatable("message.raft.hammock.rest"), true);
        }
        return InteractionResult.SUCCESS;
    }
}
