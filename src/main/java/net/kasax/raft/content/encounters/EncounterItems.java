package net.kasax.raft.content.encounters;

import net.kasax.raft.content.BcReg;
import net.kasax.raft.content.BcTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.SpawnEggItem;

/** Spawn eggs of the encounter enemies (creative tab Creatures). */
public final class EncounterItems {
    public static Item SCAVENGER_SPAWN_EGG;
    public static Item REEF_GUARDIAN_SPAWN_EGG;
    public static Item CORSAIR_CAPTAIN_SPAWN_EGG;

    private EncounterItems() {
    }

    public static void register() {
        BcTabs.Tab tab = BcTabs.Tab.CREATURES;
        SCAVENGER_SPAWN_EGG = BcReg.item("scavenger_spawn_egg", SpawnEggItem::new,
                new Item.Properties().spawnEgg(EncounterEntities.SCAVENGER), tab);
        REEF_GUARDIAN_SPAWN_EGG = BcReg.item("reef_guardian_spawn_egg", SpawnEggItem::new,
                new Item.Properties().spawnEgg(EncounterEntities.REEF_GUARDIAN), tab);
        CORSAIR_CAPTAIN_SPAWN_EGG = BcReg.item("corsair_captain_spawn_egg", SpawnEggItem::new,
                new Item.Properties().spawnEgg(EncounterEntities.CORSAIR_CAPTAIN), tab);
    }
}
