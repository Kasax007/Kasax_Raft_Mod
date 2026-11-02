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

        // Check a number of times for a safe spot within 5 blocks of the player's X/Z
        serverPlayer.sendOverlayMessage(Component.translatable("item.raft.teleporter1"));
        net.minecraft.util.RandomSource rand = destWorld.getRandom();
        for (int i = 1; i < 6; i++) {
            BlockPos playerLoc = player.blockPosition();
            int x = playerLoc.getX() + rand.nextInt(11) - 5;
            int z = playerLoc.getZ() + rand.nextInt(11) - 5;
            ChunkAccess chunk = destWorld.getChunk(x >> 4, z >> 4);

            // The ground is the first block from the top that is not air; two air blocks above it by construction.
            int y = Math.min(heightMax, destWorld.getMaxY() + 2);
            while (y - 2 > heightMin && chunk.getBlockState(new BlockPos(x, y - 2, z)).isAir()) {
                y--;
            }
            if (y - 2 <= heightMin) continue;
            BlockPos groundPos = new BlockPos(x, y - 2, z);
            var ground = chunk.getBlockState(groundPos);
            // never into lava, onto magma or the nether roof - and never down into a cave below them: try another spot
            if (ground.is(Blocks.BEDROCK) || ground.is(Blocks.LAVA) || ground.is(Blocks.MAGMA_BLOCK)) continue;
            // Water under the feet: a stone block to stand on (a real block update, so every player sees it)
            if (ground.is(Blocks.WATER)) {
                destWorld.setBlockAndUpdate(groundPos, Blocks.STONE.defaultBlockState());
            }
            serverPlayer.stopRiding();
            Vec3 destVec = new Vec3(x + 0.5, groundPos.getY() + 1, z + 0.5);
            serverPlayer.teleport(new TeleportTransition(destWorld, destVec, Vec3.ZERO,
                    player.getYRot(), player.getXRot(), TeleportTransition.DO_NOTHING));
            serverPlayer.fallDistance = 0.0F;
            destWorld.playSound(null, x, groundPos.getY() + 1, z, SoundEvents.LODESTONE_COMPASS_LOCK, SoundSource.NEUTRAL, 0.5F,
                    1F / (destWorld.getRandom().nextFloat() * 0.4F + 0.8F));
            return true;
        }
        serverPlayer.sendOverlayMessage(Component.translatable("item.raft.teleporter3"));
        return false;
    }
}
