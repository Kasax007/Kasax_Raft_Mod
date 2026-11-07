package net.kasax.raft.compat;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * 1.21.1 port helper: a small reader over a {@link CompoundTag} with the method names of the newer game's value input,
 * so the save code of block entities and entities stays the same in both versions.
 */
public final class ValueInput {
    private final CompoundTag tag;
    private final HolderLookup.Provider lookup;

    private ValueInput(CompoundTag tag, HolderLookup.Provider lookup) {
        this.tag = tag;
        this.lookup = lookup;
    }

    public static ValueInput of(CompoundTag tag, HolderLookup.Provider lookup) {
        return new ValueInput(tag, lookup);
    }

    public CompoundTag tag() {
        return this.tag;
    }

    public HolderLookup.Provider lookup() {
        return this.lookup;
    }

    public <T> Optional<T> read(String name, Codec<T> codec) {
        Tag t = this.tag.get(name);
        if (t == null) return Optional.empty();
        return codec.parse(this.lookup.createSerializationContext(NbtOps.INSTANCE), t).result();
    }

    public <T> Optional<T> read(MapCodec<T> codec) {
        return codec.codec().parse(this.lookup.createSerializationContext(NbtOps.INSTANCE), this.tag).result();
    }

    public Optional<ValueInput> child(String name) {
        return this.tag.contains(name, Tag.TAG_COMPOUND) ? Optional.of(new ValueInput(this.tag.getCompound(name), this.lookup)) : Optional.empty();
    }

    public ValueInput childOrEmpty(String name) {
        return new ValueInput(this.tag.getCompound(name), this.lookup);
    }

    public Optional<ValueInputList> childrenList(String name) {
        return this.tag.contains(name, Tag.TAG_LIST) ? Optional.of(childrenListOrEmpty(name)) : Optional.empty();
    }

    public ValueInputList childrenListOrEmpty(String name) {
        ListTag list = this.tag.getList(name, Tag.TAG_COMPOUND);
        List<ValueInput> out = new ArrayList<>();
        for (int i = 0; i < list.size(); i++) out.add(new ValueInput(list.getCompound(i), this.lookup));
        return new ValueInputList(out);
    }

    public <T> TypedInputList<T> listOrEmpty(String name, Codec<T> codec) {
        return list(name, codec).orElse(new TypedInputList<>(List.of()));
    }

    public <T> Optional<TypedInputList<T>> list(String name, Codec<T> codec) {
        Tag t = this.tag.get(name);
        if (!(t instanceof ListTag list)) return Optional.empty();
        List<T> out = new ArrayList<>();
        var ops = this.lookup.createSerializationContext(NbtOps.INSTANCE);
        for (Tag e : list) codec.parse(ops, e).result().ifPresent(out::add);
        return Optional.of(new TypedInputList<>(out));
    }

    public boolean getBooleanOr(String name, boolean def) {
        return this.tag.contains(name, Tag.TAG_ANY_NUMERIC) ? this.tag.getBoolean(name) : def;
    }

    public byte getByteOr(String name, byte def) {
        return this.tag.contains(name, Tag.TAG_ANY_NUMERIC) ? this.tag.getByte(name) : def;
    }

    public int getShortOr(String name, short def) {
        return this.tag.contains(name, Tag.TAG_ANY_NUMERIC) ? this.tag.getShort(name) : def;
    }

    public Optional<Integer> getInt(String name) {
        return this.tag.contains(name, Tag.TAG_ANY_NUMERIC) ? Optional.of(this.tag.getInt(name)) : Optional.empty();
    }

    public int getIntOr(String name, int def) {
        return this.tag.contains(name, Tag.TAG_ANY_NUMERIC) ? this.tag.getInt(name) : def;
    }

    public long getLongOr(String name, long def) {
        return this.tag.contains(name, Tag.TAG_ANY_NUMERIC) ? this.tag.getLong(name) : def;
    }

    public Optional<Long> getLong(String name) {
        return this.tag.contains(name, Tag.TAG_ANY_NUMERIC) ? Optional.of(this.tag.getLong(name)) : Optional.empty();
    }

    public float getFloatOr(String name, float def) {
        return this.tag.contains(name, Tag.TAG_ANY_NUMERIC) ? this.tag.getFloat(name) : def;
    }

    public double getDoubleOr(String name, double def) {
        return this.tag.contains(name, Tag.TAG_ANY_NUMERIC) ? this.tag.getDouble(name) : def;
    }

    public Optional<String> getString(String name) {
        return this.tag.contains(name, Tag.TAG_STRING) ? Optional.of(this.tag.getString(name)) : Optional.empty();
    }

    public String getStringOr(String name, String def) {
        return this.tag.contains(name, Tag.TAG_STRING) ? this.tag.getString(name) : def;
    }

    public Optional<int[]> getIntArray(String name) {
        return this.tag.contains(name, Tag.TAG_INT_ARRAY) ? Optional.of(this.tag.getIntArray(name)) : Optional.empty();
    }

    public record ValueInputList(List<ValueInput> values) implements Iterable<ValueInput> {
        public boolean isEmpty() {
            return this.values.isEmpty();
        }

        public Stream<ValueInput> stream() {
            return this.values.stream();
        }

        @Override
        public java.util.Iterator<ValueInput> iterator() {
            return this.values.iterator();
        }
    }

    public record TypedInputList<T>(List<T> values) implements Iterable<T> {
        public boolean isEmpty() {
            return this.values.isEmpty();
        }

        public Stream<T> stream() {
            return this.values.stream();
        }

        @Override
        public java.util.Iterator<T> iterator() {
            return this.values.iterator();
        }
    }
}
