package net.kasax.raft.content.vessel;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.player.UseItemCallback;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.kasax.raft.content.vessel.net.VesselBlocksPayload;
import net.kasax.raft.content.vessel.net.VesselBreakPayload;
import net.kasax.raft.content.vessel.net.VesselDataPayload;
import net.kasax.raft.content.vessel.net.VesselUseResultPayload;
import net.kasax.raft.content.vessel.net.VesselUsePayload;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/** Common events of the vessel module: payloads, foundations on open water, cleanup. */
public final class VesselEvents {
    private VesselEvents() {
    }

    static void register() {
        PayloadTypeRegistry.clientboundPlay().register(VesselDataPayload.TYPE, VesselDataPayload.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(VesselBlocksPayload.TYPE, VesselBlocksPayload.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(VesselUseResultPayload.TYPE, VesselUseResultPayload.CODEC);
        PayloadTypeRegistry.serverboundPlay().register(VesselUsePayload.TYPE, VesselUsePayload.CODEC);
        PayloadTypeRegistry.serverboundPlay().register(VesselBreakPayload.TYPE, VesselBreakPayload.CODEC);
        ServerPlayNetworking.registerGlobalReceiver(VesselUsePayload.TYPE, (payload, context) -> {
            ServerPlayer player = context.player();
            context.server().execute(() -> {
                if (player.level().getEntity(payload.entityId()) instanceof VesselEntity vessel && !player.isSpectator()) {
                    VesselInteractions.use(vessel, player, payload.local(), payload.hit(), payload.face(), payload.inside());
                }
            });
        });
        ServerPlayNetworking.registerGlobalReceiver(VesselBreakPayload.TYPE, (payload, context) -> {
            ServerPlayer player = context.player();
            context.server().execute(() -> {
                if (player.level().getEntity(payload.entityId()) instanceof VesselEntity vessel && !player.isSpectator()) {
                    if (payload.done()) {
                        VesselInteractions.finishBreak(vessel, player, payload.local());
                    } else {
                        VesselInteractions.startBreak(vessel, player, payload.local());
                    }
                }
            });
        });
        UseItemCallback.EVENT.register(VesselEvents::placeFoundationOnWater);
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            VesselAssembler.tickLaunches();
            VesselInteractions.tick(server);
        });
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> VesselInteractions.forget(handler.player));
        ServerLifecycleEvents.SERVER_STOPPING.register(server -> VesselAssembler.abortLaunches());
        ServerLifecycleEvents.SERVER_STOPPED.register(server -> VesselColliders.clear(false));
    }

    /**
     * Raft foundations can be set straight onto open water: looking at the sea with nothing else in reach, the
     * foundation takes the place of the top water block, so its top is level with the surface.
     */
    private static InteractionResult placeFoundationOnWater(Player player, Level level, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!(stack.getItem() instanceof BlockItem blockItem) || !blockItem.getBlock().defaultBlockState().is(VesselContent.HULLS)
                || player.isSpectator()) {
            return InteractionResult.PASS;
        }
        Vec3 eye = player.getEyePosition();
        Vec3 end = eye.add(player.getViewVector(1.0F).scale(player.blockInteractionRange()));
        BlockHitResult hit = level.clip(new ClipContext(eye, end, ClipContext.Block.OUTLINE, ClipContext.Fluid.SOURCE_ONLY, player));
        if (hit.getType() != HitResult.Type.BLOCK) {
            return InteractionResult.PASS;
        }
        var water = hit.getBlockPos();
        if (!level.getFluidState(water).is(FluidTags.WATER) || !level.getFluidState(water).isSource()
                || !level.getBlockState(water).canBeReplaced() || !level.getBlockState(water.above()).isAir()) {
            return InteractionResult.PASS;
        }
        BlockPlaceContext context = new BlockPlaceContext(player, hand, stack,
                new BlockHitResult(hit.getLocation(), Direction.UP, water, false));
        InteractionResult result = blockItem.place(context);
        return result.consumesAction() ? result : InteractionResult.PASS;
    }
}
