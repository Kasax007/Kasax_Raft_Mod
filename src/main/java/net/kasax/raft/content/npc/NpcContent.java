package net.kasax.raft.content.npc;

import com.mojang.serialization.Codec;
import net.kasax.raft.platform.AttachmentRegistry;
import net.kasax.raft.platform.AttachmentType;
import net.kasax.raft.platform.FabricDefaultAttributeRegistry;
import net.kasax.raft.Raft;
import net.kasax.raft.content.BcReg;
import net.kasax.raft.content.BcTabs;
import net.kasax.raft.content.npc.entity.NpcEntity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.SpawnEggItem;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Entity types (one per character), spawn eggs and the per-player memory of the npc module. */
public final class NpcContent {
    public static final Map<NpcCharacter, EntityType<NpcEntity>> TYPES = new EnumMap<>(NpcCharacter.class);
    private static final Map<EntityType<?>, NpcCharacter> BY_TYPE = new HashMap<>();

    /**
     * The people a player has talked to: character id -> game time of the last gift (0 when there was none). Kept
     * through death, so gifts are not handed out twice.
     */
    public static final AttachmentType<Map<String, Long>> MET = AttachmentRegistry.<Map<String, Long>>builder()
            .persistent(Codec.unboundedMap(Codec.STRING, Codec.LONG))
            .copyOnDeath()
            .buildAndRegister(Raft.id("npc_met"));

    /**
     * Structure spots that were checked for their person (chunk attachment, see {@link NpcRetrofit}): an older
     * structure gets its person once, a person removed later is not brought back.
     */
    public static final AttachmentType<List<String>> SPOTS_DONE = AttachmentRegistry.<List<String>>builder()
            .persistent(Codec.STRING.listOf())
            .buildAndRegister(Raft.id("npc_spots_done"));

    private NpcContent() {
    }

    static void register() {
        for (NpcCharacter c : NpcCharacter.values()) {
            EntityType<NpcEntity> type = BcReg.entity(c.id, EntityType.Builder.<NpcEntity>of(NpcEntity::new, MobCategory.MISC)
                    .sized(0.6F, 1.95F).eyeHeight(1.62F).clientTrackingRange(10).fireImmune().noLootTable());
            TYPES.put(c, type);
            BY_TYPE.put(type, c);
            FabricDefaultAttributeRegistry.register(type, NpcEntity.createAttributes());
        }
        for (NpcCharacter c : NpcCharacter.values()) {
            BcReg.item(c.id + "_spawn_egg", SpawnEggItem::new, new Item.Properties().spawnEgg(TYPES.get(c)), BcTabs.Tab.CREATURES);
        }
    }

    /** The character of an NPC entity type (null for other types). */
    public static NpcCharacter character(EntityType<?> type) {
        return BY_TYPE.get(type);
    }
}
