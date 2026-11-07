package net.kasax.raft.compat.worldgen;

import com.mojang.serialization.MapCodec;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;

/** 1.21.1 port helper: the feature type of a {@link WorldFeature}. */
public class CompatFeature<C extends WorldFeature> extends Feature<C> {
    public CompatFeature(MapCodec<C> codec) {
        super(codec.codec());
    }

    @Override
    public boolean place(FeaturePlaceContext<C> context) {
        return context.config().place(context.level(), context.chunkGenerator(), context.random(), context.origin());
    }
}
