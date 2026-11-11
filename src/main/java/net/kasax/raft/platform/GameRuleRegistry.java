package net.kasax.raft.platform;

import net.minecraft.world.level.GameRules;

/** Fabric's game rule registry: vanilla's register is public. */
public final class GameRuleRegistry {
    private GameRuleRegistry() {
    }

    public static <T extends GameRules.Value<T>> GameRules.Key<T> register(String name, GameRules.Category category, GameRules.Type<T> type) {
        return GameRules.register(name, category, type);
    }
}
