package net.kasax.raft.block.custom;

import net.kasax.raft.block.entity.MakeshiftSolarPanelBlockEntity;
import net.kasax.raft.block.entity.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
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

public class MakeshiftSolarPanelBlock extends BaseEntityBlock {
    @Override
    protected com.mojang.serialization.MapCodec<? extends BaseEntityBlock> codec() {
        return simpleCodec(MakeshiftSolarPanelBlock::new);
    }

    public MakeshiftSolarPanelBlock(Properties properties) {
        super(properties);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return ModBlockEntities.MAKESHIFT_SOLAR_PANEL_BLOCK_ENTITY.create(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (level.isClientSide()) {
            return null;
        }
        return createTickerHelper(type, ModBlockEntities.MAKESHIFT_SOLAR_PANEL_BLOCK_ENTITY,
                (lvl, pos, st, be) -> be.tick(lvl, pos, st));
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!level.isClientSide()) {
            if (level.getBlockEntity(pos) instanceof MakeshiftSolarPanelBlockEntity solarPanel) {
                player.displayClientMessage(Component.translatable("message.raft.solar_panel.energy", solarPanel.getEnergy(), solarPanel.getMaxEnergy()), true);
            }
        }
        return InteractionResult.SUCCESS;
    }
}
