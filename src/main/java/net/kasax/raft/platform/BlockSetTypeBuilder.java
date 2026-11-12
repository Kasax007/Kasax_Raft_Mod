package net.kasax.raft.platform;

import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.state.properties.BlockSetType;

/** Fabric's BlockSetTypeBuilder (copy of a vanilla set with changed flags, registered under the full id). */
public final class BlockSetTypeBuilder {
    private final BlockSetType base;
    private boolean openableByHand;
    private boolean openableByWindCharge;
    private boolean buttonActivatedByArrows;
    private BlockSetType.PressurePlateSensitivity pressurePlateSensitivity;

    private BlockSetTypeBuilder(BlockSetType base) {
        this.base = base;
        this.openableByHand = base.canOpenByHand();
        this.openableByWindCharge = base.canOpenByWindCharge();
        this.buttonActivatedByArrows = base.canButtonBeActivatedByArrows();
        this.pressurePlateSensitivity = base.pressurePlateSensitivity();
    }

    public static BlockSetTypeBuilder copyOf(BlockSetType setType) {
        return new BlockSetTypeBuilder(setType);
    }

    public BlockSetTypeBuilder openableByHand(boolean value) {
        this.openableByHand = value;
        return this;
    }

    public BlockSetTypeBuilder openableByWindCharge(boolean value) {
        this.openableByWindCharge = value;
        return this;
    }

    public BlockSetTypeBuilder buttonActivatedByArrows(boolean value) {
        this.buttonActivatedByArrows = value;
        return this;
    }

    public BlockSetTypeBuilder pressurePlateSensitivity(BlockSetType.PressurePlateSensitivity value) {
        this.pressurePlateSensitivity = value;
        return this;
    }

    public BlockSetType register(Identifier id) {
        return BlockSetType.register(new BlockSetType(id.toString(), openableByHand, openableByWindCharge, buttonActivatedByArrows,
                pressurePlateSensitivity, base.soundType(), base.doorClose(), base.doorOpen(), base.trapdoorClose(), base.trapdoorOpen(),
                base.pressurePlateClickOff(), base.pressurePlateClickOn(), base.buttonClickOff(), base.buttonClickOn()));
    }
}
