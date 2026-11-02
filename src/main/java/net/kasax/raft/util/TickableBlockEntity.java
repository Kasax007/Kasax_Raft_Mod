package net.kasax.raft.util;

import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;

public interface TickableBlockEntity {
    void tick();

    static <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level) {
        return level.isClientSide() ? null : (lvl, pos, state, blockEntity) -> {
            if (blockEntity instanceof TickableBlockEntity tickableBlockEntity) {
                tickableBlockEntity.tick();
            }
        };
    }
}
