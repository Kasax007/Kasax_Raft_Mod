package net.kasax.raft.content.npc;

import net.kasax.raft.Raft;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.server.level.ServerPlayer;

/**
 * Where a player stands in "Tides of the Deep", read from the {@code raft:story/*} advancements (same rules as the
 * Expedition Journal of the story module, looked up by id so the modules stay independent).
 */
public final class NpcStory {
    /** Advancement paths {@code raft:story/<id>} in story order (docs/design/beachcraft3.md, 9.5). */
    public static final String[] CHAPTERS = {"lighthouse", "saltmere", "coral_sigil", "helix", "pearl_sigil",
            "port_solace", "storm_sigil", "ashen_forge", "ember_sigil", "coral_temple", "tidewarden_key",
            "abyssal_sanctum", "leviathan"};
    public static final int DONE = CHAPTERS.length;

    private NpcStory() {
    }

    public static boolean isDone(ServerPlayer player, String chapter) {
        AdvancementHolder holder = player.level().getServer().getAdvancements().get(Raft.id("story/" + chapter));
        return holder != null && player.getAdvancements().getOrStartProgress(holder).isDone();
    }

    /**
     * The current story stage: index of the first chapter not done ({@link #DONE} when the Leviathan is defeated).
     * A chapter also counts as done when a later one is done, except the sigils: those count only once the key is
     * forged (a player can visit Port Solace without having taken the Pearl Sigil).
     */
    public static int stage(ServerPlayer player) {
        int raw = 0;
        for (int i = 0; i < CHAPTERS.length; i++) {
            if (isDone(player, CHAPTERS[i])) {
                raw |= 1 << i;
            }
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
        for (int i = 0; i < CHAPTERS.length; i++) {
            if ((mask & (1 << i)) == 0) {
                return i;
            }
        }
        return DONE;
    }

    public static int indexOf(String chapter) {
        for (int i = 0; i < CHAPTERS.length; i++) {
            if (CHAPTERS[i].equals(chapter)) {
                return i;
            }
        }
        return -1;
    }

    /** Lang key of the shared hint for the next step ({@code npc.raft.hint.<chapter>} or {@code npc.raft.hint.done}). */
    public static String hintKey(int stage) {
        return "npc.raft.hint." + (stage >= DONE ? "done" : CHAPTERS[stage]);
    }
}
