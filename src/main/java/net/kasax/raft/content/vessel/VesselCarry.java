package net.kasax.raft.content.vessel;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;

/**
 * Client only: keeps the own player smooth while a raft carries it. A raft moves the player in the raft's tick. If
 * the player ticks after the raft in the same client tick, its tick start would store the carried position as the
 * "old" position and the carry would show as a jump instead of being drawn over the tick; so that carry is taken
 * back out of the old position when the player's tick starts (mixins on ClientLevel.tickEntities and LocalPlayer.tick).
 */
public final class VesselCarry {
    private static boolean playerTicked;
    private static double pendingX;
    private static double pendingY;
    private static double pendingZ;
    private static float pendingYaw;

    private VesselCarry() {
    }

    /** Start of the client's entity ticks. */
    public static void startEntityTicks() {
        playerTicked = false;
        pendingX = 0.0;
        pendingY = 0.0;
        pendingZ = 0.0;
        pendingYaw = 0.0F;
    }

    /** A raft moved an entity on the client (only the own player is carried there). */
    static void carried(Entity entity, double dx, double dy, double dz, float dyaw) {
        if (playerTicked || !(entity instanceof Player)) {
            return;
        }
        pendingX += dx;
        pendingY += dy;
        pendingZ += dz;
        pendingYaw += dyaw;
    }

    /** The own player starts its tick (its old position has just been stored). */
    public static void beforeLocalPlayerTick(Player player) {
        if (pendingX != 0.0 || pendingY != 0.0 || pendingZ != 0.0) {
            player.xo -= pendingX;
            player.xOld -= pendingX;
            player.yo -= pendingY;
            player.yOld -= pendingY;
            player.zo -= pendingZ;
            player.zOld -= pendingZ;
        }
        if (pendingYaw != 0.0F) {
            player.yRotO -= pendingYaw;
            player.yHeadRotO -= pendingYaw;
            player.yBodyRotO -= pendingYaw;
        }
        pendingX = 0.0;
        pendingY = 0.0;
        pendingZ = 0.0;
        pendingYaw = 0.0F;
        playerTicked = true;
    }
}
