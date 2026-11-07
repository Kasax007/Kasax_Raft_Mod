package net.kasax.raft.world.gen;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.stateproviders.BlockStateProvider;
import net.minecraft.world.level.levelgen.feature.stateproviders.BlockStateProviderType;

/** Picks the secondary provider with the given chance, otherwise the primary one. */
public class RandomizedBlockStateProvider extends BlockStateProvider {
    public static final MapCodec<RandomizedBlockStateProvider> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            BlockStateProvider.CODEC.fieldOf("primary").forGetter(RandomizedBlockStateProvider::primary),
            BlockStateProvider.CODEC.fieldOf("secondary").forGetter(RandomizedBlockStateProvider::secondary),
            Codec.FLOAT.fieldOf("secondary_chance").forGetter(RandomizedBlockStateProvider::secondaryChance)
    ).apply(instance, RandomizedBlockStateProvider::new));
    public static final BlockStateProviderType<RandomizedBlockStateProvider> TYPE = new BlockStateProviderType<>(CODEC);

    private final BlockStateProvider primary;
    private final BlockStateProvider secondary;
    private final float secondaryChance;

    public RandomizedBlockStateProvider(BlockStateProvider primary, BlockStateProvider secondary, float secondaryChance) {
        this.primary = primary;
        this.secondary = secondary;
        this.secondaryChance = secondaryChance;
    }

    public BlockStateProvider primary() {
        return this.primary;
    }

    public BlockStateProvider secondary() {
        return this.secondary;
    }

    public float secondaryChance() {
        return this.secondaryChance;
    }

    @Override
    protected BlockStateProviderType<?> type() {
        return TYPE;
    }

    @Override
    public BlockState getState(RandomSource random, BlockPos pos) {
        return random.nextFloat() < this.secondaryChance ? this.secondary.getState(random, pos) : this.primary.getState(random, pos);
    }
}
