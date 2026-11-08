package net.kasax.raft.compat.worldgen;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.blockpredicates.BlockPredicate;
import net.minecraft.world.level.levelgen.feature.stateproviders.BlockStateProvider;

import java.util.List;

/**
 * 1.21.1 port of the newer game's simple block with a rule based block (e.g. sea glass and shells waterlogged under
 * water, dry on the beach): the first rule whose predicate holds at the spot gives the block, else the fallback.
 */
public record RuleBlockFeature(BlockStateProvider fallback, List<Rule> rules) implements WorldFeature {
    public record Rule(BlockPredicate ifTrue, BlockStateProvider then) {
        static final com.mojang.serialization.Codec<Rule> CODEC = RecordCodecBuilder.create(i -> i.group(
                BlockPredicate.CODEC.fieldOf("if_true").forGetter(Rule::ifTrue),
                BlockStateProvider.CODEC.fieldOf("then").forGetter(Rule::then)
        ).apply(i, Rule::new));
    }

    public static final MapCodec<RuleBlockFeature> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            BlockStateProvider.CODEC.fieldOf("fallback").forGetter(RuleBlockFeature::fallback),
            Rule.CODEC.listOf().fieldOf("rules").forGetter(RuleBlockFeature::rules)
    ).apply(i, RuleBlockFeature::new));

    @Override
    public MapCodec<RuleBlockFeature> codec() {
        return CODEC;
    }

    @Override
    public boolean place(WorldGenLevel level, ChunkGenerator generator, RandomSource random, BlockPos origin) {
        BlockStateProvider provider = this.fallback;
        for (Rule rule : this.rules) {
            if (rule.ifTrue().test(level, origin)) {
                provider = rule.then();
                break;
            }
        }
        BlockState state = provider.getState(random, origin);
        if (!state.canSurvive(level, origin)) {
            return false;
        }
        level.setBlock(origin, state, 2);
        return true;
    }
}
