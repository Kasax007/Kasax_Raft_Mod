// CC0 https://github.com/kwpugh/mining_Dimensions

package net.kasax.raft.util;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.portal.TeleportTransition;
import net.minecraft.world.phys.Vec3;

import java.util.Random;

public class TeleporterUtil {
    /**
     * Moves the player to the target dimension (or back to the overworld when already there) and
     * puts him on safe ground. Returns true when the player was teleported.
     */
    public static boolean movePlayer(ResourceKey<Level> dimKey, Level level, Player player) {
        int heightMax = 300;
        int heightMin = 0;

        // Only run on server side
        if (level.isClientSide() || !(level instanceof ServerLevel serverLevel) || !(player instanceof ServerPlayer serverPlayer)) {
            return false;
        }

        // Logic to either send to dim of teleporter in hand or back to Overworld, depending on current location
        if (player.isShiftKeyDown()) {
            return false;
        }

        ServerLevel overWorld = serverLevel.getServer().getLevel(Level.OVERWORLD);
        ServerLevel targetWorld = serverLevel.getServer().getLevel(dimKey);
        ServerLevel destWorld = level.dimension() == dimKey ? overWorld : targetWorld;
        if (destWorld == null) {
            return false;
        }

        // Check a number of times for a safe spot
        for (int i = 1; i < 6; i++) {
            serverPlayer.sendOverlayMessage(Component.translatable(i == 1 ? "item.raft.teleporter1" : "item.raft.teleporter2"));

            BlockPos playerLoc = player.blockPosition();
            Random rand = new Random();

            // Use players current x and z for starting point
            int x = playerLoc.getX() + rand.nextInt(10 + 5) - 5;
            int y = heightMax;   // starting height for a given dimension
            int z = playerLoc.getZ() + rand.nextInt(10 + 5) - 5;

            ChunkAccess chunk = destWorld.getChunk(x >> 4, z >> 4);

            //Let's avoid putting them underground
            while (y > heightMin) {
                y--;
                BlockPos groundPos = new BlockPos(x, y - 2, z);

                boolean isAir = chunk.getBlockState(groundPos).isAir();
                boolean isBedrock = chunk.getBlockState(groundPos).is(Blocks.BEDROCK);
                boolean isLava = chunk.getBlockState(groundPos).is(Blocks.LAVA) || chunk.getBlockState(groundPos).is(Blocks.MAGMA_BLOCK);
                boolean canFit = (y - 2) > heightMin;

                if (!isAir && !isBedrock && !isLava && canFit) {
                    // If block pos under feet is water, place a stone block
                    if (chunk.getBlockState(groundPos).is(Blocks.WATER)) {
                        chunk.setBlockState(groundPos, Blocks.STONE.defaultBlockState(), 0);
                    }

                    BlockPos legPos = new BlockPos(x, y - 1, z);

                    if (chunk.getBlockState(legPos).isAir()) {
                        BlockPos headPos = new BlockPos(x, y, z);

                        if (chunk.getBlockState(headPos).isAir()) {
                            serverPlayer.stopRiding();

                            Vec3 destVec = new Vec3(x + 0.5, y, z + 0.5);
                            serverPlayer.teleport(new TeleportTransition(destWorld, destVec, Vec3.ZERO,
                                    player.getYRot(), player.getXRot(), TeleportTransition.DO_NOTHING));

                            serverPlayer.fallDistance = 0.0F;
                            destWorld.playSound(null, x, y, z, SoundEvents.LODESTONE_COMPASS_LOCK, SoundSource.NEUTRAL, 0.5F,
                                    1F / (destWorld.getRandom().nextFloat() * 0.4F + 0.8F));
                            return true;
                        }
                    }
                }
            }
        }
        serverPlayer.sendOverlayMessage(Component.translatable("item.raft.teleporter3"));
        return false;
    }
}
