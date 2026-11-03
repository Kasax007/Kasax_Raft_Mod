package net.kasax.raft.content.voyage;

/** The upgrades of the Expedition Raft. Each one is a bit in the raft's upgrade mask (and the item component). */
public enum RaftUpgrade {
    SAIL("raft_sail"),
    ENGINE("raft_engine"),
    ANCHOR("raft_anchor"),
    CRATE("raft_crate"),
    LANTERN("raft_lantern");

    public final String itemId;

    RaftUpgrade(String itemId) {
        this.itemId = itemId;
    }

    public int bit() {
        return 1 << this.ordinal();
    }

    public boolean in(int mask) {
        return (mask & this.bit()) != 0;
    }

    /** Lang key of the upgrade's short name, e.g. {@code upgrade.raft.raft_sail}. */
    public String langKey() {
        return "upgrade.raft." + this.itemId;
    }

    public static final int ALL = (1 << values().length) - 1;
}
