package net.kasax.raft.compat;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import org.jetbrains.annotations.Nullable;

/** 1.21.1 port helper: a writer over a {@link CompoundTag} with the method names of the newer game's value output. */
public final class ValueOutput {
    private final CompoundTag tag;
    private final HolderLookup.Provider lookup;

    private ValueOutput(CompoundTag tag, HolderLookup.Provider lookup) {
        this.tag = tag;
        this.lookup = lookup;
    }

    public static ValueOutput of(CompoundTag tag, HolderLookup.Provider lookup) {
        return new ValueOutput(tag, lookup);
    }

    public CompoundTag tag() {
        return this.tag;
    }

    public HolderLookup.Provider lookup() {
        return this.lookup;
    }

    public <T> void store(String name, Codec<T> codec, T value) {
        codec.encodeStart(this.lookup.createSerializationContext(NbtOps.INSTANCE), value).result().ifPresent(t -> this.tag.put(name, t));
    }

    public <T> void storeNullable(String name, Codec<T> codec, @Nullable T value) {
        if (value != null) store(name, codec, value);
    }

    public <T> void store(MapCodec<T> codec, T value) {
        codec.codec().encodeStart(this.lookup.createSerializationContext(NbtOps.INSTANCE), value).result()
                .ifPresent(t -> { if (t instanceof CompoundTag c) this.tag.merge(c); });
    }

    public void putBoolean(String name, boolean value) { this.tag.putBoolean(name, value); }
    public void putByte(String name, byte value) { this.tag.putByte(name, value); }
    public void putShort(String name, short value) { this.tag.putShort(name, value); }
    public void putInt(String name, int value) { this.tag.putInt(name, value); }
    public void putLong(String name, long value) { this.tag.putLong(name, value); }
    public void putFloat(String name, float value) { this.tag.putFloat(name, value); }
    public void putDouble(String name, double value) { this.tag.putDouble(name, value); }
    public void putString(String name, String value) { this.tag.putString(name, value); }
    public void putIntArray(String name, int[] value) { this.tag.putIntArray(name, value); }

    public ValueOutput child(String name) {
        CompoundTag c = new CompoundTag();
        this.tag.put(name, c);
        return new ValueOutput(c, this.lookup);
    }

    public ValueOutputList childrenList(String name) {
        ListTag list = new ListTag();
        this.tag.put(name, list);
        return new ValueOutputList(list, this.lookup);
    }

    public <T> TypedOutputList<T> list(String name, Codec<T> codec) {
        ListTag list = new ListTag();
        this.tag.put(name, list);
        return new TypedOutputList<>(list, codec, this.lookup);
    }

    public void discard(String name) {
        this.tag.remove(name);
    }

    public boolean isEmpty() {
        return this.tag.isEmpty();
    }

    public static final class ValueOutputList {
        private final ListTag list;
        private final HolderLookup.Provider lookup;

        ValueOutputList(ListTag list, HolderLookup.Provider lookup) {
            this.list = list;
            this.lookup = lookup;
        }

        public ValueOutput addChild() {
            CompoundTag c = new CompoundTag();
            this.list.add(c);
            return new ValueOutput(c, this.lookup);
        }

        public void discardLast() {
            if (!this.list.isEmpty()) this.list.remove(this.list.size() - 1);
        }

        public boolean isEmpty() {
            return this.list.isEmpty();
        }
    }

    public static final class TypedOutputList<T> {
        private final ListTag list;
        private final Codec<T> codec;
        private final HolderLookup.Provider lookup;

        TypedOutputList(ListTag list, Codec<T> codec, HolderLookup.Provider lookup) {
            this.list = list;
            this.codec = codec;
            this.lookup = lookup;
        }

        public void add(T value) {
            this.codec.encodeStart(this.lookup.createSerializationContext(NbtOps.INSTANCE), value).result().ifPresent(this.list::add);
        }

        public boolean isEmpty() {
            return this.list.isEmpty();
        }
    }
}
