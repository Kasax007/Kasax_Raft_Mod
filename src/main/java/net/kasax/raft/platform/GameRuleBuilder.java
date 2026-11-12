package net.kasax.raft.platform;

import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.serialization.Codec;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.flag.FeatureFlagSet;
import net.minecraft.world.level.gamerules.GameRule;
import net.minecraft.world.level.gamerules.GameRuleCategory;
import net.minecraft.world.level.gamerules.GameRuleType;
import net.minecraft.world.level.gamerules.GameRuleTypeVisitor;

/** Fabric's GameRuleBuilder (the boolean rule Beach Craft uses): a vanilla game rule in the game rule registry. */
public final class GameRuleBuilder {
    private final boolean defaultValue;
    private GameRuleCategory category = GameRuleCategory.MISC;

    private GameRuleBuilder(boolean defaultValue) {
        this.defaultValue = defaultValue;
    }

    public static GameRuleBuilder forBoolean(boolean defaultValue) {
        return new GameRuleBuilder(defaultValue);
    }

    public GameRuleBuilder category(GameRuleCategory category) {
        this.category = category;
        return this;
    }

    public GameRule<Boolean> buildAndRegister(Identifier id) {
        return Registry.register(BuiltInRegistries.GAME_RULE, id, new GameRule<>(category, GameRuleType.BOOL, BoolArgumentType.bool(),
                GameRuleTypeVisitor::visitBoolean, Codec.BOOL, b -> b ? 1 : 0, defaultValue, FeatureFlagSet.of()));
    }
}
