package net.kasax.raft.content.encounters;

import net.kasax.raft.content.ContentModule;

/**
 * Beach Craft 3.2 module "encounters": the guards of the structures and their enemies (scavengers, reef guardians,
 * the corsair captain and the elites), so the way to the Leviathan gets harder step by step. See StructureEncounters
 * for the difficulty curve.
 */
public class EncountersModule implements ContentModule {
    @Override
    public String name() {
        return "encounters";
    }

    @Override
    public void init() {
        EncounterSounds.register();
        EncounterEntities.register();
        EncounterItems.register();
        StructureEncounters.register();
    }

    @Override
    public void initClient() {
        net.kasax.raft.content.encounters.client.EncountersClient.init();
    }
}
