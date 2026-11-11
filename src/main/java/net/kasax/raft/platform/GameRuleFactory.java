package net.kasax.raft.platform;

import net.minecraft.world.level.GameRules;

/** Fabric's game rule factory (the boolean rule Beach Craft uses). */
public final class GameRuleFactory {
    private GameRuleFactory() {
    }

    public static GameRules.Type<GameRules.BooleanValue> createBooleanRule(boolean defaultValue) {
        return GameRules.BooleanValue.create(defaultValue);
    }
}
