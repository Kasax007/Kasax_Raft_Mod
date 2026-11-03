package net.kasax.raft.content.story;

import net.kasax.raft.Raft;
import net.kasax.raft.content.BcReg;
import net.kasax.raft.content.BcTabs;
import net.kasax.raft.content.story.block.ExpeditionRadioBlock;
import net.kasax.raft.content.story.block.LeviathanAltarBlock;
import net.kasax.raft.content.story.block.LeviathanAltarBlockEntity;
import net.kasax.raft.content.story.block.ResearchTerminalBlock;
import net.kasax.raft.content.story.block.ResearchTerminalBlockEntity;
import net.kasax.raft.content.story.block.SigilPedestalBlock;
import net.kasax.raft.content.story.block.TideSealBlock;
import net.kasax.raft.content.story.block.TidewardenAltarBlock;
import net.kasax.raft.content.story.item.ExpeditionJournalItem;
import net.kasax.raft.content.story.item.MessageInABottleItem;
import net.kasax.raft.content.story.item.RelicItem;
import net.kasax.raft.content.story.item.SupplyBarrelItem;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.tags.TagKey;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.component.DamageResistant;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.TransparentBlock;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;

/** Items, blocks, block entities and sounds of the story module (docs/design/beachcraft3.md, section 9). */
public final class StoryContent {
    /** Damage types that sigils and the key ignore as item entities: fire, lava, explosions, lightning, cactus. */
    public static final TagKey<DamageType> RELIC_RESISTANT = TagKey.create(Registries.DAMAGE_TYPE, Raft.id("relic_resistant"));
    /** Hardness and blast resistance of the unbreakable sanctum blocks (like bedrock). */
    public static final float UNBREAKABLE = -1.0F;
    public static final float BLAST_PROOF = 3_600_000.0F;

    public static Item EXPEDITION_JOURNAL;
    public static Item MESSAGE_IN_A_BOTTLE;
    public static Item SUPPLY_BARREL;
    public static Item CORAL_SIGIL;
    public static Item PEARL_SIGIL;
    public static Item STORM_SIGIL;
    public static Item EMBER_SIGIL;
    public static Item TIDEWARDEN_KEY;

    public static Block TIDEWARDEN_ALTAR;
    public static Block SIGIL_PEDESTAL;
    public static Block RESEARCH_TERMINAL;
    public static Block EXPEDITION_RADIO;
    public static Block TIDE_SEAL;
    public static Block SANCTUM_TIDESTONE;
    public static Block SANCTUM_GLASS;
    public static Block LEVIATHAN_ALTAR;

    public static BlockEntityType<ResearchTerminalBlockEntity> RESEARCH_TERMINAL_BE;
    public static BlockEntityType<LeviathanAltarBlockEntity> LEVIATHAN_ALTAR_BE;

    public static SoundEvent RADIO_STATIC;

    private StoryContent() {
    }

    private static Item.Properties relic(Rarity rarity) {
        return new Item.Properties().stacksTo(1).rarity(rarity)
                .component(DataComponents.ENCHANTMENT_GLINT_OVERRIDE, true)
                .delayedComponent(DataComponents.DAMAGE_RESISTANT, context -> new DamageResistant(context.getOrThrow(RELIC_RESISTANT)));
    }

    private static BlockBehaviour.Properties sanctum() {
        return BlockBehaviour.Properties.of().strength(UNBREAKABLE, BLAST_PROOF)
                .pushReaction(PushReaction.IMMOVEABLE).isValidSpawn(Blocks::never);
    }

    static void register() {
        BcTabs.Tab tab = BcTabs.Tab.STORY;
        // ---- items
        EXPEDITION_JOURNAL = BcReg.item("expedition_journal", ExpeditionJournalItem::new,
                new Item.Properties().stacksTo(1).rarity(Rarity.UNCOMMON), tab);
        MESSAGE_IN_A_BOTTLE = BcReg.item("message_in_a_bottle", MessageInABottleItem::new,
                new Item.Properties().stacksTo(16), tab);
        SUPPLY_BARREL = BcReg.item("supply_barrel", SupplyBarrelItem::new, new Item.Properties().stacksTo(16), tab);
        CORAL_SIGIL = BcReg.item("coral_sigil", p -> new RelicItem(p, "sigil"), relic(Rarity.EPIC), tab);
        PEARL_SIGIL = BcReg.item("pearl_sigil", p -> new RelicItem(p, "sigil"), relic(Rarity.EPIC), tab);
        STORM_SIGIL = BcReg.item("storm_sigil", p -> new RelicItem(p, "sigil"), relic(Rarity.EPIC), tab);
        EMBER_SIGIL = BcReg.item("ember_sigil", p -> new RelicItem(p, "sigil"), relic(Rarity.EPIC), tab);
        TIDEWARDEN_KEY = BcReg.item("tidewarden_key", p -> new RelicItem(p, "tidewarden_key"), relic(Rarity.EPIC), tab);

        // ---- blocks
        TIDEWARDEN_ALTAR = BcReg.blockWithItem("tidewarden_altar", TidewardenAltarBlock::new,
                BlockBehaviour.Properties.of().mapColor(MapColor.WARPED_NYLIUM).strength(5.0F, 1200.0F)
                        .requiresCorrectToolForDrops().sound(SoundType.DEEPSLATE_BRICKS)
                        .lightLevel(TidewardenAltarBlock::light),
                tab, "tooltip.raft.tidewarden_altar.0");
        SIGIL_PEDESTAL = BcReg.blockWithItem("sigil_pedestal", SigilPedestalBlock::new,
                BlockBehaviour.Properties.of().mapColor(MapColor.WARPED_NYLIUM).strength(2.5F, 9.0F)
                        .requiresCorrectToolForDrops().sound(SoundType.DEEPSLATE_BRICKS).noOcclusion(), tab);
        RESEARCH_TERMINAL = BcReg.blockWithItem("research_terminal", ResearchTerminalBlock::new,
                BlockBehaviour.Properties.of().mapColor(MapColor.SAND).strength(2.0F, 6.0F)
                        .requiresCorrectToolForDrops().sound(SoundType.COPPER).noOcclusion().lightLevel(s -> 6),
                tab, "tooltip.raft.research_terminal.0");
        EXPEDITION_RADIO = BcReg.blockWithItem("expedition_radio", ExpeditionRadioBlock::new,
                BlockBehaviour.Properties.of().mapColor(MapColor.WOOD).strength(1.5F, 3.0F)
                        .sound(SoundType.WOOD).noOcclusion(),
                tab, "tooltip.raft.expedition_radio.0");
        TIDE_SEAL = BcReg.blockWithItem("tide_seal", TideSealBlock::new,
                sanctum().mapColor(MapColor.DIAMOND).sound(SoundType.AMETHYST).noOcclusion()
                        .lightLevel(s -> s.getValue(TideSealBlock.OPEN) ? 3 : 8)
                        .isRedstoneConductor((s, l, p) -> false).isSuffocating((s, l, p) -> false)
                        .isViewBlocking((s, l, p, b) -> false),
                tab, "tooltip.raft.tide_seal.0", "tooltip.raft.sanctum.0");
        SANCTUM_TIDESTONE = BcReg.blockWithItem("sanctum_tidestone", Block::new,
                sanctum().mapColor(MapColor.WARPED_NYLIUM).sound(SoundType.DEEPSLATE_BRICKS),
                tab, "tooltip.raft.sanctum.0");
        SANCTUM_GLASS = BcReg.blockWithItem("sanctum_glass", TransparentBlock::new,
                sanctum().mapColor(MapColor.COLOR_CYAN).sound(SoundType.GLASS).noOcclusion()
                        .isRedstoneConductor((s, l, p) -> false).isSuffocating((s, l, p) -> false)
                        .isViewBlocking((s, l, p, b) -> false),
                tab, "tooltip.raft.sanctum.0");
        LEVIATHAN_ALTAR = BcReg.blockWithItem("leviathan_altar", LeviathanAltarBlock::new,
                sanctum().mapColor(MapColor.COLOR_CYAN).sound(SoundType.DEEPSLATE_BRICKS).noOcclusion()
                        .lightLevel(s -> s.getValue(LeviathanAltarBlock.ACTIVE) ? 12 : 4),
                tab, "tooltip.raft.leviathan_altar.0", "tooltip.raft.sanctum.0");

        RESEARCH_TERMINAL_BE = BcReg.blockEntity("research_terminal", ResearchTerminalBlockEntity::new, RESEARCH_TERMINAL);
        LEVIATHAN_ALTAR_BE = BcReg.blockEntity("leviathan_altar", LeviathanAltarBlockEntity::new, LEVIATHAN_ALTAR);

        RADIO_STATIC = BcReg.sound("radio_static");

        BcTabs.icon(tab, () -> new ItemStack(EXPEDITION_JOURNAL));
    }

    /** The sigil item of an altar slot name (coral, pearl, storm, ember). */
    public static Item sigil(String name) {
        return switch (name) {
            case "coral" -> CORAL_SIGIL;
            case "pearl" -> PEARL_SIGIL;
            case "storm" -> STORM_SIGIL;
            default -> EMBER_SIGIL;
        };
    }
}
