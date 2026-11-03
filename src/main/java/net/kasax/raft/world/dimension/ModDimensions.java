package net.kasax.raft.world.dimension;

import net.kasax.raft.Raft;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.HolderSet;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.resources.ResourceKey;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.ARGB;
import net.minecraft.util.valueproviders.ConstantInt;
import net.minecraft.world.attribute.AmbientSounds;
import net.minecraft.world.attribute.BackgroundMusic;
import net.minecraft.world.attribute.BedRule;
import net.minecraft.world.attribute.EnvironmentAttributeMap;
import net.minecraft.world.attribute.EnvironmentAttributes;
import net.minecraft.world.level.CardinalLighting;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.dimension.LevelStem;

import java.util.Optional;

public class ModDimensions {
    public static final ResourceKey<LevelStem> RAFTDIM_KEY = ResourceKey.create(Registries.LEVEL_STEM, Raft.id("raftdim"));
    public static final ResourceKey<Level> RAFTDIM_LEVEL_KEY = ResourceKey.create(Registries.DIMENSION, Raft.id("raftdim"));
    public static final ResourceKey<DimensionType> RAFT_DIM_TYPE = ResourceKey.create(Registries.DIMENSION_TYPE, Raft.id("raftdim_type"));

    public static void bootstrapType(BootstrapContext<DimensionType> context) {
        HolderGetter<Block> blocks = context.lookup(Registries.BLOCK);

        // Endless bright day: no time of day (so no timelines and no clock), full sky light.
        EnvironmentAttributeMap attributes = EnvironmentAttributeMap.builder()
                .set(EnvironmentAttributes.SKY_COLOR, ARGB.vector3fFromRGB24(0x6cbaea))
                .set(EnvironmentAttributes.FOG_COLOR, ARGB.vector3fFromRGB24(0x22a1e6))
                .set(EnvironmentAttributes.AMBIENT_LIGHT_COLOR, ARGB.vector3fFromRGB24(0x0A0A0A))
                .set(EnvironmentAttributes.CLOUD_COLOR, ARGB.vector4fFromARGB32(ARGB.white(0.8F)))
                .set(EnvironmentAttributes.CLOUD_HEIGHT, 192.33F)
                .set(EnvironmentAttributes.BACKGROUND_MUSIC, BackgroundMusic.OVERWORLD)
                .set(EnvironmentAttributes.BED_RULE, BedRule.CAN_SLEEP_WHEN_DARK)
                .set(EnvironmentAttributes.STRAW_BED_RULE, BedRule.DESTROY_ON_LEAVE)
                .set(EnvironmentAttributes.RESPAWN_ANCHOR_WORKS, false)
                .set(EnvironmentAttributes.AMBIENT_SOUNDS, AmbientSounds.LEGACY_CAVE_SETTINGS)
                // without timelines both angles would default to 0: the full moon drawn right on top of the sun
                .set(EnvironmentAttributes.MOON_ANGLE, 180F)
                // eternal day: undead burn like at noon in the overworld (vanilla only sets this through the day timeline)
                .set(EnvironmentAttributes.MONSTERS_BURN, true)
                .build();

        context.register(RAFT_DIM_TYPE, new DimensionType(
                true, // hasFixedTime
                true, // hasSkyLight
                false, // hasCeiling
                false, // hasEnderDragonFight
                1.0, // coordinateScale
                -64, // minY
                320, // height
                320, // logicalHeight (portals and the raft platform up to Y 220 need the whole height)
                blocks.getOrThrow(BlockTags.INFINIBURN_OVERWORLD), // infiniburn
                0.0f, // ambientLight: like the overworld, so the deep sea, trench and caves get dark
                new DimensionType.MonsterSettings(ConstantInt.of(0), 0),
                DimensionType.Skybox.OVERWORLD,
                CardinalLighting.Type.DEFAULT,
                attributes,
                HolderSet.empty(), // timelines
                Optional.empty())); // default clock
    }
}
