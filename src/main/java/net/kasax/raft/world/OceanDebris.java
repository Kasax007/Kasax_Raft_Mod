package net.kasax.raft.world;

import net.kasax.raft.platform.ServerTickEvents;
import net.kasax.raft.Raft;
import net.kasax.raft.block.ModBlocks;
import net.kasax.raft.content.story.StoryContent;
import net.kasax.raft.item.ModItems;
import net.kasax.raft.world.dimension.ModDimensions;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

/**
 * Floating ocean debris of the raft dimension. Every 30 seconds each player in the dimension may get a single
 * piece (one item) that appears 20 to 40 blocks upstream and drifts past them with the current. Debris is limited
 * per player (a few pieces around them) and despawns when it is old or far away from every player. 3.2: about a
 * tenth of 3.1's amount, one item per piece, rarer barrel loot: a nice find, not a stream of items. An item catcher
 * on a sailing raft collects debris it sails through.
 */
public class OceanDebris {
    /** Scoreboard tag of debris item entities. */
    public static final String TAG = "raft_debris";
    public static final ResourceKey<LootTable> BARREL_LOOT = ResourceKey.create(Registries.LOOT_TABLE, Raft.id("gameplay/ocean_barrel"));

    public static final int SPAWN_INTERVAL_TICKS = 600;
    /** Chance that a spawn attempt brings a piece at all. */
    public static final float SPAWN_CHANCE = 0.6f;
    public static final int MIN_DISTANCE = 20;
    public static final int MAX_DISTANCE = 40;
    /** The debris limit per player, counted within {@link #COUNT_RADIUS} blocks. */
    public static final int MAX_PER_PLAYER = 5;
    public static final double COUNT_RADIUS = 96.0;
    /** The current flows towards positive x (east). */
    public static final double CURRENT_SPEED = 0.07;
    public static final int MAX_AGE_TICKS = 2400;
    public static final double DESPAWN_DISTANCE = 128.0;
    /** Chance for a barrel loot item instead of a plain material. */
    public static final float BARREL_CHANCE = 0.03f;
    /** Rare story debris: a message in a bottle (a lore letter) and a floating supply barrel. */
    public static final float BOTTLE_CHANCE = 0.015f;
    public static final float SUPPLY_BARREL_CHANCE = 0.01f;

    public static void register() {
        ServerTickEvents.END_LEVEL_TICK.register(OceanDebris::tick);
    }

    private static void tick(ServerLevel level) {
        if (!level.dimension().equals(ModDimensions.RAFTDIM_LEVEL_KEY) || level.players().isEmpty()) {
            return;
        }
        long time = level.getGameTime();
        if (time % 2 == 0) {
            driftAndDespawn(level);
        }
        if (time % SPAWN_INTERVAL_TICKS == 0) {
            for (ServerPlayer player : level.players()) {
                if (!player.isSpectator()) {
                    spawnFor(level, player);
                }
            }
        }
    }

    public static boolean isDebris(Entity entity) {
        return entity instanceof ItemEntity && entity.entityTags().contains(TAG);
    }

    private static void driftAndDespawn(ServerLevel level) {
        List<ItemEntity> debris = new ArrayList<>();
        for (Entity entity : level.getAllEntities()) {
            if (isDebris(entity)) {
                debris.add((ItemEntity) entity);
            }
        }
        for (ItemEntity item : debris) {
            if (item.tickCount > MAX_AGE_TICKS || !hasPlayerNearby(level, item)) {
                item.discard();
                continue;
            }
            if (item.isInWater()) {
                Vec3 motion = item.getDeltaMovement();
                // the current slowly takes the item up to its own speed, steering is damped
                item.setDeltaMovement(motion.x + (CURRENT_SPEED - motion.x) * 0.25, motion.y, motion.z * 0.9);
            }
        }
    }

    private static boolean hasPlayerNearby(ServerLevel level, Entity entity) {
        for (ServerPlayer player : level.players()) {
            if (player.distanceToSqr(entity) < DESPAWN_DISTANCE * DESPAWN_DISTANCE) {
                return true;
            }
        }
        return false;
    }

    /** Number of debris items around a player. */
    public static int countAround(ServerLevel level, Entity center) {
        AABB area = center.getBoundingBox().inflate(COUNT_RADIUS);
        return level.getEntitiesOfClass(ItemEntity.class, area, OceanDebris::isDebris).size();
    }

    private static void spawnFor(ServerLevel level, ServerPlayer player) {
        if (countAround(level, player) >= MAX_PER_PLAYER) {
            return;
        }
        RandomSource random = level.getRandom();
        int count = random.nextFloat() < SPAWN_CHANCE ? 1 : 0;
        for (int i = 0; i < count; i++) {
            double distance = MIN_DISTANCE + random.nextDouble() * (MAX_DISTANCE - MIN_DISTANCE);
            double x = player.getX() - distance;
            double z = player.getZ() + (random.nextDouble() - 0.5) * 12.0;
            spawnDebrisAt(level, x, z);
        }
    }

    /** Spawns one debris item floating on the water at the given column, if there is water. */
    public static ItemEntity spawnDebrisAt(ServerLevel level, double x, double z) {
        BlockPos column = BlockPos.containing(x, 0, z);
        if (!level.hasChunkAt(column)) {
            return null;
        }
        int surface = level.getHeight(Heightmap.Types.WORLD_SURFACE, column.getX(), column.getZ());
        BlockPos water = new BlockPos(column.getX(), surface - 1, column.getZ());
        if (!level.getFluidState(water).is(FluidTags.WATER)) {
            return null;
        }
        ItemStack stack = randomDebris(level, level.getRandom());
        if (stack.isEmpty()) {
            return null;
        }
        ItemEntity item = new ItemEntity(level, x, surface - 0.45, z, stack, CURRENT_SPEED, 0.0, 0.0);
        item.addTag(TAG);
        level.addFreshEntity(item);
        return item;
    }

    public static ItemStack randomDebris(ServerLevel level, RandomSource random) {
        float story = random.nextFloat();
        if (story < BOTTLE_CHANCE) {
            return new ItemStack(StoryContent.MESSAGE_IN_A_BOTTLE);
        } else if (story < BOTTLE_CHANCE + SUPPLY_BARREL_CHANCE) {
            return new ItemStack(StoryContent.SUPPLY_BARREL);
        }
        if (random.nextFloat() < BARREL_CHANCE) {
            LootTable table = level.getServer().reloadableRegistries().getLootTable(BARREL_LOOT);
            List<ItemStack> loot = table.getRandomItems(new LootParams.Builder(level).create(LootContextParamSets.EMPTY));
            if (!loot.isEmpty()) {
                return loot.getFirst();
            }
        }
        // one item per piece: driftwood and leaves are common, plastic less, scrap metal rare
        int roll = random.nextInt(100);
        if (roll < 38) {
            return new ItemStack(ModBlocks.DRIFTWOOD_PLANKS);
        } else if (roll < 66) {
            return new ItemStack(ModBlocks.PALM_LEAVES);
        } else if (roll < 88) {
            return new ItemStack(ModItems.PLASTIC);
        } else {
            return new ItemStack(ModItems.SCRAP_METAL);
        }
    }
}
