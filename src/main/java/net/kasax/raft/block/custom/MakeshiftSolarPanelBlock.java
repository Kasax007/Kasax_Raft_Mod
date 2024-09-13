package net.kasax.raft.block.custom;

import com.mojang.serialization.MapCodec;
import net.kasax.raft.Raft;
import net.kasax.raft.block.entity.MakeshiftSolarPanelBlockEntity;
import net.kasax.raft.block.entity.ModBlockEntities;
import net.minecraft.block.Block;
import net.minecraft.block.BlockEntityProvider;
import net.minecraft.block.BlockState;
import net.minecraft.block.BlockWithEntity;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityTicker;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class MakeshiftSolarPanelBlock extends BlockWithEntity implements BlockEntityProvider {
    public MakeshiftSolarPanelBlock(Settings settings) {
        super(settings);
    }

    @Override
    protected MapCodec<? extends BlockWithEntity> getCodec() {
        return null;
    }

    @Nullable
    @Override
    public BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
        return ModBlockEntities.MAKESHIT_SOLAR_PANEL_BLOCK_ENTITY.instantiate(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(World world, BlockState state, BlockEntityType<T> type) {
        return validateTicker(type, ModBlockEntities.MAKESHIT_SOLAR_PANEL_BLOCK_ENTITY, (world1, pos, state1, blockEntity) -> blockEntity.tick(world1, pos, state1));
    }

    @Override
    public ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, BlockHitResult hit) {
        if (!world.isClient) {
            BlockEntity blockEntity = world.getBlockEntity(pos);
            if (blockEntity instanceof MakeshiftSolarPanelBlockEntity solarPanel) {
                long energy = solarPanel.getEnergy();
                long maxEnergy = solarPanel.getMaxEnergy();
                player.sendMessage(Text.of("Energy: " + energy + " / Max Energy: " + maxEnergy), true);
            }
        }
        return ActionResult.success(world.isClient);
    }

    @Override
    public void appendTooltip(ItemStack stack, Item.TooltipContext context, List<Text> tooltip, TooltipType options) {
        tooltip.add(Text.translatable("tooltip.raft.makeshift_solar_panel.tooltip").formatted(Formatting.AQUA));
        super.appendTooltip(stack, context, tooltip, options);
    }
}
