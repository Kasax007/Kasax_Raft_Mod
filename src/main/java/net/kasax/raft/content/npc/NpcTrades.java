package net.kasax.raft.content.npc;

import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.item.trading.ItemCost;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.item.trading.MerchantOffers;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.BiFunction;

/**
 * The fixed trade lists of the people of the sea. The currency is pirate gold ({@code raft:gold_coin}, from corsairs,
 * wrecks and treasure) and pearls ({@code raft:pearl}, from giant clams and sea otters). Items are looked up by id, so
 * a trade whose item does not exist is simply left out.
 */
public final class NpcTrades {
    private static final String COIN = "raft:gold_coin";
    private static final String PEARL = "raft:pearl";

    /** One offer: costA (+ optional costB) for the result. modify may change the result (enchantments). */
    record Trade(String costA, int countA, String costB, int countB, String result, int count, int maxUses,
                 BiFunction<ItemStack, ServerLevel, ItemStack> modify) {
    }

    private NpcTrades() {
    }

    private static Trade sell(int price, String result, int count, int maxUses) {
        return new Trade(COIN, price, null, 0, result, count, maxUses, null);
    }

    private static Trade sellForPearls(int price, String result, int count, int maxUses) {
        return new Trade(PEARL, price, null, 0, result, count, maxUses, null);
    }

    private static Trade buy(String item, int count, int coins, int maxUses) {
        return new Trade(item, count, null, 0, COIN, coins, maxUses, null);
    }

    private static Trade barter(String cost, int countA, String result, int count, int maxUses) {
        return new Trade(cost, countA, null, 0, result, count, maxUses, null);
    }

    public static boolean hasTrades(NpcCharacter c) {
        return !list(c).isEmpty();
    }

    static List<Trade> list(NpcCharacter c) {
        return switch (c) {
            case KEEPER_FENN -> List.of(
                    buy("minecraft:cod", 6, 1, 16),
                    buy("minecraft:salmon", 4, 1, 16),
                    sell(1, "minecraft:candle", 4, 12),
                    sell(2, "minecraft:lantern", 1, 8),
                    sell(4, "raft:ship_lantern", 1, 4),
                    sell(3, "minecraft:glowstone_dust", 4, 8),
                    sell(6, "minecraft:spyglass", 1, 2));
            case NALU -> List.of(
                    new Trade("raft:seashell", 8, null, 0, PEARL, 1, 8, null),
                    sellForPearls(1, "raft:woven_rug", 3, 8),
                    sellForPearls(1, "raft:paper_lantern_cyan", 2, 8),
                    sellForPearls(1, "raft:paper_lantern_red", 2, 8),
                    sellForPearls(1, "raft:hibiscus", 3, 8),
                    sellForPearls(2, "raft:wind_chime", 1, 4),
                    sellForPearls(2, "raft:seashell_lamp", 1, 4),
                    sellForPearls(3, "raft:jelly_lamp", 1, 4),
                    sellForPearls(4, "raft:diving_helmet", 1, 1));
            case OLD_TAMO -> List.of(
                    buy("minecraft:cod", 6, 1, 16),
                    buy("minecraft:salmon", 4, 1, 16),
                    buy("minecraft:tropical_fish", 3, 1, 12),
                    buy("minecraft:pufferfish", 2, 1, 12),
                    buy("raft:crab_claw", 4, 1, 12),
                    buy("raft:eel_fillet", 3, 1, 12),
                    sell(1, "minecraft:cooked_cod", 5, 12),
                    new Trade(COIN, 3, null, 0, "minecraft:fishing_rod", 1, 3, NpcTrades::luckOfTheSea),
                    sell(2, "minecraft:sea_pickle", 4, 8),
                    sell(2, "raft:glow_kelp", 6, 8),
                    sell(5, "raft:palm_boat", 1, 2),
                    new Trade(COIN, 6, PEARL, 1, "minecraft:nautilus_shell", 1, 2, null));
            case DR_VOSS -> List.of();
            case TEO -> List.of(
                    sell(2, "raft:pina_colada", 1, 8),
                    sell(1, "raft:coconut_milk", 2, 8),
                    sell(1, "raft:fresh_water", 3, 12),
                    sell(2, "raft:cooked_crab_claw", 4, 8),
                    sell(1, "raft:pineapple", 2, 8),
                    buy("raft:pineapple", 3, 1, 12),
                    buy("raft:coconut", 4, 1, 12),
                    buy("raft:rum_bottles", 1, 3, 4),
                    sell(4, "raft:deck_chair", 1, 4));
            case KAI -> List.of(
                    barter("minecraft:cookie", 4, "raft:message_in_a_bottle", 1, 6),
                    barter("minecraft:cookie", 2, "raft:rope", 4, 8),
                    barter("minecraft:cookie", 2, "raft:plastic", 6, 8),
                    barter("minecraft:cookie", 3, "raft:scrap_metal", 4, 8),
                    barter("minecraft:bread", 1, "raft:coconut", 3, 8),
                    barter("minecraft:cookie", 5, "raft:supply_barrel", 1, 2),
                    buy("raft:net", 1, 3, 1));
            case DR_LUND -> List.of(
                    buy("raft:jelly_blob", 2, 1, 12),
                    buy("raft:glow_jelly", 1, 1, 12),
                    buy("raft:angler_lure", 1, 2, 8),
                    buy("raft:shark_tooth", 2, 1, 12),
                    new Trade("raft:leviathan_scale", 1, null, 0, PEARL, 2, 4, null),
                    sell(2, "raft:harpoon", 4, 8),
                    sell(4, "raft:wetsuit", 1, 2),
                    sell(4, "raft:flippers", 1, 2),
                    sell(6, "raft:oxygen_tank", 1, 2),
                    sell(1, "minecraft:blue_ice", 2, 8));
            case OKAFOR -> List.of(
                    buy("minecraft:copper_ingot", 4, 1, 12),
                    buy("minecraft:iron_ingot", 3, 1, 12),
                    buy("raft:plastic", 8, 1, 12),
                    sell(1, "raft:scrap_metal", 4, 12),
                    sell(1, "minecraft:redstone", 8, 8),
                    sell(2, "raft:copper_cable", 8, 8),
                    sell(3, "raft:circuit_board", 1, 8),
                    sell(4, "raft:titanium_ingot", 1, 4),
                    sell(6, "raft:makeshift_battery", 1, 2));
        };
    }

    /** The offers of a character in list order; trades with unknown items are left out. */
    public static MerchantOffers build(NpcCharacter c, ServerLevel level) {
        MerchantOffers offers = new MerchantOffers();
        for (Trade t : list(c)) {
            Item a = item(t.costA()), r = item(t.result());
            Item b = t.costB() == null ? null : item(t.costB());
            if (a == null || r == null || (t.costB() != null && b == null)) {
                continue;
            }
            ItemStack result = new ItemStack(r, t.count());
            if (t.modify() != null) {
                result = t.modify().apply(result, level);
            }
            Optional<ItemCost> costB = b == null ? Optional.empty() : Optional.of(new ItemCost(b, t.countB()));
            offers.add(new MerchantOffer(new ItemCost(a, t.countA()), costB, result, t.maxUses(), 2, 0.05F));
        }
        return offers;
    }

    private static Item item(String id) {
        Identifier ident = Identifier.tryParse(id);
        if (ident == null) {
            return null;
        }
        Item item = BuiltInRegistries.ITEM.getValue(ident);
        return item == Items.AIR ? null : item;
    }

    private static ItemStack luckOfTheSea(ItemStack stack, ServerLevel level) {
        Optional<Holder.Reference<Enchantment>> luck = level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT)
                .get(Enchantments.LUCK_OF_THE_SEA);
        luck.ifPresent(holder -> stack.enchant(holder, 2));
        return stack;
    }
}
