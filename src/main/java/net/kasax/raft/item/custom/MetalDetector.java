package net.kasax.raft.item.custom;

import net.minecraft.ChatFormatting;

import net.kasax.raft.block.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

public class MetalDetector extends Item {
    public MetalDetector(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Player player = context.getPlayer();
        if (player == null) {
            return InteractionResult.PASS;
        }
        if (!context.getLevel().isClientSide()) {
            BlockPos positionClicked = context.getClickedPos();
            boolean foundBlock = false;

            int minY = context.getLevel().getMinBuildHeight();
            for (int i = 0; positionClicked.getY() - i >= minY; i++) {
                BlockState state = context.getLevel().getBlockState(positionClicked.below(i));

                if (isValuableBlock(state)) {
                    outputValuableCoordinates(positionClicked.below(i), player, state.getBlock());
                    foundBlock = true;

                    break;
                }
            }

            if (!foundBlock) {
                player.displayClientMessage(Component.translatable("item.raft.metal_detector.nothing").withStyle(ChatFormatting.AQUA), true);
            }

            player.getCooldowns().addCooldown(context.getItemInHand().getItem(), 20);
            context.getItemInHand().hurtAndBreak(1, player, net.minecraft.world.entity.LivingEntity.getSlotForHand(context.getHand()));
        }

        return InteractionResult.SUCCESS;
    }

    private void outputValuableCoordinates(BlockPos blockPos, Player player, Block block) {
        player.displayClientMessage(Component.translatable("item.raft.metal_detector.found", block.getName(), blockPos.getY())
                .withStyle(ChatFormatting.AQUA), true);
    }

    private boolean isValuableBlock(BlockState state) {
        return state.is(ModBlocks.TITANIUM_ORE) || state.is(ModBlocks.DEEPSLATE_TITANIUM_ORE);
    }
}
