package cn.wusheng.oatz;

import java.util.UUID;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.world.phys.Vec3;

/** A server-approved change in motion, never a replacement of client inertia. */
public record MovementImpulsePayload(int playerId, Identifier dimension, UUID sessionUuid,
        long sequence, Vec3 delta, double radialLimit) implements CustomPacketPayload {
    public MovementImpulsePayload(int playerId, Identifier dimension, UUID sessionUuid, long sequence, Vec3 delta) {
        this(playerId, dimension, sessionUuid, sequence, delta, -1.0D);
    }
    public static final Type<MovementImpulsePayload> TYPE =
            new Type<>(Identifier.fromNamespaceAndPath(WseeMod.MOD_ID, "movement_impulse"));
    public static final StreamCodec<RegistryFriendlyByteBuf, MovementImpulsePayload> STREAM_CODEC = new StreamCodec<>() {
        @Override public MovementImpulsePayload decode(RegistryFriendlyByteBuf buf) {
            return new MovementImpulsePayload(buf.readVarInt(), buf.readIdentifier(), buf.readUUID(),
                    buf.readVarLong(), new Vec3(buf.readDouble(), buf.readDouble(), buf.readDouble()), buf.readDouble());
        }

        @Override public void encode(RegistryFriendlyByteBuf buf, MovementImpulsePayload value) {
            buf.writeVarInt(value.playerId); buf.writeIdentifier(value.dimension); buf.writeUUID(value.sessionUuid);
            buf.writeVarLong(value.sequence);
            buf.writeDouble(value.delta.x); buf.writeDouble(value.delta.y); buf.writeDouble(value.delta.z);
            buf.writeDouble(value.radialLimit);
        }
    };

    @Override public Type<MovementImpulsePayload> type() { return TYPE; }
}
