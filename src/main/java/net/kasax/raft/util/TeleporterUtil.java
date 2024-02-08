// CC0 https://github.com/kwpugh/mining_Dimensions

package net.kasax.raft.util;
import net.fabricmc.fabric.api.dimension.v1.FabricDimensions;
import net.minecraft.block.Blocks;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.RegistryKey;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.TeleportTarget;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;

import java.util.Random;

public class TeleporterUtil
{
    public static TypedActionResult<ItemStack> movePlayer(RegistryKey<World> dimKey, World world, PlayerEntity player, Hand hand)
    {
        ItemStack stack = player.getStackInHand(hand);
        ItemStack stack1 = player.getStackInHand(hand);

        int heightMax = 300;
        int heightMin = 0;

        // Only run on server side
        if(world.isClient) return TypedActionResult.success(stack);

        // Logic to either send to dim of teleporter in hand or back to Overworld, depending on current location
        if(!player.isSneaking())
        {
            ServerWorld destWorld;
            ServerWorld overWorld = ((ServerWorld)world).getServer().getWorld(World.OVERWORLD);
            ServerWorld targetWorld = ((ServerWorld)world).getServer().getWorld(dimKey);
            ServerPlayerEntity serverPlayer = (ServerPlayerEntity) player;
            RegistryKey<World> currentWorldKey = world.getRegistryKey();

            if(currentWorldKey == dimKey)
            {
                destWorld = overWorld;
            }
            else
            {
                destWorld = targetWorld;
            }

            assert destWorld != null;
            RegistryKey<World> destKey = destWorld.getRegistryKey();

            // Check a number of times for a safe spot
            for (int i = 1; i < 6; i++)
            {

                if (i == 1)
                {
                    serverPlayer.sendMessage((Text.translatable("item.raft.teleporter1")), true);   //checking...
                }

                if (i > 1)
                {
                    serverPlayer.sendMessage((Text.translatable("item.raft.teleporter2")), true);
                }

                BlockPos playerLoc = player.getBlockPos();
                Random rand = new Random();

                // Use players current x and z for starting point
                int x = Math.round(playerLoc.getX()) + rand.nextInt(10 + 5) - 5;
                int y = heightMax;   // starting height for a given dimension
                int z = Math.round(playerLoc.getZ()) + rand.nextInt(10 + 5) - 5;

                Chunk chunk = destWorld.getChunk(x >> 4, z >> 4);

                //Let's avoid putting them underground
                while(y > heightMin)
                {
                    y--;
                    BlockPos groundPos = new BlockPos(x, y - 2, z);

                    boolean isAir = chunk.getBlockState(groundPos).getBlock() == Blocks.AIR;
                    boolean isBedrock = chunk.getBlockState(groundPos).getBlock() == Blocks.BEDROCK;
                    boolean isLava = chunk.getBlockState(groundPos).getBlock() == Blocks.LAVA || chunk.getBlockState(groundPos).getBlock() == Blocks.MAGMA_BLOCK;
                    boolean canFit = (y - 2) > heightMin;

                    if (!isAir && !isBedrock && !isLava && canFit)
                    {
                        // If block pos under feet is water, place a stone block
                        if(chunk.getBlockState(groundPos).getBlock().equals(Blocks.WATER)) chunk.setBlockState(groundPos, Blocks.STONE.getDefaultState(), false);

                        BlockPos legPos = new BlockPos(x, y - 1, z);

                        if (chunk.getBlockState(legPos).getBlock() == Blocks.AIR)
                        {
                            BlockPos headPos = new BlockPos(x, y, z);

                            if (chunk.getBlockState(headPos).getBlock() == Blocks.AIR)
                            {
                                serverPlayer.stopRiding();

                                Vec3d destVec = new Vec3d(x, y, z);
                                TeleportTarget teleportTarget = new TeleportTarget(destVec, null, player.getYaw(), player.getPitch());
                                FabricDimensions.teleport(serverPlayer, destWorld, teleportTarget);

                                serverPlayer.fallDistance = 0.0F;
                                world.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.ENTITY_ENDERMAN_TELEPORT, SoundCategory.NEUTRAL, 0.5F, 0.4F / (world.getRandom().nextFloat() * 0.4F + 0.8F));

                                return TypedActionResult.success(stack);
                            }
                        }
                    }
                }
            }
            //BlockPos playerLoc = player.getBlockPos();
            //Random rand = new Random();
            // Use players current x and z for starting point
            //int x = Math.round(playerLoc.getX()) + rand.nextInt(10 + 5) - 5;
            //int y = heightMax;   // starting height for a given dimension
            //int z = Math.round(playerLoc.getZ()) + rand.nextInt(10 + 5) - 5;
            //Vec3d destVec = new Vec3d(x, y, z);
            //TeleportTarget teleportTarget = new TeleportTarget(destVec, null, player.getYaw(), player.getPitch());
            //FabricDimensions.teleport(serverPlayer, destWorld, teleportTarget);
            serverPlayer.sendMessage((Text.translatable("item.raft.teleporter3")), true);
        }

        return TypedActionResult.success(stack);
    }
}

