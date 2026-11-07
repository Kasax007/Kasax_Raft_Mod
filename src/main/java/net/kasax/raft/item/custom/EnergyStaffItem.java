package net.kasax.raft.item.custom;

import net.kasax.raft.block.ModBlocks;
import net.kasax.raft.world.portal.RaftPortalShape;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

import java.util.Optional;
import java.util.function.Consumer;

/**
 * The energy staff lights a rectangular driftwood portal frame (at least 2x3 inside) and
 * opens the portal to the raft dimension. It only works in the overworld.
 */
public class EnergyStaffItem extends Item {
    public EnergyStaffItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        Player user = context.getPlayer();
        BlockPos pos = context.getClickedPos();
        BlockState state = level.getBlockState(pos);

        if (state.is(ModBlocks.DRIFTWOOD_PORTAL_FRAME)) {
            // the overworld side, and the raft side too (to repair a broken return portal)
            if (level.dimension() != Level.OVERWORLD && level.dimension() != net.kasax.raft.world.dimension.ModDimensions.RAFTDIM_LEVEL_KEY) {
                if (!level.isClientSide() && user != null) {
                    user.displayClientMessage(Component.translatable("chat.raft.energy_staff.overworld_only").withStyle(ChatFormatting.BLUE), true);
                }
                return InteractionResult.FAIL;
            }

            // Look for an empty frame next to the clicked frame block: clicking any side of the frame works.
            Optional<RaftPortalShape> shape = Optional.empty();
            Direction clickedFace = context.getClickedFace();
            Direction[] order = {clickedFace, Direction.UP, Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST};
            for (Direction dir : order) {
                if (dir == Direction.DOWN) continue;
                BlockPos inside = pos.relative(dir);
                shape = RaftPortalShape.findEmptyPortalShape(level, inside, Direction.Axis.X);
                if (shape.isPresent()) break;
            }

            if (shape.isPresent()) {
                if (!level.isClientSide()) {
                    shape.get().createPortalBlocks(level);
                    level.playSound(null, pos, SoundEvents.LODESTONE_COMPASS_LOCK, SoundSource.NEUTRAL, 1.0F,
                            1F / (level.getRandom().nextFloat() * 0.4F + 0.8F));
                    if (user != null) {
                        user.getCooldowns().addCooldown(context.getItemInHand().getItem(), 20);
                        context.getItemInHand().hurtAndBreak(1, user, net.minecraft.world.entity.LivingEntity.getSlotForHand(context.getHand()));
                    }
                }
                return InteractionResult.SUCCESS;
            }

            if (!level.isClientSide() && user != null) {
                user.displayClientMessage(Component.translatable("chat.raft.energy_staff.no_portal").withStyle(ChatFormatting.BLUE), true);
            }
            return InteractionResult.FAIL;
        } else {
            // Display a message for other blocks
            if (!level.isClientSide() && user != null) {
                user.displayClientMessage(Component.translatable("chat.raft.energy_staff.invalid_target").withStyle(ChatFormatting.BLUE), true);
            }
            return InteractionResult.FAIL;
        }
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, java.util.List<net.minecraft.network.chat.Component> builderList121, TooltipFlag flag) {
        java.util.function.Consumer<net.minecraft.network.chat.Component> builder = builderList121::add;
        builder.accept(Component.translatable("tooltip.raft.energy_staff.tooltip").withStyle(ChatFormatting.AQUA));
        super.appendHoverText(stack, context, builderList121, flag);
    }
}
