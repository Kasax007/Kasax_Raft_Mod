package net.kasax.raft.platform;

import java.util.HashMap;
import java.util.Map;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.common.ItemAbilities;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.level.BlockEvent;

/** Fabric's strippable block registry on NeoForge's tool modification event (axe strip), keeping the axis. */
public final class StrippableBlockRegistry {
    private static final Map<Block, Block> STRIPPED = new HashMap<>();
    private static boolean hooked;

    private StrippableBlockRegistry() {
    }

    public static synchronized void register(Block input, Block stripped) {
        if (!input.defaultBlockState().hasProperty(RotatedPillarBlock.AXIS) || !stripped.defaultBlockState().hasProperty(RotatedPillarBlock.AXIS)) {
            throw new IllegalArgumentException("Input and stripped blocks must have the 'axis' property");
        }
        STRIPPED.put(input, stripped);
        if (!hooked) {
            hooked = true;
            NeoForge.EVENT_BUS.addListener(BlockEvent.BlockToolModificationEvent.class, StrippableBlockRegistry::onToolUse);
        }
    }

    private static void onToolUse(BlockEvent.BlockToolModificationEvent event) {
        if (event.getItemAbility() != ItemAbilities.AXE_STRIP) {
            return;
        }
        BlockState state = event.getState();
        Block stripped = STRIPPED.get(state.getBlock());
        if (stripped != null) {
            event.setFinalState(stripped.defaultBlockState().setValue(RotatedPillarBlock.AXIS, state.getValue(RotatedPillarBlock.AXIS)));
        }
    }
}
