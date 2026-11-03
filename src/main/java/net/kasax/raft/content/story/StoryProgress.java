package net.kasax.raft.content.story;

import net.kasax.raft.Raft;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.levelgen.structure.Structure;

/**
 * The chapters of "Tides of the Deep" in story order, read from the player's {@code raft:story/*} advancements
 * (docs/design/beachcraft3.md, 9.5). The Expedition Journal shows the first chapter that is not done.
 */
public final class StoryProgress {
    /** Advancement paths {@code raft:story/<id>} in story order. */
    public static final String[] CHAPTERS = {"lighthouse", "saltmere", "coral_sigil", "helix", "pearl_sigil",
            "port_solace", "storm_sigil", "ashen_forge", "ember_sigil", "coral_temple", "tidewarden_key",
            "abyssal_sanctum", "leviathan"};
    /** The place a chapter happens at (map tag {@code raft:on_<place>_maps}, map name {@code filled_map.raft.<place>}). */
    public static final String[] PLACES = {"lighthouse", "saltmere", "saltmere", "helix", "helix", "port_solace",
            "port_solace", "ashen_forge", "ashen_forge", "coral_temple", "coral_temple", "abyssal_sanctum",
            "abyssal_sanctum"};
    /** The chapter of the story (docs/design/beachcraft3.md, 3.2) each step belongs to; chapter 1 is the castaway. */
    public static final int[] CHAPTER_NUMBER = {2, 3, 3, 4, 4, 5, 5, 6, 6, 7, 7, 8, 8};
    public static final int DONE = CHAPTERS.length;

    private StoryProgress() {
    }

    public static boolean isDone(ServerPlayer player, String chapter) {
        AdvancementHolder holder = player.level().getServer().getAdvancements().get(Raft.id("story/" + chapter));
        return holder != null && player.getAdvancements().getOrStartProgress(holder).isDone();
    }

    /**
     * Bit i set = chapter i done. A chapter also counts as done when a later one is done (a player who found Port
     * Solace has obviously been to Helix), except the sigils: those count as done only once the key is forged.
     */
    public static int doneMask(ServerPlayer player) {
        int raw = 0;
        for (int i = 0; i < CHAPTERS.length; i++) {
            if (isDone(player, CHAPTERS[i])) raw |= 1 << i;
        }
        int key = indexOf("tidewarden_key");
        int mask = raw;
        for (int i = 0; i < CHAPTERS.length; i++) {
            boolean sigil = CHAPTERS[i].endsWith("_sigil");
            for (int k = i + 1; k < CHAPTERS.length; k++) {
                if ((raw & (1 << k)) != 0 && (!sigil || k >= key)) {
                    mask |= 1 << i;
                    break;
                }
            }
        }
        return mask;
    }

    public static int indexOf(String chapter) {
        for (int i = 0; i < CHAPTERS.length; i++) {
            if (CHAPTERS[i].equals(chapter)) return i;
        }
        return -1;
    }

    /** Index of the current chapter (first one not done), {@link #DONE} when the story is complete. */
    public static int stage(int mask) {
        for (int i = 0; i < CHAPTERS.length; i++) {
            if ((mask & (1 << i)) == 0) return i;
        }
        return DONE;
    }

    public static TagKey<Structure> mapTag(String place) {
        return TagKey.create(Registries.STRUCTURE, Raft.id("on_" + place + "_maps"));
    }
}
