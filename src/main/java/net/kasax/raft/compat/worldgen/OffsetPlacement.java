package net.kasax.raft.compat.worldgen;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.util.valueproviders.ConstantInt;
import net.minecraft.util.valueproviders.IntProvider;
import net.minecraft.world.level.levelgen.placement.PlacementContext;
import net.minecraft.world.level.levelgen.placement.PlacementModifier;
import net.minecraft.world.level.levelgen.placement.PlacementModifierType;

import java.util.stream.Stream;

/** 1.21.1 port of the newer game's {@code minecraft:offset} placement: moves the position by x, y, z (numbers or int providers). */
public class OffsetPlacement extends PlacementModifier {
    public static final MapCodec<OffsetPlacement> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            IntProvider.codec(-64, 64).optionalFieldOf("x", ConstantInt.ZERO).forGetter(p -> p.x),
            IntProvider.codec(-64, 64).optionalFieldOf("y", ConstantInt.ZERO).forGetter(p -> p.y),
            IntProvider.codec(-64, 64).optionalFieldOf("z", ConstantInt.ZERO).forGetter(p -> p.z)
    ).apply(i, OffsetPlacement::new));
    public static final PlacementModifierType<OffsetPlacement> TYPE = () -> CODEC;

    private final IntProvider x;
    private final IntProvider y;
    private final IntProvider z;

    public OffsetPlacement(IntProvider x, IntProvider y, IntProvider z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }

    @Override
    public Stream<BlockPos> getPositions(PlacementContext context, RandomSource random, BlockPos pos) {
        return Stream.of(pos.offset(this.x.sample(random), this.y.sample(random), this.z.sample(random)));
    }

    @Override
    public PlacementModifierType<?> type() {
        return TYPE;
    }
}
