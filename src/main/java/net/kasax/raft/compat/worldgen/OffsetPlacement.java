package net.kasax.raft.compat.worldgen;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.levelgen.placement.PlacementContext;
import net.minecraft.world.level.levelgen.placement.PlacementModifier;
import net.minecraft.world.level.levelgen.placement.PlacementModifierType;

import java.util.stream.Stream;

/** 1.21.1 port of the newer game's {@code minecraft:offset} placement: moves the position by a fixed x, y, z. */
public class OffsetPlacement extends PlacementModifier {
    public static final MapCodec<OffsetPlacement> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            Codec.INT.optionalFieldOf("x", 0).forGetter(p -> p.x),
            Codec.INT.optionalFieldOf("y", 0).forGetter(p -> p.y),
            Codec.INT.optionalFieldOf("z", 0).forGetter(p -> p.z)
    ).apply(i, OffsetPlacement::new));
    public static final PlacementModifierType<OffsetPlacement> TYPE = () -> CODEC;

    private final int x;
    private final int y;
    private final int z;

    public OffsetPlacement(int x, int y, int z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }

    @Override
    public Stream<BlockPos> getPositions(PlacementContext context, RandomSource random, BlockPos pos) {
        return Stream.of(pos.offset(this.x, this.y, this.z));
    }

    @Override
    public PlacementModifierType<?> type() {
        return TYPE;
    }
}
