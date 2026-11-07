package net.kasax.raft.compat.mixin;

import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.advancements.AdvancementProgress;
import net.minecraft.client.multiplayer.ClientAdvancements;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.Map;

/** 1.21.1 port helper: the client's advancement progress (the newer game has a getter). */
@Mixin(ClientAdvancements.class)
public interface ClientAdvancementsAccess {
    @Accessor("progress")
    Map<AdvancementHolder, AdvancementProgress> raft$progress();
}
