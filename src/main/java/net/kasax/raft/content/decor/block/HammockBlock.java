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
import net.minecraft.world.attribute.BedRule;
import net.minecraft.world.attribute.EnvironmentAttribute;
import net.minecraft.world.attribute.EnvironmentAttributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.AbstractBedBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BedPart;
import net.minecraft.world.level.storage.LevelData;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.Map;
import java.util.OptionalDouble;

/**
 * A hammock two blocks long ({@code facing}, {@code part=head/foot}, like a bed). Right-click: at night you lie down
 * and sleep like in a bed; by day you settle in and rest. Either way it sets your respawn point.
 */
public class HammockBlock extends AbstractBedBlock {
    /** Height of the fabric where the player lies or sits. */
    public static final double REST_HEIGHT = 0.3;

    private final Map<Direction, VoxelShape> footShapes = DecorBlock.shapes("hammock_foot", true);
    private final Map<Direction, VoxelShape> headShapes = DecorBlock.shapes("hammock_head", true);

    public HammockBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(PART, BedPart.FOOT)
                .setValue(OCCUPIED, false));
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return (state.getValue(PART) == BedPart.HEAD ? headShapes : footShapes).get(state.getValue(FACING));
    }

    @Override
    protected EnvironmentAttribute<BedRule> getBedEnvironmentAttribute() {
        return EnvironmentAttributes.BED_RULE;
    }

    @Override
    protected InteractionResult destroyOnUse(BlockState state, Level level, BlockPos pos, Player player) {
        // a hammock never explodes; where beds do not work it simply does nothing
        return InteractionResult.SUCCESS_SERVER;
    }

    @Override
    protected void destroyOnLeave(Level level, BlockPos pos) {
    }

    @Override
    public OptionalDouble getSleepHeight(BlockState state, Level level, BlockPos pos) {
        return state.is(this) ? OptionalDouble.of(REST_HEIGHT + 0.15) : OptionalDouble.empty();
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (level.isClientSide()) return InteractionResult.SUCCESS_SERVER;
        BlockPos head = state.getValue(PART) == BedPart.HEAD ? pos : pos.relative(state.getValue(FACING));
        BlockState headState = level.getBlockState(head);
        if (!headState.is(this)) return InteractionResult.CONSUME;
        BedRule rule = getBedRule(level, head);
        if (rule.canSleep(level) || rule.destroyOnUse()) {
            // night: sleep like in a bed (sets the spawn point too)
            level.playSound(null, pos, AudioSounds.HAMMOCK_CREAK, SoundSource.BLOCKS, 0.8F, 0.9F + level.getRandom().nextFloat() * 0.2F);
            return super.useWithoutItem(state, level, pos, player, hit);
        }
        if (rule.canSetSpawn(level) && player instanceof ServerPlayer serverPlayer && level instanceof ServerLevel serverLevel) {
            ServerPlayer.RespawnConfig config = new ServerPlayer.RespawnConfig(
                    LevelData.RespawnData.of(serverLevel.dimension(), head, player.getYRot(), player.getXRot()), false);
            ServerPlayer.RespawnConfig old = serverPlayer.getRespawnConfig();
            serverPlayer.setRespawnPosition(config, old == null || !old.isSamePosition(config));
        }
        if (SeatEntity.sit(level, pos, player, REST_HEIGHT)) {
            level.playSound(null, pos, AudioSounds.HAMMOCK_CREAK, SoundSource.BLOCKS, 0.8F, 0.9F + level.getRandom().nextFloat() * 0.2F);
            player.sendOverlayMessage(Component.translatable("message.raft.hammock.rest"));
        }
        return InteractionResult.SUCCESS_SERVER;
    }
}
