package net.kasax.raft.world.gen;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.stateproviders.BlockStateProvider;

/** Picks the secondary provider with the given chance, otherwise the primary one. */
public record RandomizedBlockStateProvider(BlockStateProvider primary, BlockStateProvider secondary, float secondaryChance) implements BlockStateProvider {
    public static final MapCodec<RandomizedBlockStateProvider> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            BlockStateProvider.DIRECT_CODEC.fieldOf("primary").forGetter(RandomizedBlockStateProvider::primary),
            BlockStateProvider.DIRECT_CODEC.fieldOf("secondary").forGetter(RandomizedBlockStateProvider::secondary),
            Codec.FLOAT.fieldOf("secondary_chance").forGetter(RandomizedBlockStateProvider::secondaryChance)
    ).apply(instance, RandomizedBlockStateProvider::new));

    @Override
    public MapCodec<? extends BlockStateProvider> codec() {
        return CODEC;
    }

    @Override
    public BlockState getState(LevelAccessor level, RandomSource random, BlockPos pos) {
        if (random.nextFloat() < this.secondaryChance) {
            return this.secondary.getState(level, random, pos);
        } else {
            return this.primary.getState(level, random, pos);
        }
    }
}
