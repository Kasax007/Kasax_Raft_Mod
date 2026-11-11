package net.kasax.raft.content.cozy;

import net.kasax.raft.platform.ServerTickEvents;
import net.kasax.raft.Raft;
import net.kasax.raft.content.decor.entity.SeatEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.TagKey;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Comfort: a player who sits (any seat, cushion or chair) or lies in a hammock or bed for 3 seconds gets the Rested
 * effect. The comfort of the place is 1 (resting) plus one point for every kind of cozy thing within 6 blocks
 * (block tags {@code raft:comfort/<kind>}: seating, table, light, fire, rug, plant, shade, music, storage, wall decor,
 * water view). Rested lasts 2 minutes plus 1 minute per comfort point; resting keeps it topped up.
 */
public final class CozyComfort {
    public static final String[] KINDS = {"seating", "table", "light", "fire", "rug", "plant", "shade", "music", "storage",
            "wall_decor", "water_view"};
    private static final List<TagKey<Block>> TAGS = new ArrayList<>();
    private static final Map<UUID, Integer> RESTING = new HashMap<>();
    public static final int RADIUS = 6;

    static {
        for (String k : KINDS) TAGS.add(TagKey.create(Registries.BLOCK, Raft.id("comfort/" + k)));
    }

    private CozyComfort() {
    }

    public static void init() {
        ServerTickEvents.END_SERVER_TICK.register(CozyComfort::tick);
    }

    private static void tick(MinecraftServer server) {
        if (server.getTickCount() % 20 != 0) return;
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            boolean resting = player.getVehicle() instanceof SeatEntity || player.isSleeping();
            if (!resting) {
                RESTING.remove(player.getUUID());
                continue;
            }
            int t = RESTING.merge(player.getUUID(), 20, Integer::sum);
            if (t == 60 || (t > 60 && (t - 60) % 600 == 0)) rest(player);
        }
    }

    /** Comfort of the place around pos: 1 + number of comfort kinds found nearby. */
    public static int comfort(ServerLevel level, BlockPos pos) {
        boolean[] found = new boolean[TAGS.size()];
        int count = 0;
        for (BlockPos p : BlockPos.betweenClosed(pos.offset(-RADIUS, -2, -RADIUS), pos.offset(RADIUS, 4, RADIUS))) {
            BlockState state = level.getBlockState(p);
            if (state.isAir()) continue;
            for (int i = 0; i < TAGS.size(); i++) {
                if (!found[i] && state.is(TAGS.get(i))) {
                    found[i] = true;
                    count++;
                }
            }
            if (count == TAGS.size()) break;
        }
        return 1 + count;
    }

    private static void rest(ServerPlayer player) {
        ServerLevel level = player.serverLevel();
        int comfort = comfort(level, player.blockPosition());
        int minutes = 2 + comfort;
        boolean fresh = !player.hasEffect(CozyContent.RESTED);
        player.addEffect(new MobEffectInstance(CozyContent.RESTED, minutes * 1200, 0, true, true, true));
        if (comfort <= 2) {
            player.displayClientMessage(Component.translatable("message.raft.comfort.low", comfort), true);
        } else {
            player.displayClientMessage(Component.translatable("message.raft.comfort", comfort, minutes), true);
        }
        if (fresh) {
            level.playSound(null, player.blockPosition(), CozyContent.RESTED_CHIME, SoundSource.PLAYERS, 0.5F, 1.0F);
        }
        Raft.LOGGER.debug("Beach Craft: {} rests with comfort {}", player.getName().getString(), comfort);
    }
}
