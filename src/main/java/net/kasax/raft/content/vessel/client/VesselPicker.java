package net.kasax.raft.content.vessel.client;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.kasax.raft.content.vessel.VesselColliders;
import net.kasax.raft.content.vessel.VesselEntity;
import net.kasax.raft.content.vessel.VesselPose;
import net.kasax.raft.content.vessel.VesselStructure;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

import java.util.List;

/**
 * Finds the raft block under the crosshair: the view ray is turned into each raft's own frame and traced through its
 * block shapes. If a raft block is closer than what vanilla found, the raft becomes the crosshair target (as an
 * entity: the use key then sends the exact block to the server, see {@link VesselClient}); the block gets an outline.
 */
@Environment(EnvType.CLIENT)
public final class VesselPicker {
    private static @Nullable VesselEntity targetVessel;
    private static @Nullable BlockPos targetLocal;
    /** The hit in the raft's frame (point, side). */
    private static @Nullable BlockHitResult targetHit;

    private VesselPicker() {
    }

    public static @Nullable BlockPos targetOf(VesselEntity vessel) {
        return vessel == targetVessel ? targetLocal : null;
    }

    static @Nullable VesselEntity targetVessel() {
        return targetVessel;
    }

    static @Nullable BlockHitResult targetHit() {
        return targetHit;
    }

    public static void pick(Minecraft mc, float partialTicks) {
        targetVessel = null;
        targetLocal = null;
        targetHit = null;
        Entity camera = mc.getCameraEntity();
        LocalPlayer player = mc.player;
        if (camera == null || player == null || mc.level == null) {
            return;
        }
        List<VesselEntity> vessels = VesselColliders.in(mc.level);
        if (vessels.isEmpty()) {
            return;
        }
        double range = player.blockInteractionRange();
        Vec3 from = camera.getEyePosition(partialTicks);
        Vec3 to = from.add(camera.getViewVector(partialTicks).scale(range));
        HitResult current = mc.hitResult;
        // vanilla's own pick of a raft entity (its box; from inside the box: the eye itself) does not count, only its blocks
        boolean vanillaHit = current != null && current.getType() != HitResult.Type.MISS
                && !(current instanceof EntityHitResult eh && eh.getEntity() instanceof VesselEntity);
        double best = vanillaHit ? current.getLocation().distanceToSqr(from) : range * range;
        Vec3 bestHit = null;
        for (VesselEntity vessel : vessels) {
            VesselStructure structure = vessel.structure();
            if (structure == null || vessel.isDocked()) {
                continue;
            }
            AABB box = vessel.getBoundingBox().inflate(1.0);
            if (!box.contains(from) && box.clip(from, to).isEmpty()) {
                continue;
            }
            VesselPose pose = vessel.renderPose(partialTicks);
            BlockHitResult hit = structure.clip(pose.toLocal(from), pose.toLocal(to));
            if (hit == null || hit.getType() == HitResult.Type.MISS) {
                continue;
            }
            Vec3 world = pose.toWorld(hit.getLocation());
            double d = world.distanceToSqr(from);
            if (d < best) {
                best = d;
                bestHit = world;
                targetVessel = vessel;
                targetLocal = hit.getBlockPos();
                targetHit = hit;
            }
        }
        if (targetVessel != null && bestHit != null) {
            mc.hitResult = new EntityHitResult(targetVessel, bestHit);
            mc.crosshairPickEntity = targetVessel;
        }
    }
}
