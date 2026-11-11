package net.kasax.raft.content.marine;

import net.kasax.raft.platform.GameRuleFactory;
import net.kasax.raft.platform.GameRuleRegistry;
import net.minecraft.world.level.GameRules;
import net.kasax.raft.Raft;
import net.minecraft.server.level.ServerLevel;

/**
 * Game rules of the marine module. 26.3 game rules are registry entries with lower-case ids, so the rule is
 * {@code /gamerule raft:shark_bites true|false}.
 */
public final class MarineGameRules {
    /** Sharks may bite and break the edge {@code raft:raft_foundation} blocks of a raft a player stands on. */
    public static GameRules.Key<GameRules.BooleanValue> SHARK_BITES;

    private MarineGameRules() {
    }

    static void register() {
        SHARK_BITES = GameRuleRegistry.register(Raft.id("shark_bites").toString(), GameRules.Category.MOBS, GameRuleFactory.createBooleanRule(true));
    }

    public static boolean sharkBites(ServerLevel level) {
        return SHARK_BITES == null || level.getGameRules().getBoolean(SHARK_BITES);
    }
}
