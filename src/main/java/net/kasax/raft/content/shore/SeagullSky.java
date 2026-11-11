package net.kasax.raft.content.shore;

import net.kasax.raft.platform.ServerTickEvents;
import net.kasax.raft.content.shore.entity.SeagullEntity;
import net.kasax.raft.world.dimension.ModDimensions;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;

/**
 * Seagulls in the sky of the Endless Sea (3.2). Natural spawning only puts gulls on island beaches when their chunks
 * are made (they are creatures), so a player out at sea or travelling fast rarely saw one. Now every 20 seconds a
 * player in the dimension may get a small flock (3 to 5) flying in 30 to 48 blocks away, high over the water,
 * as long as fewer than {@link #CAP} of these sky gulls are around them. These sky gulls fly off (are removed) when no player is
 * within {@link #DESPAWN_DISTANCE}; gulls born on the islands stay as they are.
 */
public final class SeagullSky {
    public static final String TAG = "raft_sky_gull";
    public static final int INTERVAL_TICKS = 400;
    public static final float CHANCE = 0.55F;
    public static final int CAP = 6;
    public static final double COUNT_RADIUS = 96.0;
    public static final double DESPAWN_DISTANCE = 160.0;

    private SeagullSky() {
    }

    public static void register() {
        // (environment BC_NO_SKY_GULLS: off, only for before/after counts in tests)
        if (System.getenv("BC_NO_SKY_GULLS") == null) {
            ServerTickEvents.END_WORLD_TICK.register(SeagullSky::tick);
        }
    }

    private static void tick(ServerLevel level) {
        if (!level.dimension().equals(ModDimensions.RAFTDIM_LEVEL_KEY) || level.players().isEmpty()) {
            return;
        }
        long time = level.getGameTime();
        if (time % 100 == 37) {
            for (Entity e : level.getAllEntities()) {
                if (e instanceof SeagullEntity gull && gull.getTags().contains(TAG) && !playerNear(level, gull)) {
                    gull.discard();
                }
            }
        }
        if (time % INTERVAL_TICKS != 0) {
            return;
        }
        for (ServerPlayer player : level.players()) {
            if (player.isSpectator() || level.getRandom().nextFloat() >= CHANCE) {
                continue;
            }
            // only the sky flocks count: the gulls sitting on a nearby island beach (8 or more around many islands)
            // used to fill the cap, so a player near land never saw a flock come in
            int around = level.getEntitiesOfClass(SeagullEntity.class, player.getBoundingBox().inflate(COUNT_RADIUS),
                    g -> g.getTags().contains(TAG)).size();
            if (around < CAP) {
                spawnFlock(level, player, Math.min(CAP - around, 3 + level.getRandom().nextInt(3)));
            }
        }
    }

    private static boolean playerNear(ServerLevel level, Entity e) {
        for (ServerPlayer p : level.players()) {
            if (p.distanceToSqr(e) < DESPAWN_DISTANCE * DESPAWN_DISTANCE) {
                return true;
            }
        }
        return false;
    }

    /** A flock in the air: around a point 30-48 blocks from the player, 18-30 blocks above the sea or the ground. */
    public static int spawnFlock(ServerLevel level, ServerPlayer player, int count) {
        RandomSource random = level.getRandom();
        float angle = random.nextFloat() * Mth.TWO_PI;
        double dist = 30 + random.nextDouble() * 18;
        double cx = player.getX() + Mth.cos(angle) * dist;
        double cz = player.getZ() + Mth.sin(angle) * dist;
        BlockPos column = BlockPos.containing(cx, 0, cz);
        if (!level.hasChunkAt(column)) {
            return 0;
        }
        int ground = level.getHeight(Heightmap.Types.MOTION_BLOCKING, column.getX(), column.getZ());
        double y = Math.max(level.getSeaLevel(), ground) + 18 + random.nextInt(12);
        int made = 0;
        for (int i = 0; i < count; i++) {
            SeagullEntity gull = net.kasax.raft.content.shore.ShoreEntities.SEAGULL.create(level);
            if (gull == null) {
                continue;
            }
            gull.moveTo(cx + random.nextGaussian() * 2.5, y + random.nextGaussian(), cz + random.nextGaussian() * 2.5,
                    random.nextFloat() * 360.0F, 0.0F);
            if (!level.noCollision(gull, new AABB(gull.blockPosition()).inflate(0.5))) {
                continue;
            }
            gull.addTag(TAG);
            level.addFreshEntity(gull);
            made++;
        }
        return made;
    }
}
