package net.kasax.raft.world.dimension;

import net.kasax.raft.Raft;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.dimension.LevelStem;


public class ModDimensions {
    public static final ResourceKey<LevelStem> RAFTDIM_KEY = ResourceKey.create(Registries.LEVEL_STEM, Raft.id("raftdim"));
    public static final ResourceKey<Level> RAFTDIM_LEVEL_KEY = ResourceKey.create(Registries.DIMENSION, Raft.id("raftdim"));
    public static final ResourceKey<DimensionType> RAFT_DIM_TYPE = ResourceKey.create(Registries.DIMENSION_TYPE, Raft.id("raftdim_type"));

    // The dimension type JSON (data/raft/dimension_type/raftdim_type.json) is written by tools/port1211 for 1.21.1:
    // day and night like the overworld, sky light, ambient light 0, music and mood sounds come from the biomes.
}
