package net.kasax.raft.platform;

import com.mojang.serialization.Codec;
import java.util.function.Supplier;
import net.minecraft.core.Registry;
import net.minecraft.resources.Identifier;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

/** Fabric's AttachmentRegistry on NeoForge data attachments (same ids, same codecs). */
public final class AttachmentRegistry {
    /** The persistent attachments, for {@link #migrateFabricData}. */
    private static final java.util.List<AttachmentType<?>> PERSISTENT = new java.util.concurrent.CopyOnWriteArrayList<>();

    private AttachmentRegistry() {
    }

    /**
     * Entities saved by the Fabric build keep these attachments under {@code fabric:attachments}; they are taken over
     * when NeoForge has none of its own yet, so a player moved from a Fabric world keeps them (chunks: see
     * {@link #migrateFabricChunkData}).
     */
    public static void migrateFabricData(net.neoforged.neoforge.attachment.IAttachmentHolder holder, net.minecraft.world.level.storage.ValueInput input) {
        input.child("fabric:attachments").ifPresent(fabric -> {
            for (AttachmentType<?> type : PERSISTENT) {
                migrate(type, holder, fabric);
            }
        });
    }

    /**
     * Chunks saved by the Fabric build keep these attachments under {@code fabric:attachments} in the chunk data. Before
     * NeoForge reads the chunk, each one it does not have yet is copied into {@code neoforge:attachments} in NeoForge's
     * form ({@code {"value": <the same codec output>}}, see {@link Builder#buildAndRegister}).
     */
    public static void migrateFabricChunkData(net.minecraft.nbt.CompoundTag chunkData) {
        var fabric = chunkData.getCompound("fabric:attachments");
        if (fabric.isEmpty()) {
            return;
        }
        net.minecraft.nbt.CompoundTag neo = chunkData.getCompound(net.neoforged.neoforge.attachment.AttachmentHolder.ATTACHMENTS_NBT_KEY).orElse(null);
        for (AttachmentType<?> type : PERSISTENT) {
            String key = type.identifier().toString();
            net.minecraft.nbt.Tag value = fabric.get().get(key);
            if (value == null || neo != null && neo.contains(key)) {
                continue;
            }
            if (neo == null) {
                neo = new net.minecraft.nbt.CompoundTag();
                chunkData.put(net.neoforged.neoforge.attachment.AttachmentHolder.ATTACHMENTS_NBT_KEY, neo);
            }
            net.minecraft.nbt.CompoundTag wrapped = new net.minecraft.nbt.CompoundTag();
            wrapped.put("value", value.copy());
            neo.put(key, wrapped);
        }
    }

    private static <A> void migrate(AttachmentType<A> type, net.neoforged.neoforge.attachment.IAttachmentHolder holder,
                                    net.minecraft.world.level.storage.ValueInput fabric) {
        if (!type.has(holder)) {
            fabric.read(type.identifier().toString(), type.persistenceCodec()).ifPresent(value -> type.set(holder, value));
        }
    }

    public static <A> Builder<A> builder() {
        return new Builder<>();
    }

    public static final class Builder<A> {
        private Codec<A> codec;
        private boolean copyOnDeath;
        private Supplier<A> initializer;

        public Builder<A> persistent(Codec<A> codec) {
            this.codec = codec;
            return this;
        }

        public Builder<A> copyOnDeath() {
            this.copyOnDeath = true;
            return this;
        }

        public Builder<A> initializer(Supplier<A> initializer) {
            this.initializer = initializer;
            return this;
        }

        public AttachmentType<A> buildAndRegister(Identifier id) {
            Supplier<A> initial = initializer;
            var builder = net.neoforged.neoforge.attachment.AttachmentType.<A>builder(() -> initial == null ? null : initial.get());
            if (codec != null) {
                builder.serialize(codec.fieldOf("value"));
            }
            if (copyOnDeath) {
                builder.copyOnDeath();
            }
            var type = builder.build();
            Registry.register(NeoForgeRegistries.ATTACHMENT_TYPES, id, type);
            AttachmentType<A> result = new AttachmentType<>(id, type, codec, initializer);
            if (codec != null) {
                PERSISTENT.add(result);
            }
            return result;
        }
    }
}
