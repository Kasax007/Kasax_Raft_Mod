package net.kasax.raft.block.custom;

import net.kasax.raft.block.entity.MakeshiftBatteryBlockEntity;
import net.kasax.raft.block.entity.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;

public class MakeshiftBatteryBlock extends BaseEntityBlock {
    @Override
    protected com.mojang.serialization.MapCodec<? extends BaseEntityBlock> codec() {
        return simpleCodec(MakeshiftBatteryBlock::new);
    }

    public MakeshiftBatteryBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!level.isClientSide()) {
            if (level.getBlockEntity(pos) instanceof MakeshiftBatteryBlockEntity battery) {
                player.openMenu(battery);
            }
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return ModBlockEntities.MAKESHIFT_BATTERY_BLOCK_ENTITY.create(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (level.isClientSide()) {
            return null;
        }
        return createTickerHelper(type, ModBlockEntities.MAKESHIFT_BATTERY_BLOCK_ENTITY,
                (lvl, pos, st, be) -> be.tick(lvl, pos, st));
    }
}
