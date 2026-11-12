package net.kasax.raft.content.npc;

import net.kasax.raft.platform.ServerPlayNetworking;
import net.kasax.raft.Raft;
import net.kasax.raft.content.npc.entity.NpcEntity;
import net.kasax.raft.content.npc.net.NpcDialogPayload;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Prediction;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.alchemy.Potions;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Builds a conversation for one player (server side) and sends it to the client screen. What the NPC says depends on
 * the player's story stage ({@link NpcStory}), whether they met before ({@link NpcContent#MET}) and, for the guides
 * of the story, hands over an item that helps with the next step.
 */
public final class NpcDialogue {
    /** Voss hands out another fire resistance potion at most this often (the lava moat is unforgiving). */
    private static final long REGIFT_TICKS = 6000L;

    private NpcDialogue() {
    }

    public static void open(ServerPlayer player, NpcEntity npc) {
        NpcCharacter c = npc.character();
        int stage = NpcStory.stage(player);
        Map<String, Long> met = NpcContent.MET.getOrElse(player, Map.of());
        boolean first = !met.containsKey(c.id);
        long now = player.level().getGameTime();
        ItemStack gift = gift(c, player, first, met.getOrDefault(c.id, 0L), now, stage);

        String bucket;
        if (first) {
            bucket = c == NpcCharacter.DR_VOSS && gift.isEmpty() ? "first_late" : "first";
        } else if (!gift.isEmpty()) {
            bucket = "regift";
        } else {
            bucket = c.greetingFor(stage);
        }
        Component greeting = Component.translatable(c.key("greet." + bucket));

        Map<String, Long> updated = new HashMap<>(met);
        updated.put(c.id, gift.isEmpty() ? met.getOrDefault(c.id, 0L) : now);
        NpcContent.MET.set(player, updated);
        if (!gift.isEmpty()) {
            give(player, gift.copy());
        }

        List<NpcDialogPayload.Topic> topics = new ArrayList<>();
        topics.add(new NpcDialogPayload.Topic(Component.translatable("npc.raft.ask.place"), List.of(Component.translatable(c.key("place")))));
        topics.add(new NpcDialogPayload.Topic(Component.translatable("npc.raft.ask.next"), List.of(Component.translatable(NpcStory.hintKey(stage)))));
        // all rumours, starting with a random one: asking again tells the next
        List<Component> rumors = new ArrayList<>();
        int start = player.getRandom().nextInt(c.rumors);
        for (int i = 0; i < c.rumors; i++) {
            rumors.add(Component.translatable(c.key("rumor." + (1 + (start + i) % c.rumors))));
        }
        topics.add(new NpcDialogPayload.Topic(Component.translatable("npc.raft.ask.rumor"), rumors));

        ServerPlayNetworking.send(player, new NpcDialogPayload(npc.getId(), c.id, npc.getDisplayName(), greeting, gift,
                topics, NpcTrades.hasTrades(c)));
        npc.startTalking(player);
        award(player, c);
    }

    /** What the character hands over now (empty: nothing). */
    private static ItemStack gift(NpcCharacter c, ServerPlayer player, boolean first, long lastGift, long now, int stage) {
        if (c == NpcCharacter.DR_VOSS) {
            // the potion for the river of fire, again if it was lost, until the Ember Sigil is taken
            boolean needed = stage <= NpcStory.indexOf("ember_sigil") && !hasFireProtection(player);
            return needed && (first || now - lastGift >= REGIFT_TICKS)
                    ? PotionContents.createItemStack(Items.POTION, Potions.LONG_FIRE_RESISTANCE) : ItemStack.EMPTY;
        }
        if (!first) {
            return ItemStack.EMPTY;
        }
        return switch (c) {
            case KEEPER_FENN -> new ItemStack(Items.SPYGLASS);
            case NALU -> PotionContents.createItemStack(Items.POTION, Potions.LONG_WATER_BREATHING);
            case OLD_TAMO -> new ItemStack(Items.COOKED_COD, 3);
            case TEO -> stack("raft:pina_colada", 1);
            case DR_LUND -> new ItemStack(Items.HONEY_BOTTLE);
            case OKAFOR -> stack("raft:circuit_board", 1);
            default -> ItemStack.EMPTY;
        };
    }

    private static boolean hasFireProtection(ServerPlayer player) {
        if (player.hasEffect(MobEffects.FIRE_RESISTANCE)) {
            return true;
        }
        return player.getInventory().contains(stack -> {
            PotionContents contents = stack.get(DataComponents.POTION_CONTENTS);
            if (contents == null) {
                return false;
            }
            for (var effect : contents.getAllEffects()) {
                if (effect.is(MobEffects.FIRE_RESISTANCE)) {
                    return true;
                }
            }
            return false;
        });
    }

    private static ItemStack stack(String id, int count) {
        Identifier ident = Identifier.tryParse(id);
        Item item = ident == null ? Items.AIR : BuiltInRegistries.ITEM.getValue(ident);
        return item == Items.AIR ? ItemStack.EMPTY : new ItemStack(item, count);
    }

    private static void give(ServerPlayer player, ItemStack stack) {
        if (!player.getInventory().add(stack) && !stack.isEmpty()) {
            player.drop(stack, false, Prediction.SERVER_ONLY);
        }
    }

    /** Advancements raft:adventure/new_friend (any person) and raft:adventure/every_soul (one criterion each). */
    private static void award(ServerPlayer player, NpcCharacter c) {
        var advancements = player.level().getServer().getAdvancements();
        AdvancementHolder friend = advancements.get(Raft.id("adventure/new_friend"));
        if (friend != null) {
            player.getAdvancements().award(friend, "talk");
        }
        AdvancementHolder all = advancements.get(Raft.id("adventure/every_soul"));
        if (all != null) {
            player.getAdvancements().award(all, c.id);
        }
    }
}
