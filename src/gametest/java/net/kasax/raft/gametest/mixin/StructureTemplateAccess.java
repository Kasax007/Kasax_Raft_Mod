package net.kasax.raft.gametest.mixin;

import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.List;

/** The block palettes and entities of a structure template, for the structure audit (StructAudit). */
@Mixin(StructureTemplate.class)
public interface StructureTemplateAccess {
    @Accessor("palettes")
    List<StructureTemplate.Palette> bc$palettes();

    @Accessor("entityInfoList")
    List<StructureTemplate.StructureEntityInfo> bc$entities();
}
