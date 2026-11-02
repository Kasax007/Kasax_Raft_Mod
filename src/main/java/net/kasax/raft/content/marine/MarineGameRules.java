package net.kasax.raft.content.marine;

import net.fabricmc.fabric.api.gamerule.v1.GameRuleBuilder;
import net.kasax.raft.Raft;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.gamerules.GameRule;
import net.minecraft.world.level.gamerules.GameRuleCategory;

/**
 * Game rules of the marine module. 26.3 game rules are registry entries with lower-case ids, so the rule is
 * {@code /gamerule raft:shark_bites true|false}.
 */
public final class MarineGameRules {
    /** Sharks may bite and break the edge {@code raft:raft_foundation} blocks of a raft a player stands on. */
    public static GameRule<Boolean> SHARK_BITES;

    private MarineGameRules() {
    }

    static void register() {
        SHARK_BITES = GameRuleBuilder.forBoolean(true).category(GameRuleCategory.MOBS).buildAndRegister(Raft.id("shark_bites"));
    }

    public static boolean sharkBites(ServerLevel level) {
        return SHARK_BITES == null || level.getGameRules().get(SHARK_BITES);
    }
}
