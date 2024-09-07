package net.kasax.raft.world.gen;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.kasax.raft.Raft;
import net.minecraft.block.BlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.gen.stateprovider.BlockStateProvider;
import net.minecraft.world.gen.stateprovider.BlockStateProviderType;

public class RandomizedBlockStateProvider extends BlockStateProvider {
    public static final MapCodec<RandomizedBlockStateProvider> CODEC = RecordCodecBuilder.mapCodec((instance) -> {
        return instance.group(
                BlockStateProvider.TYPE_CODEC.fieldOf("primary").forGetter((provider) -> provider.primary),
                BlockStateProvider.TYPE_CODEC.fieldOf("secondary").forGetter((provider) -> provider.secondary),
                Codec.FLOAT.fieldOf("secondary_chance").forGetter((provider) -> provider.secondaryChance)
        ).apply(instance, RandomizedBlockStateProvider::new);
    });
    private final BlockStateProvider primary;
    private final BlockStateProvider secondary;
    private final float secondaryChance;

    public RandomizedBlockStateProvider(BlockStateProvider primary, BlockStateProvider secondary, float secondaryChance) {
        this.primary = primary;
        this.secondary = secondary;
        this.secondaryChance = secondaryChance;
    }

    @Override
    protected BlockStateProviderType<?> getType() {
        return Raft.RANDOMIZED_BLOCK_STATE_PROVIDER;
    }

    @Override
    public BlockState get(Random random, BlockPos pos) {
        if (random.nextFloat() < this.secondaryChance) {
            return this.secondary.get(random, pos);
        } else {
            return this.primary.get(random, pos);
        }
    }
}

