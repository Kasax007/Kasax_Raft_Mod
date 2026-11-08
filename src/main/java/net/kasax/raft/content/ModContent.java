package net.kasax.raft.content;

import net.kasax.raft.Raft;

import java.util.List;

/** The list of Beach Craft 3 content modules, in init order. */
public final class ModContent {
    public static final List<ContentModule> MODULES = List.of(
            new net.kasax.raft.content.world.WorldModule(),
            new net.kasax.raft.content.building.BuildingModule(),
            new net.kasax.raft.content.decor.DecorModule(),
            new net.kasax.raft.content.shore.ShoreModule(),
            new net.kasax.raft.content.marine.MarineModule(),
            new net.kasax.raft.content.voyage.VoyageModule(),
            new net.kasax.raft.content.vessel.VesselModule(),
            new net.kasax.raft.content.diving.DivingModule(),
            new net.kasax.raft.content.story.StoryModule(),
            new net.kasax.raft.content.boss.BossModule(),
            new net.kasax.raft.content.guide.GuideModule(),
            new net.kasax.raft.content.audio.AudioModule(),
            new net.kasax.raft.content.npc.NpcModule(),
            new net.kasax.raft.content.encounters.EncountersModule(),
            // Beach Craft 3.3
            new net.kasax.raft.content.cozy.CozyModule(),
            new net.kasax.raft.content.food.FoodModule(),
            new net.kasax.raft.content.gear.GearModule());

    private ModContent() {
    }

    public static void init() {
        for (ContentModule module : MODULES) {
            Raft.LOGGER.info("Beach Craft: init module {}", module.name());
            module.init();
        }
    }

    public static void initClient() {
        for (ContentModule module : MODULES) {
            module.initClient();
        }
    }
}
