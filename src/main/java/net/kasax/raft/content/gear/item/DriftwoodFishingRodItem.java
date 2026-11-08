package net.kasax.raft.content.gear.item;

import net.fabricmc.fabric.api.menu.v1.ExtendedMenuProvider;
import net.kasax.raft.content.gear.GearContent;
import net.kasax.raft.content.gear.menu.TackleMenu;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.SlotAccess;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.FishingHook;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickAction;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.FishingRodItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.gameevent.GameEvent;

import java.util.function.Consumer;

/**
 * The Driftwood Fishing Rod: a sturdier rod (128 uses, takes the usual rod enchantments) with a tackle box for one
 * stack of Fish Bait and one stack of Golden Lures ({@link Tackle}). Sneak-use opens the tackle box; in the inventory
 * you can also right-click bait or lures onto the rod (and right-click it with an empty hand to take them out again).
 * Every cast uses one bait (bites 5 seconds sooner, like Lure I) and one lure (better treasure like Luck of the
 * Sea II, and bites 5 seconds sooner) on top of the rod's own enchantments. Unlike a plain rod, it never takes bait
 * from the inventory: it only uses what is loaded.
 */
public class DriftwoodFishingRodItem extends FishingRodItem {
    public static final int BAIT_TICKS = 100;
    public static final int LURE_LUCK = 2;
    /** Bites never come sooner than this after the hook lands (vanilla waits 5 to 30 seconds). */
    private static final int MAX_LURE_SPEED = 450;

    public DriftwoodFishingRodItem(Properties properties) {
        super(properties);
    }

    public static Tackle tackle(ItemStack rod) {
        return rod.getOrDefault(GearContent.TACKLE, Tackle.EMPTY);
    }

    /** The tackle slot this item goes into (0 bait, 1 lure), or -1. */
    public static int slotFor(ItemStack stack) {
        if (stack.is(GearContent.FISH_BAIT)) return 0;
        if (stack.is(GearContent.GOLDEN_LURE)) return 1;
        return -1;
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        ItemStack rod = player.getItemInHand(hand);
        if (player.fishing != null) return super.use(level, player, hand);
        if (player.isSecondaryUseActive()) {
            if (player instanceof ServerPlayer sp) openTackleBox(sp, hand);
            return InteractionResult.SUCCESS;
        }
        level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.FISHING_BOBBER_THROW,
                SoundSource.NEUTRAL, 0.5F, 0.4F / (level.getRandom().nextFloat() * 0.4F + 0.8F));
        if (level instanceof ServerLevel serverLevel) {
            int lureSpeed = (int) (EnchantmentHelper.getFishingTimeReduction(serverLevel, rod, player) * 20.0F);
            int luck = EnchantmentHelper.getFishingLuckBonus(serverLevel, rod, player);
            Tackle tackle = tackle(rod);
            if (!tackle.bait().isEmpty()) lureSpeed += BAIT_TICKS;
            if (!tackle.lure().isEmpty()) {
                lureSpeed += BAIT_TICKS;
                luck += LURE_LUCK;
            }
            if (!tackle.isEmpty() && !player.hasInfiniteMaterials()) rod.set(GearContent.TACKLE, tackle.afterCast());
            Projectile.spawnProjectile(new FishingHook(player, level, luck, Math.min(MAX_LURE_SPEED, lureSpeed)), serverLevel, rod);
        }
        player.awardStat(Stats.ITEM_USED.get(this));
        rod.causeUseVibration(player, GameEvent.ITEM_INTERACT_START);
        return InteractionResult.SUCCESS;
    }

    private static void openTackleBox(ServerPlayer player, InteractionHand hand) {
        int slot = hand == InteractionHand.OFF_HAND ? Inventory.SLOT_OFFHAND : player.getInventory().getSelectedSlot();
        player.openMenu(new ExtendedMenuProvider<Integer>() {
            @Override
            public Integer getScreenOpeningData(ServerPlayer p) {
                return slot;
            }

            @Override
            public Component getDisplayName() {
                return Component.translatable("container.raft.tackle_box");
            }

            @Override
            public AbstractContainerMenu createMenu(int id, Inventory inventory, Player p) {
                return new TackleMenu(id, inventory, slot);
            }
        });
    }

    /** Puts as much of the stack as fits into the rod; returns what is left. */
    private static ItemStack load(ItemStack rod, ItemStack stack) {
        int slot = slotFor(stack);
        if (slot < 0) return stack;
        Tackle tackle = tackle(rod);
        ItemStack in = tackle.get(slot);
        if (!in.isEmpty() && !ItemStack.isSameItemSameComponents(in, stack)) return stack;
        int room = stack.getMaxStackSize() - in.getCount();
        int moved = Math.min(room, stack.getCount());
        if (moved <= 0) return stack;
        ItemStack loaded = stack.copyWithCount(in.getCount() + moved);
        rod.set(GearContent.TACKLE, tackle.with(slot, loaded));
        ItemStack rest = stack.copy();
        rest.shrink(moved);
        return rest;
    }

    @Override
    public boolean overrideOtherStackedOnMe(ItemStack rod, ItemStack carried, Slot slot, ClickAction action, Player player,
                                            SlotAccess carriedAccess) {
        if (action != ClickAction.SECONDARY || !slot.allowModification(player)) return false;
        if (carried.isEmpty()) {
            // right-click with an empty hand: take out the lures first, then the bait
            Tackle tackle = tackle(rod);
            int from = !tackle.lure().isEmpty() ? 1 : !tackle.bait().isEmpty() ? 0 : -1;
            if (from < 0) return false;
            carriedAccess.set(tackle.get(from));
            rod.set(GearContent.TACKLE, tackle.with(from, ItemStack.EMPTY));
            player.playSound(SoundEvents.BUNDLE_REMOVE_ONE, 0.8F, 0.8F + player.level().getRandom().nextFloat() * 0.4F);
            return true;
        }
        if (slotFor(carried) < 0) return false;
        ItemStack rest = load(rod, carried);
        if (rest.getCount() == carried.getCount()) {
            player.playSound(SoundEvents.BUNDLE_INSERT_FAIL, 1.0F, 1.0F);
        } else {
            carriedAccess.set(rest);
            player.playSound(SoundEvents.BUNDLE_INSERT, 0.8F, 0.8F + player.level().getRandom().nextFloat() * 0.4F);
        }
        return true;
    }

    @Override
    public boolean overrideStackedOnOther(ItemStack rod, Slot slot, ClickAction action, Player player) {
        if (action != ClickAction.SECONDARY) return false;
        ItemStack other = slot.getItem();
        if (slotFor(other) < 0 || !slot.allowModification(player)) return false;
        ItemStack rest = load(rod, other);
        if (rest.getCount() != other.getCount()) {
            slot.set(rest);
            player.playSound(SoundEvents.BUNDLE_INSERT, 0.8F, 0.8F + player.level().getRandom().nextFloat() * 0.4F);
        }
        return true;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> builder,
                                TooltipFlag flag) {
        Tackle tackle = tackle(stack);
        if (tackle.isEmpty()) {
            builder.accept(Component.translatable("tooltip.raft.driftwood_fishing_rod.empty").withStyle(ChatFormatting.GRAY));
        }
        if (!tackle.bait().isEmpty()) {
            builder.accept(Component.translatable("tooltip.raft.driftwood_fishing_rod.bait", tackle.bait().getCount())
                    .withStyle(ChatFormatting.YELLOW));
        }
        if (!tackle.lure().isEmpty()) {
            builder.accept(Component.translatable("tooltip.raft.driftwood_fishing_rod.lure", tackle.lure().getCount())
                    .withStyle(ChatFormatting.GOLD));
        }
        builder.accept(Component.translatable("tooltip.raft.driftwood_fishing_rod").withStyle(ChatFormatting.AQUA));
        builder.accept(Component.translatable("tooltip.raft.driftwood_fishing_rod.1").withStyle(ChatFormatting.AQUA));
        super.appendHoverText(stack, context, display, builder, flag);
    }
}
