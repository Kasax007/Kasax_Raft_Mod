package net.kasax.raft.content.boss;

import net.kasax.raft.content.BcReg;
import net.kasax.raft.content.BcTabs;
import net.kasax.raft.content.boss.block.HeartOfTheTideBlock;
import net.kasax.raft.content.boss.block.HeartOfTheTideBlockEntity;
import net.kasax.raft.content.boss.block.LeviathanTrophyBlock;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;

/** Reward blocks of the Leviathan fight: the Heart of the Tide (aura) and the Leviathan trophy. */
public final class BossBlocks {
    public static Block HEART_OF_THE_TIDE;
    public static Block LEVIATHAN_TROPHY;
    public static BlockEntityType<HeartOfTheTideBlockEntity> HEART_OF_THE_TIDE_BE;

    private BossBlocks() {
    }

    static void register() {
        HEART_OF_THE_TIDE = BcReg.block("heart_of_the_tide", HeartOfTheTideBlock::new, BlockBehaviour.Properties.of()
                .mapColor(MapColor.DIAMOND).strength(3.0F, 1200.0F).lightLevel(state -> 15).sound(SoundType.AMETHYST)
                .noOcclusion().pushReaction(PushReaction.BLOCK).requiresCorrectToolForDrops());
        LEVIATHAN_TROPHY = BcReg.block("leviathan_trophy", LeviathanTrophyBlock::new, BlockBehaviour.Properties.of()
                .mapColor(MapColor.COLOR_CYAN).strength(2.0F, 6.0F).sound(SoundType.BONE_BLOCK).noOcclusion()
                .requiresCorrectToolForDrops());
        HEART_OF_THE_TIDE_BE = BcReg.blockEntity("heart_of_the_tide", HeartOfTheTideBlockEntity::new, HEART_OF_THE_TIDE);
    }

    static void registerItems() {
        BcReg.blockItem("heart_of_the_tide", HEART_OF_THE_TIDE, new Item.Properties().rarity(Rarity.EPIC).fireResistant().stacksTo(16),
                BcTabs.Tab.STORY, "tooltip.raft.heart_of_the_tide.1", "tooltip.raft.heart_of_the_tide.2");
        BcReg.blockItem("leviathan_trophy", LEVIATHAN_TROPHY, new Item.Properties().rarity(Rarity.RARE).fireResistant(),
                BcTabs.Tab.STORY, "tooltip.raft.leviathan_trophy");
    }
}
