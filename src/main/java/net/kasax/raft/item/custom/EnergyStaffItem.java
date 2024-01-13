package net.kasax.raft.item.custom;

import net.kasax.raft.block.ModBlocks;
import net.minecraft.block.BlockState;
import net.minecraft.client.item.TooltipContext;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUsageContext;
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
                world.playSound((PlayerEntity) null, user.getX(), user.getY(), user.getZ(),
                        ITEM_LODESTONE_COMPASS_LOCK, SoundCategory.NEUTRAL, 20F,
                        1F / (world.getRandom().nextFloat() * 0.4F + 0.8F));
            }
            return ActionResult.SUCCESS;
        } else {
            // Display a message in the chat for other blocks
            if (!world.isClient() && user != null) {
                user.sendMessage(Text.translatable("chat.raft.energy_staff.invalid_target").formatted(Formatting.BLUE), true);
            }
            return ActionResult.FAIL;
        }
    }


    @Override
    public void appendTooltip(ItemStack stack, @Nullable World world, List<Text> tooltip, TooltipContext context) {
        tooltip.add(Text.translatable("tooltip.raft.energy_staff.tooltip").formatted(Formatting.AQUA));
        super.appendTooltip(stack, world, tooltip, context);
    }
}
