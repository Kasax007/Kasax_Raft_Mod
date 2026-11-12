package net.kasax.raft.platform;

import com.mojang.serialization.Codec;
import java.util.function.Supplier;
import net.minecraft.resources.Identifier;
import net.neoforged.neoforge.attachment.IAttachmentHolder;
import org.jetbrains.annotations.Nullable;

/**
 * A Fabric-style attachment (absent until set) on a NeoForge attachment type. Fabric's injected
 * {@code holder.getAttached(type)} calls are written {@code type.get(holder)} on NeoForge.
 */
public final class AttachmentType<A> {
    private final Identifier id;
    private final net.neoforged.neoforge.attachment.AttachmentType<A> neo;
    private final Codec<A> codec;
    private final Supplier<A> initializer;

    AttachmentType(Identifier id, net.neoforged.neoforge.attachment.AttachmentType<A> neo, Codec<A> codec, Supplier<A> initializer) {
        this.id = id;
        this.neo = neo;
        this.codec = codec;
        this.initializer = initializer;
    }

    public Identifier identifier() {
        return id;
    }

    public net.neoforged.neoforge.attachment.AttachmentType<A> neoType() {
        return neo;
    }

    @Nullable
    public Codec<A> persistenceCodec() {
        return codec;
    }

    @Nullable
    public A get(IAttachmentHolder holder) {
        return holder.hasData(neo) ? holder.getData(neo) : null;
    }

    public A getOrElse(IAttachmentHolder holder, A defaultValue) {
        A value = get(holder);
        return value != null ? value : defaultValue;
    }

    public A getOrCreate(IAttachmentHolder holder) {
        A value = get(holder);
        if (value == null) {
            value = initializer.get();
            set(holder, value);
        }
        return value;
    }

    public boolean has(IAttachmentHolder holder) {
        return holder.hasData(neo);
    }

    @Nullable
    public A set(IAttachmentHolder holder, @Nullable A value) {
        A old = get(holder);
        if (value == null) {
            holder.removeData(neo);
        } else {
            holder.setData(neo, value);
        }
        return old;
    }

    @Nullable
    public A remove(IAttachmentHolder holder) {
        return set(holder, null);
    }
}
