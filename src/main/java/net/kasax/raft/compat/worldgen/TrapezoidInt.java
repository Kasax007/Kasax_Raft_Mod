package net.kasax.raft.compat.worldgen;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.util.valueproviders.IntProvider;
import net.minecraft.util.valueproviders.IntProviderType;

/**
 * 1.21.1 port of the newer game's int trapezoid provider ({@code raft:trapezoid}; same sampling): the spread of
 * scattered features (anemones, shells, grass patches) around their centre.
 */
public class TrapezoidInt extends IntProvider {
    public static final MapCodec<TrapezoidInt> CODEC = RecordCodecBuilder.<TrapezoidInt>mapCodec(i -> i.group(
            Codec.INT.fieldOf("min").forGetter(t -> t.min),
            Codec.INT.fieldOf("max").forGetter(t -> t.max),
            Codec.INT.fieldOf("plateau").forGetter(t -> t.plateau)
    ).apply(i, TrapezoidInt::new)).validate(t -> t.max < t.min ? DataResult.error(() -> "max < min")
            : t.plateau > t.max - t.min ? DataResult.error(() -> "plateau wider than the span") : DataResult.success(t));
    public static final IntProviderType<TrapezoidInt> TYPE = () -> CODEC;

    private final int min;
    private final int max;
    private final int plateau;

    public TrapezoidInt(int min, int max, int plateau) {
        this.min = min;
        this.max = max;
        this.plateau = plateau;
    }

    @Override
    public int sample(RandomSource random) {
        if (this.plateau == 0 && this.max == -this.min) {
            return random.nextInt(this.max + 1) - random.nextInt(this.max + 1);
        }
        int range = this.max - this.min;
        if (this.plateau == range) {
            return Mth.randomBetweenInclusive(random, this.min, this.max);
        }
        int plateauStart = (range - this.plateau) / 2;
        int plateauEnd = range - plateauStart;
        return this.min + Mth.randomBetweenInclusive(random, 0, plateauEnd) + Mth.randomBetweenInclusive(random, 0, plateauStart);
    }

    @Override
    public int getMinValue() {
        return this.min;
    }

    @Override
    public int getMaxValue() {
        return this.max;
    }

    @Override
    public IntProviderType<?> getType() {
        return TYPE;
    }
}
