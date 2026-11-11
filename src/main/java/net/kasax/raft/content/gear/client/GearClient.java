package net.kasax.raft.content.gear.client;

import net.kasax.raft.platform.client.ClientTickEvents;
import net.kasax.raft.platform.client.EntityRendererRegistry;
import net.kasax.raft.content.gear.GearContent;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/** Client init of the gear module: the spinning boomerang and the range finder of the captain's spyglass. */
public final class GearClient {
    private GearClient() {
    }

    public static void init() {
        EntityRendererRegistry.register(GearContent.BOOMERANG_ENTITY, BoomerangRenderer::new);
        ClientTickEvents.END_CLIENT_TICK.register(GearClient::rangeFinder);
        net.minecraft.client.gui.screens.MenuScreens.register(GearContent.TACKLE_MENU, TackleScreen::new);
    }

    private static void rangeFinder(Minecraft mc) {
        LocalPlayer player = mc.player;
        if (player == null || mc.level == null || player.tickCount % 4 != 0) return;
        if (!player.isScoping() || !player.getUseItem().is(GearContent.CAPTAINS_SPYGLASS)) return;
        double reach = 256;
        Vec3 eye = player.getEyePosition();
        Vec3 look = player.getViewVector(1.0F);
        HitResult block = player.pick(reach, 1.0F, false);
        double blockDist = block.getType() == HitResult.Type.MISS ? reach : block.getLocation().distanceTo(eye);
        Vec3 end = eye.add(look.scale(blockDist));
        AABB area = player.getBoundingBox().expandTowards(look.scale(blockDist)).inflate(1.0);
        EntityHitResult entity = ProjectileUtil.getEntityHitResult(player, eye, end, area,
                e -> !e.isSpectator() && e.isPickable() && e != player, blockDist * blockDist);
        if (entity != null) {
            Entity e = entity.getEntity();
            player.displayClientMessage(Component.translatable("message.raft.spyglass.entity", e.getDisplayName(),
                    (int) Math.round(e.position().distanceTo(eye))), true);
        } else if (block.getType() != HitResult.Type.MISS) {
            player.displayClientMessage(Component.translatable("message.raft.spyglass.distance", (int) Math.round(blockDist)), true);
        }
    }
}
