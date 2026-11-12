package net.kasax.raft.platform;

import com.mojang.serialization.Codec;
import java.util.function.Supplier;
import net.minecraft.core.Registry;
import net.minecraft.resources.Identifier;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

/** Fabric's AttachmentRegistry on NeoForge data attachments (same ids, same codecs). */
public final class AttachmentRegistry {
    private AttachmentRegistry() {
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
            return new AttachmentType<>(id, type, codec, initializer);
        }
    }
}
