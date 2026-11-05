package net.kasax.raft.content.vessel;

import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.kasax.raft.content.vessel.net.VesselUseResultPayload;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.Stats;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUtils;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.LiquidBlockContainer;
import net.minecraft.world.level.block.SignBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Server: what players do to the blocks of a sailing raft. Their client finds the block in the raft's frame and sends
 * it (see VesselPicker, VesselClient); here it is done to the real block in the raft's shipyard plot, with the vanilla
 * logic: {@code ServerPlayerGameMode.useItemOn} for using and placing, {@code destroyBlock} for breaking. During the
 * call the player faces the way they face on the raft (so placed stairs, doors or furnaces point the right way);
 * reach checks of menus map plot positions to the raft (PlayerReachMixin), drops and sounds appear at the raft.
 */
public final class VesselInteractions {
    /**
     * The block a player is breaking on a raft, the server tick they began and whether their client says it is broken
     * (if that comes earlier than the server can follow, e.g. while the server lags, it is broken a little later).
     */
    private record Mining(int vessel, long local, int startTick, boolean done) {
    }

    private static final Map<UUID, Mining> MINING = new HashMap<>();

    private VesselInteractions() {
    }

    /** The plot position of a block of the raft, or null when the plot cannot be used right now. */
    private static BlockPos plotPos(VesselEntity vessel, ServerLevel level, BlockPos local) {
        if (vessel.plot() < 0 || !VesselShipyard.loaded(level, vessel.plot())) {
            return null;
        }
        return VesselShipyard.origin(level, vessel.plot()).offset(local);
    }

    /** Is the block in the player's reach, as their client sees the raft? */
    private static boolean inReach(VesselEntity vessel, ServerPlayer player, BlockPos local) {
        Vec3 center = vessel.clientPose().toWorld(local.getX() + 0.5, local.getY() + 0.5, local.getZ() + 0.5);
        double reach = player.blockInteractionRange() + 3.0;
        return player.getEyePosition().distanceToSqr(center) <= reach * reach;
    }

    /** Runs {@code action} while the player faces, in the plot, the way they face on the raft. */
    private static <T> T facingOnRaft(VesselEntity vessel, ServerPlayer player, java.util.function.Supplier<T> action) {
        float yRot = player.getYRot();
        float headRot = player.getYHeadRot();
        float bodyRot = player.yBodyRot;
        float raftYaw = vessel.getYRot();
        player.setYRot(Mth.wrapDegrees(yRot - raftYaw));
        player.setYHeadRot(Mth.wrapDegrees(headRot - raftYaw));
        player.yBodyRot = Mth.wrapDegrees(bodyRot - raftYaw);
        try {
            return action.get();
        } finally {
            player.setYRot(yRot);
            player.setYHeadRot(headRot);
            player.yBodyRot = bodyRot;
        }
    }

    // ------------------------------------------------------------------------------------------- use
    /** A player used (right click) a block of the raft: {@code hit} is in the raft's frame. */
    static void use(VesselEntity vessel, ServerPlayer player, BlockPos local, Vec3 hit, Direction face, boolean inside) {
        VesselStructure structure = vessel.structure();
        if (structure == null || vessel.isDocked() || structure.get(local) == null || !inReach(vessel, player, local)
                || !(player.level() instanceof ServerLevel level)) {
            return;
        }
        if (local.equals(structure.helm())) {
            vessel.useHelm(player);
            return;
        }
        BlockPos at = plotPos(vessel, level, local);
        if (at == null) {
            return;
        }
        BlockState state = level.getBlockState(at);
        if (state.getBlock() instanceof BedBlock) {
            player.sendOverlayMessage(Component.translatable("message.raft.vessel.no_sleep"));
            return;
        }
        BlockPos origin = VesselShipyard.origin(level, vessel.plot());
        BlockHitResult plotHit = new BlockHitResult(Vec3.atLowerCornerOf(origin).add(hit), face, at, inside);
        for (InteractionHand hand : InteractionHand.values()) {
            ItemStack stack = player.getItemInHand(hand);
            if (state.getBlock() instanceof SignBlock && stack.isEmpty()) {
                // sign text is edited on the client, which has no block there: only once anchored
                player.sendOverlayMessage(Component.translatable("message.raft.vessel.anchor_first"));
                return;
            }
            if (!fitsOnRaft(vessel, level, player, hand, stack, plotHit)) {
                return;
            }
            InteractionResult result = facingOnRaft(vessel, player, () -> {
                InteractionResult r = player.gameMode.useItemOn(player, level, stack, hand, plotHit);
                if (!r.consumesAction() && stack.getItem() instanceof BucketItem bucket) {
                    r = emptyBucket(player, level, hand, stack, bucket, plotHit);
                }
                return r;
            });
            if (result.consumesAction()) {
                // the player's own client swung already (it predicts); the others see it now
                player.swing(hand, stack.getInteractAnimation(), false);
                return;
            }
            if (result instanceof InteractionResult.Fail) {
                break;
            }
        }
        // nothing to do with the block: the client uses the item in the air instead (eat, drink, throw, ...)
        ServerPlayNetworking.send(player, new VesselUseResultPayload(vessel.getId()));
    }

    /**
     * Blocks placed on the raft must stay inside its plot and the raft within its size; tells the player otherwise.
     */
    private static boolean fitsOnRaft(VesselEntity vessel, ServerLevel level, ServerPlayer player, InteractionHand hand, ItemStack stack,
                                      BlockHitResult plotHit) {
        BlockPos target;
        if (stack.getItem() instanceof BlockItem) {
            target = facingOnRaft(vessel, player, () -> new BlockPlaceContext(player, hand, stack, plotHit).getClickedPos());
        } else if (stack.getItem() instanceof BucketItem) {
            target = plotHit.getBlockPos().relative(plotHit.getDirection());
        } else {
            return true;
        }
        if (VesselShipyard.plotAt(level, target.getX(), target.getY(), target.getZ()) != vessel.plot()) {
            player.sendOverlayMessage(Component.translatable("message.raft.vessel.too_far_out"));
            return false;
        }
        VesselStructure structure = vessel.structure();
        if (structure != null && structure.blockCount() >= VesselStructure.MAX_BLOCKS_AT_SEA && level.getBlockState(target).isAir()) {
            player.sendOverlayMessage(Component.translatable("message.raft.vessel.too_big", VesselStructure.MAX_BLOCKS_AT_SEA));
            return false;
        }
        return true;
    }

    /**
     * A full bucket on the raft (vanilla aims it from the player's eyes, which are not in the plot): only into blocks
     * that hold water (waterlogging), as open water would run off the deck.
     */
    private static InteractionResult emptyBucket(ServerPlayer player, ServerLevel level, InteractionHand hand, ItemStack stack,
                                                 BucketItem bucket, BlockHitResult hit) {
        if (stack.is(Items.BUCKET)) {
            // an empty bucket scoops from the sea (the client uses it in the air, through the deck)
            return InteractionResult.PASS;
        }
        BlockPos pos = hit.getBlockPos();
        if (!(level.getBlockState(pos).getBlock() instanceof LiquidBlockContainer) || !bucket.emptyContents(player, level, pos, hit)) {
            player.sendOverlayMessage(Component.translatable("message.raft.vessel.no_open_water"));
            return InteractionResult.FAIL;
        }
        bucket.checkExtraContent(player, level, stack, pos);
        player.awardStat(Stats.ITEM_USED.get(bucket));
        player.setItemInHand(hand, ItemUtils.createFilledResult(stack, player, BucketItem.getEmptySuccessItem(stack, player)));
        return InteractionResult.SUCCESS_SERVER;
    }

    // ------------------------------------------------------------------------------------------- break
    /** A player began breaking a block of the raft (their client counts the progress). */
    static void startBreak(VesselEntity vessel, ServerPlayer player, BlockPos local) {
        MINING.put(player.getUUID(), new Mining(vessel.getId(), local.asLong(), player.level().getServer().getTickCount(), false));
    }

    /** A player's client says the block is broken: check the time it took and break it in the plot. */
    static void finishBreak(VesselEntity vessel, ServerPlayer player, BlockPos local) {
        Mining mining = MINING.remove(player.getUUID());
        if (player.isCreative()) {
            breakNow(vessel, player, local);
        } else if (mining != null && mining.vessel() == vessel.getId() && mining.local() == local.asLong()) {
            if (!tryBreak(vessel, player, mining)) {
                MINING.put(player.getUUID(), new Mining(mining.vessel(), mining.local(), mining.startTick(), true));
            }
        }
    }

    /** Server tick: breaks that came early are done once enough time has passed (or dropped after a while). */
    static void tick(net.minecraft.server.MinecraftServer server) {
        if (MINING.isEmpty()) {
            return;
        }
        MINING.entrySet().removeIf(e -> {
            Mining mining = e.getValue();
            if (!mining.done()) {
                return false;
            }
            ServerPlayer player = server.getPlayerList().getPlayer(e.getKey());
            if (player == null || !(player.level().getEntity(mining.vessel()) instanceof VesselEntity vessel)) {
                return true;
            }
            return tryBreak(vessel, player, mining) || server.getTickCount() - mining.startTick() > 1200;
        });
    }

    /** Breaks the block if the time since the player began is enough for it (70 %, as vanilla); true when done with it. */
    private static boolean tryBreak(VesselEntity vessel, ServerPlayer player, Mining mining) {
        BlockPos local = BlockPos.of(mining.local());
        BlockPos at = vessel.level() instanceof ServerLevel level ? plotPos(vessel, level, local) : null;
        if (at == null) {
            return true;
        }
        BlockState state = vessel.level().getBlockState(at);
        float perTick = state.getDestroyProgress(player, vessel.level(), at);
        if (state.isAir() || perTick <= 0.0F) {
            return true;
        }
        int took = player.level().getServer().getTickCount() - mining.startTick() + 1;
        if (perTick * took < 0.7F) {
            return false;
        }
        breakNow(vessel, player, local);
        return true;
    }

    private static void breakNow(VesselEntity vessel, ServerPlayer player, BlockPos local) {
        VesselStructure structure = vessel.structure();
        if (structure == null || vessel.isDocked() || structure.get(local) == null || !inReach(vessel, player, local)
                || !(player.level() instanceof ServerLevel level)) {
            return;
        }
        if (local.equals(structure.helm())) {
            player.sendOverlayMessage(Component.translatable("message.raft.vessel.anchor_first"));
            return;
        }
        BlockPos at = plotPos(vessel, level, local);
        if (at == null || level.getBlockState(at).isAir()) {
            return;
        }
        facingOnRaft(vessel, player, () -> player.gameMode.destroyBlock(at));
    }

    static void forget(ServerPlayer player) {
        MINING.remove(player.getUUID());
    }
}
