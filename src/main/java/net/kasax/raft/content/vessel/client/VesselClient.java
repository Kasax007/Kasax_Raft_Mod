package net.kasax.raft.content.vessel.client;

import net.kasax.raft.platform.client.ClientTickEvents;
import net.kasax.raft.platform.client.ClientPlayConnectionEvents;
import net.kasax.raft.platform.client.ClientPlayNetworking;
import net.kasax.raft.platform.client.EntityRendererRegistry;
import net.kasax.raft.content.vessel.VesselColliders;
import net.kasax.raft.content.vessel.VesselContent;
import net.kasax.raft.content.vessel.VesselEntity;
import net.kasax.raft.content.vessel.VesselStructure;
import net.kasax.raft.content.vessel.net.VesselArrivalPayload;
import net.kasax.raft.content.vessel.net.VesselBlocksPayload;
import net.kasax.raft.content.vessel.net.VesselBreakPayload;
import net.kasax.raft.content.vessel.net.VesselDataPayload;
import net.kasax.raft.content.vessel.net.VesselUsePayload;
import net.kasax.raft.content.vessel.net.VesselUseResultPayload;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.EmptyBlockGetter;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

/**
 * Client-only part of the vessel module: the raft renderer, the block payloads, and the player's hands on raft blocks.
 * Right click sends the block (in the raft's frame) to the server, which uses it like a world block; if that does
 * nothing, the item is used in the air as in vanilla. Holding left click breaks raft blocks: the client counts the
 * progress like vanilla mining (tool, enchantments, effects) and tells the server when the block is broken.
 */
public final class VesselClient {
    /** The helm answers once per click (no flickering in and out of the helm while the key is held). */
    private static boolean helmUseHeld;
    private static boolean wasAtHelm;
    /** Where the server put the player on a raft (raft, position in its frame, where to wait meanwhile, ticks left). */
    private record Arrival(int vessel, Vec3 local, Vec3 hold, int[] ticksLeft) {
    }

    private static @Nullable Arrival arrival;
    private static int miningVessel = -1;
    private static @Nullable BlockPos miningLocal;
    private static float miningProgress;
    private static int miningTicks;
    private static int miningCooldown;

    private VesselClient() {
    }

    public static void init() {
        EntityRendererRegistry.register(VesselContent.VESSEL, VesselRenderer::new);
        net.minecraft.client.gui.screens.MenuScreens.register(VesselContent.RAFT_MOTOR_MENU, RaftMotorScreen::new);
        ClientPlayNetworking.registerGlobalReceiver(VesselDataPayload.TYPE, (payload, context) -> {
            Minecraft mc = context.client();
            mc.execute(() -> {
                Entity entity = mc.level == null ? null : mc.level.getEntity(payload.entityId());
                if (entity instanceof VesselEntity vessel) {
                    vessel.setClientStructure(payload.structure());
                } else {
                    VesselDataPayload.putPending(payload.entityId(), payload.structure());
                }
            });
        });
        ClientPlayNetworking.registerGlobalReceiver(VesselBlocksPayload.TYPE, (payload, context) -> {
            Minecraft mc = context.client();
            mc.execute(() -> {
                if (mc.level != null && mc.level.getEntity(payload.entityId()) instanceof VesselEntity vessel) {
                    vessel.applyBlockChanges(payload.positions(), payload.states());
                }
            });
        });
        ClientPlayNetworking.registerGlobalReceiver(VesselArrivalPayload.TYPE, (payload, context) -> {
            Minecraft mc = context.client();
            mc.execute(() -> {
                if (mc.player != null) {
                    arrival = new Arrival(payload.entityId(), payload.local(), mc.player.position(), new int[]{100});
                }
            });
        });
        ClientPlayNetworking.registerGlobalReceiver(VesselUseResultPayload.TYPE, (payload, context) -> {
            Minecraft mc = context.client();
            mc.execute(() -> useInAir(mc));
        });
        VesselEntity.clientUse = VesselClient::use;
        ClientTickEvents.START_CLIENT_TICK.register(mc -> {
            // taking the helm with a long click: the held use key must not reach the wheel again
            boolean atHelm = mc.player != null && mc.player.getVehicle() instanceof VesselEntity;
            if (atHelm && !wasAtHelm && mc.options.keyUse.isDown()) {
                helmUseHeld = true;
            }
            wasAtHelm = atHelm;
        });
        ClientTickEvents.END_CLIENT_TICK.register(mc -> {
            if (helmUseHeld && !mc.options.keyUse.isDown()) {
                helmUseHeld = false;
            }
            tickMining(mc);
        });
        ClientPlayConnectionEvents.DISCONNECT.register((handler, mc) -> {
            VesselColliders.clear(true);
            VesselDataPayload.clearPending();
            stopMining();
            arrival = null;
        });
    }

    /**
     * The start of the own player's tick (LocalPlayerTickMixin): after the server put the player onto a raft, wait in the
     * air where it put them until this client has the raft's blocks, then stand on the raft where the server meant
     * (its copy of the raft may be a little behind the server's). Without this the player falls through the deck while
     * the raft entity is still on its way (after a teleport from far away, a login or a dimension change).
     */
    public static void beforePlayerTick(LocalPlayer player) {
        Arrival a = arrival;
        if (a == null) {
            return;
        }
        if (player.isPassenger() || player.isSpectator() || a.ticksLeft()[0] <= 0) {
            arrival = null;
            return;
        }
        if (!(Minecraft.getInstance().screen instanceof net.minecraft.client.gui.screens.ReceivingLevelScreen)) {
            a.ticksLeft()[0]--;
        }
        if (player.level().getEntity(a.vessel()) instanceof VesselEntity vessel && vessel.structure() != null) {
            if (!vessel.isDocked()) {
                Vec3 at = vessel.currentPose().toWorld(a.local());
                player.setPos(at.x, at.y, at.z);
                player.setOldPosAndRot();
                player.setDeltaMovement(player.getDeltaMovement().multiply(1.0, 0.0, 1.0));
                player.resetFallDistance();
            }
            arrival = null;
        } else {
            player.setPos(a.hold().x, a.hold().y, a.hold().z);
            player.setDeltaMovement(Vec3.ZERO);
            player.resetFallDistance();
        }
    }

    /**
     * The server moved the own player (ClientPacketListenerTeleportMixin): a wait from an earlier move ends. A move
     * onto a raft is followed by its VesselArrivalPayload, which starts the wait again.
     */
    public static void movedByServer() {
        arrival = null;
    }

    /** The use key on a raft block: the server uses the block (or the held item on it) as if it were in the world. */
    private static InteractionResult use(VesselEntity vessel, InteractionHand hand) {
        BlockHitResult hit = VesselPicker.targetHit();
        VesselStructure structure = vessel.structure();
        if (VesselPicker.targetOf(vessel) == null || hit == null || structure == null || hand != InteractionHand.MAIN_HAND) {
            return InteractionResult.PASS;
        }
        if (hit.getBlockPos().equals(structure.helm())) {
            if (helmUseHeld) {
                return InteractionResult.SUCCESS;
            }
            helmUseHeld = true;
        }
        ClientPlayNetworking.send(new VesselUsePayload(vessel.getId(), hit.getBlockPos(), hit.getLocation(), hit.getDirection(), hit.isInside()));
        return InteractionResult.SUCCESS;
    }

    /** The server found nothing to do with the raft block: use the held item in the air, as vanilla does then. */
    private static void useInAir(Minecraft mc) {
        LocalPlayer player = mc.player;
        if (player == null || mc.gameMode == null || mc.screen != null || !mc.options.keyUse.isDown() || player.isUsingItem()) {
            return;
        }
        for (InteractionHand hand : InteractionHand.values()) {
            ItemStack held = player.getItemInHand(hand);
            if (held.isEmpty()) {
                continue;
            }
            InteractionResult result = mc.gameMode.useItem(player, hand);
            if (result.consumesAction()) {
                if (result.shouldSwing()) {
                    player.swing(hand);
                }
                mc.gameRenderer.itemInHandRenderer.itemUsed(hand);
                return;
            }
        }
    }

    // ------------------------------------------------------------------------------------------- breaking
    /**
     * Vanilla attacks the raft entity on a click (see MultiPlayerGameModeAttackMixin): in creative mode that breaks the
     * block under the crosshair at once, even for a click shorter than a tick (holding the key goes on in tickMining).
     */
    public static void attackedRaft(Entity entity) {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        BlockHitResult hit = VesselPicker.targetHit();
        if (!(entity instanceof VesselEntity vessel) || player == null || !player.isCreative() || miningCooldown > 0 || hit == null
                || VesselPicker.targetOf(vessel) == null) {
            return;
        }
        ClientPlayNetworking.send(new VesselBreakPayload(vessel.getId(), hit.getBlockPos(), false));
        ClientPlayNetworking.send(new VesselBreakPayload(vessel.getId(), hit.getBlockPos(), true));
        miningCooldown = 5;
        stopMining();
    }

    private static void stopMining() {
        miningVessel = -1;
        miningLocal = null;
        miningProgress = 0.0F;
        miningTicks = 0;
    }

    private static void tickMining(Minecraft mc) {
        LocalPlayer player = mc.player;
        if (player == null || mc.level == null || mc.screen != null || player.isSpectator() || !mc.options.keyAttack.isDown()
                || player.isUsingItem()) {
            stopMining();
            return;
        }
        if (miningCooldown > 0) {
            miningCooldown--;
            return;
        }
        VesselEntity vessel = VesselPicker.targetVessel();
        BlockHitResult hit = VesselPicker.targetHit();
        VesselStructure structure = vessel == null ? null : vessel.structure();
        BlockState state = structure == null || hit == null ? null : structure.get(hit.getBlockPos());
        if (state == null) {
            stopMining();
            return;
        }
        BlockPos local = hit.getBlockPos();
        if (vessel.getId() != miningVessel || !local.equals(miningLocal)) {
            stopMining();
            miningVessel = vessel.getId();
            miningLocal = local;
            ClientPlayNetworking.send(new VesselBreakPayload(vessel.getId(), local, false));
        }
        if (player.isCreative()) {
            ClientPlayNetworking.send(new VesselBreakPayload(vessel.getId(), local, true));
            miningCooldown = 5;
            stopMining();
            return;
        }
        float perTick = state.getDestroyProgress(player, EmptyBlockGetter.INSTANCE, local);
        if (perTick <= 0.0F) {
            return;
        }
        miningProgress += perTick;
        Vec3 at = vessel.currentPose().toWorld(hit.getLocation());
        if (miningTicks++ % 4 == 0) {
            SoundType sound = state.getSoundType();
            mc.level.playLocalSound(at.x, at.y, at.z, sound.getHitSound(), SoundSource.BLOCKS, (sound.getVolume() + 1.0F) / 8.0F,
                    sound.getPitch() * 0.5F, false);
            player.swing(InteractionHand.MAIN_HAND);
        }
        mc.level.addParticle(new BlockParticleOption(ParticleTypes.BLOCK, state), at.x, at.y, at.z, 0.0, 0.0, 0.0);
        if (miningProgress >= 1.0F) {
            ClientPlayNetworking.send(new VesselBreakPayload(vessel.getId(), local, true));
            miningCooldown = 5;
            stopMining();
        }
    }
}
