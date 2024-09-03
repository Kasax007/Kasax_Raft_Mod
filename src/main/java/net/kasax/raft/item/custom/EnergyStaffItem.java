package net.kasax.raft.item.custom;

import net.kasax.raft.block.ModBlocks;
import net.minecraft.block.BlockState;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

import java.util.List;

import static net.minecraft.sound.SoundEvents.ITEM_LODESTONE_COMPASS_LOCK;

public class EnergyStaffItem extends Item {
    public EnergyStaffItem(Settings settings) {
        super(settings);
    }

    @Override
    public ActionResult useOnBlock(ItemUsageContext context) {
        World world = context.getWorld();
        PlayerEntity user = context.getPlayer();
        Hand hand = context.getHand();
        BlockPos pos = context.getBlockPos();
        BlockState state = world.getBlockState(pos);

        if (state.isOf(ModBlocks.DRIFTWOOD_PORTAL_FRAME)) {
            // Perform your custom logic for the custom portal block
            if (!world.isClient() && hand == Hand.MAIN_HAND) {
                assert user != null;
                user.getItemCooldownManager().set(this, 20);
                world.playSound(null, user.getX(), user.getY(), user.getZ(),
                        ITEM_LODESTONE_COMPASS_LOCK, SoundCategory.NEUTRAL, 20F,
                        1F / (world.getRandom().nextFloat() * 0.4F + 0.8F));
                damageItem((ServerPlayerEntity) user, 1);
            }
            return ActionResult.PASS;
        } else {
            // Display a message in the chat for other blocks
            if (!world.isClient() && user != null) {
                user.sendMessage(Text.translatable("chat.raft.energy_staff.invalid_target").formatted(Formatting.BLUE), true);
            }
            return ActionResult.FAIL;
        }
    }


    @Override
    public void appendTooltip(ItemStack stack, Item.TooltipContext context, List<Text> tooltip, TooltipType options) {
        tooltip.add(Text.translatable("tooltip.raft.energy_staff.tooltip").formatted(Formatting.AQUA));
        super.appendTooltip(stack, context, tooltip, options);
    }

    // Custom method to handle item damage
    private void damageItem(ServerPlayerEntity player, int amount) {
        ItemStack stack = player.getMainHandStack();
        if (!player.isCreative()) {
            if (stack.isDamageable()) {
                stack.damage(amount, player, (entity) -> entity.sendToolBreakStatus(player.getActiveHand()));
                if (stack.isEmpty()) {
                    player.sendToolBreakStatus(player.getActiveHand());
                    player.setStackInHand(player.getActiveHand(), ItemStack.EMPTY);
                }
            }
        }
    }
}
