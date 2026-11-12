package net.kasax.raft.platform.transfer;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.kasax.raft.platform.energy.EnergyStorage;
import org.jspecify.annotations.Nullable;

/** Fabric's BlockApiCache for the energy lookup: one position, asked for a side each time. */
public final class BlockApiCache<A, C> {
    private final EnergyStorage.Lookup lookup;
    private final ServerLevel level;
    private final BlockPos pos;

    private BlockApiCache(EnergyStorage.Lookup lookup, ServerLevel level, BlockPos pos) {
        this.lookup = lookup;
        this.level = level;
        this.pos = pos.immutable();
    }

    public static BlockApiCache<EnergyStorage, Direction> create(EnergyStorage.Lookup lookup, ServerLevel level, BlockPos pos) {
        return new BlockApiCache<>(lookup, level, pos);
    }

    public @Nullable BlockEntity getBlockEntity() {
        return level.isLoaded(pos) ? level.getBlockEntity(pos) : null;
    }

    @SuppressWarnings("unchecked")
    public @Nullable A find(@Nullable C context) {
        if (!level.isLoaded(pos)) {
            return null;
        }
        return (A) lookup.find(level, pos, (Direction) context);
    }
}
