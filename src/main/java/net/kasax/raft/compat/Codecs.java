package net.kasax.raft.compat;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.phys.Vec3;

/** 1.21.1 port helper: network codecs the newer game has. */
public final class Codecs {
    private Codecs() {
    }

    public static final StreamCodec<ByteBuf, Vec3> VEC3 = StreamCodec.of(
            (buf, v) -> {
                buf.writeDouble(v.x);
                buf.writeDouble(v.y);
                buf.writeDouble(v.z);
            },
            buf -> new Vec3(buf.readDouble(), buf.readDouble(), buf.readDouble()));
}
