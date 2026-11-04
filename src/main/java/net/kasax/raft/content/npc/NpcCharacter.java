package net.kasax.raft.content.npc;

import java.util.List;
import java.util.Locale;

/**
 * The people of the Endless Sea (docs/design/npcs.md). Every character is its own entity type {@code raft:<id>} that
 * shares {@link net.kasax.raft.content.npc.entity.NpcEntity}; this enum holds what makes them different.
 *
 * <p>Texts: {@code entity.raft.<id>} (name), {@code npc.raft.<id>.title}, {@code npc.raft.<id>.greet.<bucket>},
 * {@code npc.raft.<id>.place}, {@code npc.raft.<id>.rumor.<1..rumors>}, {@code npc.raft.<id>.hurt}
 * (written by tools/content/npc/texts.py).
 */
public enum NpcCharacter {
    /** Old Lighthouse: the keeper. Points to Saltmere, gives his spyglass, sells lamp supplies for fish. */
    KEEPER_FENN("keeper_fenn", false, 8, Idle.GAZE, 3, "0:at,2:visited,3:after",
            List.of("raft:chart_table", "minecraft:bookshelf", "raft:expedition_radio", "minecraft:smoker",
                    "minecraft:barrel", "minecraft:campfire", "raft:driftwood_table"),
            "minecraft:air"),
    /** Saltmere, Nana Kalea's house: her grandson. Points to the shrine and Helix, gives a water breathing potion. */
    NALU("nalu", false, 7, Idle.WORK, 3, "0:at,3:after,4:later",
            List.of("raft:paper_lantern_red", "raft:tiki_torch", "raft:aquarium", "raft:deck_chair", "raft:wind_chime",
                    "raft:palm_table", "raft:expedition_radio", "raft:chart_table"),
            "minecraft:air"),
    /** Saltmere docks: the fisherman. Buys the catch, sells fishing goods. */
    OLD_TAMO("old_tamo", false, 3, Idle.FISH, 3, "0:default",
            List.of(),
            "minecraft:fishing_rod"),
    /** Ashen Forge: the scientist who cracked the seal. Gives fire resistance for the lava moat, points to the temple. */
    DR_VOSS("dr_voss", true, 6, Idle.READ, 3, "0:at,9:after,11:key",
            List.of("minecraft:anvil", "minecraft:chipped_anvil", "minecraft:cauldron", "minecraft:iron_block",
                    "raft:glowing_tidestone", "minecraft:chest", "minecraft:copper_block"),
            "minecraft:writable_book"),
    /** Beach bar (tiki bar variant): the barkeeper waiting for Ana. Drinks and island food. */
    TEO("teo", false, 4, Idle.WORK, 3, "0:default,6:solace",
            List.of("minecraft:barrel", "raft:rum_bottles", "raft:simple_grill", "raft:wooden_crate", "raft:water_purifier",
                    "raft:palm_table"),
            "minecraft:glass_bottle"),
    /** Castaway camp (tent variant): the castaway who wants biscuits. Barters flotsam for cookies. */
    KAI("kai", false, 6, Idle.SIT, 3, "0:default",
            List.of(),
            "minecraft:air"),
    /** Iceberg Outpost: the polar researcher. Buys sea creature specimens, sells Expedition gear. */
    DR_LUND("dr_lund", true, 3, Idle.WORK, 3, "0:default",
            List.of("minecraft:smoker", "raft:research_terminal", "raft:expedition_radio", "raft:server_rack",
                    "raft:chart_table", "minecraft:barrel"),
            "minecraft:honey_bottle"),
    /** Rusted Rig: the last engineer of Rig Seven. Sells technology parts, buys metal. */
    OKAFOR("okafor", true, 4, Idle.WORK, 3, "0:default",
            List.of("raft:control_panel", "raft:server_rack", "raft:research_terminal", "minecraft:furnace",
                    "minecraft:smoker", "minecraft:barrel"),
            "raft:circuit_board");

    /** How a character spends the time when nobody talks to them. */
    public enum Idle {
        /** Walks between the places of interest and works there (arm swings). */
        WORK,
        /** Like WORK, now and then stops to watch the sea through a spyglass. */
        GAZE,
        /** Stays at the pier edge, casts and reels in. */
        FISH,
        /** Walks between the places of interest and reads notes there (head bowed). */
        READ,
        /** Sits by the camp fire most of the time, gets up to stretch the legs. */
        SIT
    }

    public final String id;
    /** Slim (Alex) arms. */
    public final boolean slim;
    /** Wander radius around the home block. */
    public final int homeRadius;
    public final Idle idle;
    /** Number of rumour lines ({@code npc.raft.<id>.rumor.1..n}). */
    public final int rumors;
    /** Story stage (index into {@link NpcStory#CHAPTERS}) from which a greeting bucket applies, ascending. */
    private final int[] stageFrom;
    private final String[] stageKey;
    /** Block ids the character likes to walk to (missing ones are ignored). */
    public final List<String> places;
    /** Item held in the main hand (id; air = empty hands). */
    public final String heldItem;

    NpcCharacter(String id, boolean slim, int homeRadius, Idle idle, int rumors, String stages, List<String> places,
                 String heldItem) {
        this.id = id;
        this.slim = slim;
        this.homeRadius = homeRadius;
        this.idle = idle;
        this.rumors = rumors;
        String[] parts = stages.split(",");
        this.stageFrom = new int[parts.length];
        this.stageKey = new String[parts.length];
        for (int i = 0; i < parts.length; i++) {
            String[] kv = parts[i].split(":");
            this.stageFrom[i] = Integer.parseInt(kv[0]);
            this.stageKey[i] = kv[1];
        }
        this.places = places;
        this.heldItem = heldItem;
    }

    /** The greeting bucket for a player at the given story stage ({@link NpcStory#DONE} = story finished). */
    public String greetingFor(int stage) {
        if (stage >= NpcStory.DONE) {
            return "done";
        }
        String key = this.stageKey[0];
        for (int i = 0; i < this.stageFrom.length; i++) {
            if (stage >= this.stageFrom[i]) {
                key = this.stageKey[i];
            }
        }
        return key;
    }

    public String key(String suffix) {
        return "npc.raft." + this.id + "." + suffix;
    }

    public static NpcCharacter byId(String id) {
        for (NpcCharacter c : values()) {
            if (c.id.equals(id)) {
                return c;
            }
        }
        return null;
    }

    @Override
    public String toString() {
        return this.name().toLowerCase(Locale.ROOT);
    }
}
