package net.kasax.raft.gametest.mixin;

import net.minecraft.core.Holder;
import net.minecraft.world.level.levelgen.structure.pools.SinglePoolElement;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureProcessorList;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** The processors of a pool element, for the structure audit (pieces that follow the terrain are not compared). */
@Mixin(SinglePoolElement.class)
public interface SinglePoolElementAccess {
    @Accessor("processors")
    Holder<StructureProcessorList> bc$processors();
}
