package net.kasax.raft.content.story.item;

import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.kasax.raft.content.story.StoryContent;
import net.kasax.raft.content.story.StoryProgress;
import net.kasax.raft.content.story.StoryUtil;
import net.kasax.raft.content.story.net.OpenJournalPayload;
import net.kasax.raft.world.dimension.ModDimensions;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.MapItem;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.maps.MapDecorationTypes;
import net.minecraft.world.level.saveddata.maps.MapItemSavedData;

import java.util.function.Consumer;

/**
 * Captain Marlow's Expedition Journal. Use: opens the journal screen with the current story goal (from the player's
 * {@code raft:story/*} advancements). Sneak-use: draws an exploration chart to the place of the current chapter
 * (like the vanilla exploration map loot function), then needs 5 minutes until the next chart. The wait is stored on
 * the stack (game time) and shown as a bar, so normal use keeps working meanwhile.
 */
public class ExpeditionJournalItem extends Item {
    public static final int MAP_COOLDOWN = 6000;
    public static final int SEARCH_RADIUS = 100;
    private static final String READY_AT = "raft_chart_ready";
    /** Game time of the client world, set every tick by the client module (bar and tooltip need it). */
    public static volatile long clientGameTime;

    public ExpeditionJournalItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!(player instanceof ServerPlayer sp)) {
            return InteractionResult.SUCCESS;
        }
        if (player.isShiftKeyDown()) {
            drawChart(sp, stack);
        } else {
            open(sp, stack);
        }
        return InteractionResult.SUCCESS;
    }

    public static void open(ServerPlayer player, ItemStack stack) {
        int mask = StoryProgress.doneMask(player);
        int sigils = 0;
        String[] slots = {"coral", "pearl", "storm", "ember"};
        String[] chapters = {"coral_sigil", "pearl_sigil", "storm_sigil", "ember_sigil"};
        boolean key = (mask & (1 << StoryProgress.indexOf("tidewarden_key"))) != 0 || player.getInventory().contains(s -> s.is(StoryContent.TIDEWARDEN_KEY));
        for (int i = 0; i < 4; i++) {
            net.minecraft.world.item.Item sigil = StoryContent.sigil(slots[i]);
            boolean has = player.getInventory().contains(s -> s.is(sigil));
            int bit = StoryProgress.indexOf(chapters[i]);
            if (has || key || (mask & (1 << bit)) != 0) sigils |= 1 << i;
        }
        long wait = Math.max(0, readyAt(stack) - player.level().getGameTime());
        player.level().playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.BOOK_PAGE_TURN,
                SoundSource.PLAYERS, 1.0F, 1.0F);
        ServerPlayNetworking.send(player, new OpenJournalPayload(mask, sigils, (int) Math.min(Integer.MAX_VALUE, wait),
                player.level().dimension() == ModDimensions.RAFTDIM_LEVEL_KEY));
    }

    private static void drawChart(ServerPlayer player, ItemStack stack) {
        ServerLevel level = player.level();
        int stage = StoryProgress.stage(StoryProgress.doneMask(player));
        if (stage == StoryProgress.DONE) {
            StoryUtil.tell(player, "journal.done");
            return;
        }
        if (level.dimension() != ModDimensions.RAFTDIM_LEVEL_KEY) {
            StoryUtil.tell(player, "journal.not_sea");
            return;
        }
        long now = level.getGameTime();
        long ready = readyAt(stack);
        if (now < ready) {
            StoryUtil.tell(player, "journal.wait", StoryUtil.time(ready - now));
            return;
        }
        String place = StoryProgress.PLACES[stage];
        BlockPos target = level.findNearestMapStructure(StoryProgress.mapTag(place), player.blockPosition(), SEARCH_RADIUS, false);
        if (target == null) {
            StoryUtil.tell(player, "journal.no_place");
            level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.BOOK_PAGE_TURN,
                    SoundSource.PLAYERS, 0.8F, 0.6F);
            return;
        }
        ItemStack chart = MapItem.create(level, target.getX(), target.getZ(), (byte) 2, true, true);
        MapItem.renderBiomePreviewMap(level, chart);
        MapItemSavedData.addTargetDecoration(chart, target, "+", MapDecorationTypes.TARGET_X);
        chart.set(DataComponents.ITEM_NAME, Component.translatable("filled_map.raft." + place));
        StoryUtil.give(player, chart);
        CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> tag.putLong(READY_AT, now + MAP_COOLDOWN));
        level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.UI_CARTOGRAPHY_TABLE_TAKE_RESULT,
                SoundSource.PLAYERS, 1.0F, 1.0F);
        StoryUtil.tell(player, "journal.drawn");
    }

    public static long readyAt(ItemStack stack) {
        CustomData data = stack.get(DataComponents.CUSTOM_DATA);
        if (data == null) return 0L;
        CompoundTag tag = data.copyTag();
        return tag.getLongOr(READY_AT, 0L);
    }

    private static long clientWait(ItemStack stack) {
        return Math.max(0, readyAt(stack) - clientGameTime);
    }

    @Override
    public boolean isBarVisible(ItemStack stack) {
        long wait = clientWait(stack);
        return wait > 0 && wait <= MAP_COOLDOWN;
    }

    @Override
    public int getBarWidth(ItemStack stack) {
        return Math.round(13.0F * (1.0F - clientWait(stack) / (float) MAP_COOLDOWN));
    }

    @Override
    public int getBarColor(ItemStack stack) {
        return 0x4FD0C8;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> builder,
                                TooltipFlag flag) {
        builder.accept(Component.translatable("tooltip.raft.expedition_journal.0").withStyle(ChatFormatting.GRAY));
        builder.accept(Component.translatable("tooltip.raft.expedition_journal.1").withStyle(ChatFormatting.GRAY));
        long wait = clientWait(stack);
        if (wait > 0 && wait <= MAP_COOLDOWN) {
            builder.accept(Component.translatable("tooltip.raft.expedition_journal.wait", StoryUtil.time(wait))
                    .withStyle(ChatFormatting.DARK_AQUA));
        }
    }
}
