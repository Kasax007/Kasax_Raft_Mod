package net.kasax.raft.compat.mixin;

import com.mojang.datafixers.util.Either;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.levelgen.structure.pools.SinglePoolElement;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** 1.21.1 port helper: the template of a single pool element (the newer game has a getter for its location). */
@Mixin(SinglePoolElement.class)
public interface SinglePoolElementTemplateAccess {
    @Accessor("template")
    Either<ResourceLocation, StructureTemplate> raft$template();
}
