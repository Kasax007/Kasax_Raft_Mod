package net.kasax.raft.content.story.block;

import net.kasax.raft.content.story.StoryContent;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/** Remembers how often the altar has called the Leviathan (the first call is free) and keeps {@code active} up to date. */
public class LeviathanAltarBlockEntity extends BlockEntity {
    private int summons;

    public LeviathanAltarBlockEntity(BlockPos pos, BlockState state) {
        super(StoryContent.LEVIATHAN_ALTAR_BE, pos, state);
    }

    public int summons() {
        return summons;
    }

    public void addSummon() {
        summons++;
        setChanged();
    }

    public static void serverTick(ServerLevel level, BlockPos pos, BlockState state, LeviathanAltarBlockEntity be) {
        if (level.getGameTime() % 20 != (pos.asLong() & 15)) {
            return;
        }
        boolean alive = LeviathanAltarBlock.findLeviathan(level, pos) != null;
        if (alive != state.getValue(LeviathanAltarBlock.ACTIVE)) {
            level.setBlock(pos, state.setValue(LeviathanAltarBlock.ACTIVE, alive), Block.UPDATE_ALL);
        }
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        summons = input.getIntOr("Summons", 0);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putInt("Summons", summons);
    }
}
