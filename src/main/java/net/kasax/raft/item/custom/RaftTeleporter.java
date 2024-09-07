package net.kasax.raft.item.custom;

import net.kasax.raft.util.TeleporterUtil;
import net.kasax.raft.world.dimension.ModDimensions;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.registry.RegistryKey;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.world.World;

import java.util.List;

public class RaftTeleporter extends Item {
    public RaftTeleporter(Settings settings) {
        super(settings);
    }
    private RegistryKey<World> dimKey = ModDimensions.RAFTDIM_LEVEL_KEY;
    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity player, Hand hand)
    {
        ItemStack stack = player.getStackInHand(hand);
        if (!player.isCreative()) {
            stack.damage(1, (LivingEntity) world.random, null);
        }
        TeleporterUtil.movePlayer(dimKey, world, player, hand);
        player.getItemCooldownManager().set(this, 20);
        return TypedActionResult.success(stack);
    }
    @Override
    public void appendTooltip(ItemStack stack, TooltipContext context, List<Text> tooltip, TooltipType options) {
        tooltip.add(Text.translatable("tooltip.raft.raft_teleporter.tooltip").formatted(Formatting.AQUA));
        super.appendTooltip(stack, context, tooltip, options);
    }
}
