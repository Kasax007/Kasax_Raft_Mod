package net.kasax.raft.compat.worldgen;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.levelgen.placement.PlacementContext;
import net.minecraft.world.level.levelgen.placement.PlacementFilter;
import net.minecraft.world.level.levelgen.placement.PlacementModifierType;

/** 1.21.1 port of the newer game's {@code minecraft:random_chance} placement ({@code raft:random_chance}). */
public class RandomChancePlacement extends PlacementFilter {
    public static final MapCodec<RandomChancePlacement> CODEC = Codec.floatRange(0.0F, 1.0F).fieldOf("chance")
            .xmap(RandomChancePlacement::new, p -> p.chance);
    public static final PlacementModifierType<RandomChancePlacement> TYPE = () -> CODEC;

    private final float chance;

    public RandomChancePlacement(float chance) {
        this.chance = chance;
    }

    @Override
    protected boolean shouldPlace(PlacementContext context, RandomSource random, BlockPos pos) {
        return random.nextFloat() < this.chance;
    }

    @Override
    public PlacementModifierType<?> type() {
        return TYPE;
    }
}
