package net.kasax.raft.compat;

import net.minecraft.core.UUIDUtil;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

/** 1.21.1 port helper: a reference to an entity by its UUID (the newer game's EntityReference). */
public final class EntityReference<T extends Entity> {
    private final UUID uuid;

    private EntityReference(UUID uuid) {
        this.uuid = uuid;
    }

    public static <T extends Entity> EntityReference<T> of(T entity) {
        return new EntityReference<>(entity.getUUID());
    }

    public UUID getUUID() {
        return this.uuid;
    }

    public boolean matches(Entity entity) {
        return entity != null && this.uuid.equals(entity.getUUID());
    }

    public @Nullable T getEntity(Level level, Class<T> type) {
        Entity e = level.getPlayerByUUID(this.uuid);
        if (e == null && level instanceof net.minecraft.server.level.ServerLevel sl) e = sl.getEntity(this.uuid);
        return type.isInstance(e) ? type.cast(e) : null;
    }

    public static void store(@Nullable EntityReference<?> ref, ValueOutput out, String key) {
        if (ref != null) out.store(key, UUIDUtil.CODEC, ref.uuid);
    }

    public static <T extends Entity> @Nullable EntityReference<T> read(ValueInput in, String key) {
        return in.read(key, UUIDUtil.CODEC).map(u -> new EntityReference<T>(u)).orElse(null);
    }
}
