package net.kasax.raft.content.vessel;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.player.UseItemCallback;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.kasax.raft.content.vessel.net.VesselDataPayload;
import net.kasax.raft.content.vessel.net.VesselUsePayload;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
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
        PayloadTypeRegistry.serverboundPlay().register(VesselUsePayload.TYPE, VesselUsePayload.CODEC);
        ServerPlayNetworking.registerGlobalReceiver(VesselUsePayload.TYPE, (payload, context) -> {
            ServerPlayer player = context.player();
            context.server().execute(() -> {
                Entity entity = player.level().getEntity(payload.entityId());
                if (entity instanceof VesselEntity vessel && !player.isSpectator()) {
                    vessel.useBlock(player, payload.local(), payload.hand());
                }
            });
        });
        UseItemCallback.EVENT.register(VesselEvents::placeFoundationOnWater);
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
