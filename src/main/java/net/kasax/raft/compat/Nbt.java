package net.kasax.raft.compat;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;

import java.util.Optional;

/** 1.21.1 port helper: the newer game's {@link CompoundTag} getters (with defaults or optionals). */
public final class Nbt {
    private Nbt() {
    }

    public static int getIntOr(CompoundTag t, String k, int d) { return t.contains(k, Tag.TAG_ANY_NUMERIC) ? t.getInt(k) : d; }
    public static long getLongOr(CompoundTag t, String k, long d) { return t.contains(k, Tag.TAG_ANY_NUMERIC) ? t.getLong(k) : d; }
    public static float getFloatOr(CompoundTag t, String k, float d) { return t.contains(k, Tag.TAG_ANY_NUMERIC) ? t.getFloat(k) : d; }
    public static double getDoubleOr(CompoundTag t, String k, double d) { return t.contains(k, Tag.TAG_ANY_NUMERIC) ? t.getDouble(k) : d; }
    public static byte getByteOr(CompoundTag t, String k, byte d) { return t.contains(k, Tag.TAG_ANY_NUMERIC) ? t.getByte(k) : d; }
    public static short getShortOr(CompoundTag t, String k, short d) { return t.contains(k, Tag.TAG_ANY_NUMERIC) ? t.getShort(k) : d; }
    public static boolean getBooleanOr(CompoundTag t, String k, boolean d) { return t.contains(k, Tag.TAG_ANY_NUMERIC) ? t.getBoolean(k) : d; }
    public static String getStringOr(CompoundTag t, String k, String d) { return t.contains(k, Tag.TAG_STRING) ? t.getString(k) : d; }
    public static CompoundTag getCompoundOrEmpty(CompoundTag t, String k) { return t.getCompound(k); }
    public static ListTag getListOrEmpty(CompoundTag t, String k) { Tag x = t.get(k); return x instanceof ListTag l ? l : new ListTag(); }

    public static Optional<Integer> getInt(CompoundTag t, String k) { return t.contains(k, Tag.TAG_ANY_NUMERIC) ? Optional.of(t.getInt(k)) : Optional.empty(); }
    public static Optional<Long> getLong(CompoundTag t, String k) { return t.contains(k, Tag.TAG_ANY_NUMERIC) ? Optional.of(t.getLong(k)) : Optional.empty(); }
    public static Optional<Boolean> getBoolean(CompoundTag t, String k) { return t.contains(k, Tag.TAG_ANY_NUMERIC) ? Optional.of(t.getBoolean(k)) : Optional.empty(); }
    public static Optional<String> getString(CompoundTag t, String k) { return t.contains(k, Tag.TAG_STRING) ? Optional.of(t.getString(k)) : Optional.empty(); }
    public static Optional<CompoundTag> getCompound(CompoundTag t, String k) { return t.contains(k, Tag.TAG_COMPOUND) ? Optional.of(t.getCompound(k)) : Optional.empty(); }
    public static Optional<ListTag> getList(CompoundTag t, String k) { return t.get(k) instanceof ListTag l ? Optional.of(l) : Optional.empty(); }
    public static Optional<int[]> getIntArray(CompoundTag t, String k) { return t.contains(k, Tag.TAG_INT_ARRAY) ? Optional.of(t.getIntArray(k)) : Optional.empty(); }
    public static Optional<long[]> getLongArray(CompoundTag t, String k) { return t.contains(k, Tag.TAG_LONG_ARRAY) ? Optional.of(t.getLongArray(k)) : Optional.empty(); }
}
