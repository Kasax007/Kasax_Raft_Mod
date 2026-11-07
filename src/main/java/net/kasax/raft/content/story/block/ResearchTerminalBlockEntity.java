package net.kasax.raft.content.story.block;

import net.kasax.raft.content.story.StoryContent;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.kasax.raft.compat.ValueInput;
import net.kasax.raft.compat.ValueOutput;

/**
 * Holds the {@code LogId} of a research terminal (set by structure NBT, e.g.
 * {@code {"id":"raft:research_terminal","LogId":"helix_1"}}). The texts live in the lang file:
 * {@code terminal.raft.<LogId>.title} and {@code .text}.
 */
public class ResearchTerminalBlockEntity extends BlockEntity {
    public static final String[] MISC_LOGS = {"lab_misc_1", "lab_misc_2", "lab_misc_3"};
    private String logId = "";

    public ResearchTerminalBlockEntity(BlockPos pos, BlockState state) {
        super(StoryContent.RESEARCH_TERMINAL_BE, pos, state);
    }

    /** The log of this terminal; a terminal without one (placed by a player) shows one of the generic lab logs. */
    public String logId() {
        if (logId.isEmpty()) {
            logId = MISC_LOGS[Math.floorMod(getBlockPos().hashCode(), MISC_LOGS.length)];
            setChanged();
        }
        return logId;
    }

    public void setLogId(String id) {
        this.logId = id == null ? "" : id;
        setChanged();
    }

    @Override
    protected void loadAdditional(net.minecraft.nbt.CompoundTag tag121, net.minecraft.core.HolderLookup.Provider reg121) {
        ValueInput input = ValueInput.of(tag121, reg121);
        super.loadAdditional(input.tag(), input.lookup());
        logId = input.getStringOr("LogId", "");
    }

    @Override
    protected void saveAdditional(net.minecraft.nbt.CompoundTag tag121, net.minecraft.core.HolderLookup.Provider reg121) {
        ValueOutput output = ValueOutput.of(tag121, reg121);
        super.saveAdditional(output.tag(), output.lookup());
        if (!logId.isEmpty()) output.putString("LogId", logId);
    }
}
