package net.kasax.raft.platform;

import com.mojang.serialization.Codec;
import java.util.function.Supplier;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

/** Fabric's AttachmentRegistry on NeoForge data attachments (same ids, same codecs). */
public final class AttachmentRegistry {
    /** The persistent attachments, for {@link #migrateFabricData}. */
    private static final java.util.List<AttachmentType<?>> PERSISTENT = new java.util.concurrent.CopyOnWriteArrayList<>();

    private AttachmentRegistry() {
    }

    /**
     * Worlds from the Fabric build keep these attachments under {@code fabric:attachments} (entities and chunks); they are
     * taken over when NeoForge has none of its own yet, so a world moved from Fabric keeps them.
     */
    public static void migrateFabricData(net.neoforged.neoforge.attachment.IAttachmentHolder holder, net.minecraft.nbt.CompoundTag tag,
                                         net.minecraft.core.HolderLookup.Provider registries) {
        if (!tag.contains("fabric:attachments", net.minecraft.nbt.Tag.TAG_COMPOUND)) {
            return;
        }
        net.minecraft.nbt.CompoundTag fabric = tag.getCompound("fabric:attachments");
        var ops = registries.createSerializationContext(net.minecraft.nbt.NbtOps.INSTANCE);
        for (AttachmentType<?> type : PERSISTENT) {
            migrate(type, holder, fabric, ops);
        }
    }

    private static <A> void migrate(AttachmentType<A> type, net.neoforged.neoforge.attachment.IAttachmentHolder holder, net.minecraft.nbt.CompoundTag fabric,
                                    com.mojang.serialization.DynamicOps<net.minecraft.nbt.Tag> ops) {
        String key = type.identifier().toString();
        if (fabric.contains(key) && !type.has(holder)) {
            type.persistenceCodec().parse(ops, fabric.get(key)).result().ifPresent(value -> type.set(holder, value));
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

        public AttachmentType<A> buildAndRegister(ResourceLocation id) {
            Supplier<A> initial = initializer;
            var builder = net.neoforged.neoforge.attachment.AttachmentType.<A>builder(() -> initial == null ? null : initial.get());
            if (codec != null) {
                builder.serialize(codec);
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
