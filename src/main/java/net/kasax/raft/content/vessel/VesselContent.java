package net.kasax.raft.content.vessel;

import net.kasax.raft.Raft;
import net.kasax.raft.content.BcReg;
import net.kasax.raft.content.BcTabs;
import net.kasax.raft.content.vessel.block.HelmBlock;
import net.kasax.raft.content.vessel.block.MastBlock;
import net.kasax.raft.content.vessel.block.AnchorWinchBlock;
import net.kasax.raft.content.vessel.block.OarBlock;
import net.kasax.raft.content.vessel.block.RaftMotorBlock;
import net.kasax.raft.content.vessel.block.RaftMotorBlockEntity;
import net.kasax.raft.content.vessel.block.SailBlock;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;

/** Registrations of the vessel module: the helm, sail, oar and raft motor blocks and the sailing raft entity. */
public final class VesselContent {
    private VesselContent() {
    }

    /** Blocks a raft floats on (its hull): raft foundations. */
    public static final TagKey<Block> HULLS = TagKey.create(Registries.BLOCK, Raft.id("vessel_hulls"));
    /** Blocks that can never go to sea (portals, bedrock, command blocks, ...): a raft with one of them stays put. */
    public static final TagKey<Block> NOT_SEAWORTHY = TagKey.create(Registries.BLOCK, Raft.id("not_seaworthy"));

    public static final Block HELM = BcReg.blockWithItem("helm", HelmBlock::new,
            BlockBehaviour.Properties.of().mapColor(MapColor.WOOD).strength(1.5F).sound(SoundType.WOOD).noOcclusion()
                    .ignitedByLava(),
            BcTabs.Tab.VOYAGE, "tooltip.raft.helm", "tooltip.raft.helm.1");
    public static final Block SAIL = BcReg.blockWithItem("sail", SailBlock::new,
            BlockBehaviour.Properties.of().mapColor(MapColor.WOOL).strength(0.8F).sound(SoundType.WOOL).noOcclusion()
                    .ignitedByLava(),
            BcTabs.Tab.VOYAGE, "tooltip.raft.sail");
    public static final Block MAST = BcReg.blockWithItem("mast", MastBlock::new,
            BlockBehaviour.Properties.of().mapColor(MapColor.WOOD).strength(1.5F).sound(SoundType.WOOD).noOcclusion()
                    .ignitedByLava(),
            BcTabs.Tab.VOYAGE, "tooltip.raft.mast");
    public static final Block ANCHOR_WINCH = BcReg.blockWithItem("anchor_winch", AnchorWinchBlock::new,
            BlockBehaviour.Properties.of().mapColor(MapColor.METAL).strength(2.0F, 6.0F).sound(SoundType.METAL).noOcclusion(),
            BcTabs.Tab.VOYAGE, "tooltip.raft.anchor_winch", "tooltip.raft.anchor_winch.1");
    public static final Block OAR = BcReg.blockWithItem("oar", OarBlock::new,
            BlockBehaviour.Properties.of().mapColor(MapColor.WOOD).strength(1.0F).sound(SoundType.WOOD).noOcclusion()
                    .ignitedByLava(),
            BcTabs.Tab.VOYAGE, "tooltip.raft.oar");
    public static final Block RAFT_MOTOR = BcReg.blockWithItem("raft_motor", RaftMotorBlock::new,
            BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_RED).strength(2.0F, 6.0F).sound(SoundType.METAL).noOcclusion(),
            BcTabs.Tab.VOYAGE, "tooltip.raft.raft_motor", "tooltip.raft.raft_motor.1");

    public static final BlockEntityType<RaftMotorBlockEntity> RAFT_MOTOR_BE = BcReg.blockEntity("raft_motor",
            RaftMotorBlockEntity::new, RAFT_MOTOR);

    /** The raft motor's fuel bay (fuel slot, flame, running time). */
    public static final net.minecraft.world.inventory.MenuType<RaftMotorMenu> RAFT_MOTOR_MENU = net.minecraft.core.Registry.register(
            net.minecraft.core.registries.BuiltInRegistries.MENU, Raft.id("raft_motor"),
            new net.minecraft.world.inventory.MenuType<>(RaftMotorMenu::new, net.minecraft.world.flag.FeatureFlags.VANILLA_SET));

    /** The sailing raft. Created only by a helm (no summon); its size comes from its blocks, not from the type. */
    public static final EntityType<VesselEntity> VESSEL = BcReg.entity("vessel",
            EntityType.Builder.<VesselEntity>of(VesselEntity::new, MobCategory.MISC).noSummon().fireImmune().sized(1.0F, 1.0F).clientTrackingRange(10).updateInterval(1));

    /** Loads the class (all fields above register themselves). */
    public static void register() {
    }
}
