package net.kasax.raft.content.cozy.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;
import net.minecraft.world.ItemInteractionResult;

/**
 * Serving plate: holds up to four of one food, shown lying on the plate. Right-click with food to serve a portion,
 * right-click with an empty hand to eat one portion right from the plate (when hungry), sneak-click to take it back.
 */
public class PlateBlock extends CozyBlock implements EntityBlock {
    public static final int MAX = 4;

    public PlateBlock(Properties properties, Config config) {
        super(properties, config);
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new PlateBlockEntity(pos, state);
    }

    @Override
    protected net.minecraft.world.ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (!(level.getBlockEntity(pos) instanceof PlateBlockEntity plate)) return ItemInteractionResult.SKIP_DEFAULT_BLOCK_INTERACTION;
        if (stack.isEmpty()) return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        if (!stack.has(DataComponents.FOOD)) {
            if (!level.isClientSide) player.displayClientMessage(Component.translatable("message.raft.plate.not_food"), true);
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        ItemStack held = plate.food();
        if (!held.isEmpty() && (!ItemStack.isSameItemSameComponents(held, stack) || held.getCount() >= MAX)) {
            if (!level.isClientSide) player.displayClientMessage(Component.translatable("message.raft.plate.full"), true);
            return ItemInteractionResult.SUCCESS;
        }
        if (!level.isClientSide) {
            int n = 1;
            ItemStack put = stack.copyWithCount(held.getCount() + n);
            stack.consume(n, player);
            plate.setFood(put);
            level.playSound(null, pos, SoundEvents.DECORATED_POT_INSERT, SoundSource.BLOCKS, 0.7F, 1.4F);
        }
        return ItemInteractionResult.SUCCESS;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!(level.getBlockEntity(pos) instanceof PlateBlockEntity plate) || plate.food().isEmpty()) return InteractionResult.PASS;
        if (level.isClientSide) return InteractionResult.SUCCESS;
        ItemStack held = plate.food();
        if (player.isShiftKeyDown()) {
            player.getInventory().placeItemBackInInventory(held.copy());
            plate.setFood(ItemStack.EMPTY);
            level.playSound(null, pos, SoundEvents.ITEM_PICKUP, SoundSource.BLOCKS, 0.5F, 1.2F);
            return InteractionResult.SUCCESS;
        }
        FoodProperties food = held.get(DataComponents.FOOD);
        if (food == null || !player.canEat(food.canAlwaysEat())) {
            player.displayClientMessage(Component.translatable("message.raft.plate.not_hungry"), true);
            return InteractionResult.SUCCESS;
        }
        ItemStack portion = held.split(1);
        plate.setFood(held);
        ItemStack rest = portion.finishUsingItem(level, player);
        if (!rest.isEmpty()) player.getInventory().placeItemBackInInventory(rest);
        level.playSound(null, pos, SoundEvents.GENERIC_EAT, SoundSource.PLAYERS, 0.8F, 1.0F);
        return InteractionResult.SUCCESS;
    }

    /** Drops the food when the plate is broken. */
    public static void dropFood(Level level, BlockPos pos, PlateBlockEntity plate) {
        if (!plate.food().isEmpty()) Containers.dropItemStack(level, pos.getX() + 0.5, pos.getY() + 0.2, pos.getZ() + 0.5, plate.food());
    }
}
