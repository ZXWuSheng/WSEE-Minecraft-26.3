package cn.wusheng.oatz;

import java.util.UUID;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.world.phys.Vec3;

/** One server-authorized steering step, bound to the active cast. */
public record FlyingHookPullPayload(int playerId, Identifier dimension, UUID sessionUuid,
        long sequence, UUID hookUuid, Vec3 targetVelocity) implements CustomPacketPayload {
    public static final Type<FlyingHookPullPayload> TYPE =
            new Type<>(Identifier.fromNamespaceAndPath(WseeMod.MOD_ID, "flying_hook_pull"));
    public static final StreamCodec<RegistryFriendlyByteBuf, FlyingHookPullPayload> STREAM_CODEC = new StreamCodec<>() {
        @Override public FlyingHookPullPayload decode(RegistryFriendlyByteBuf buf) {
            return new FlyingHookPullPayload(buf.readVarInt(), buf.readIdentifier(), buf.readUUID(),
                    buf.readVarLong(), buf.readUUID(), new Vec3(buf.readDouble(), buf.readDouble(), buf.readDouble()));
        }

        @Override public void encode(RegistryFriendlyByteBuf buf, FlyingHookPullPayload value) {
            buf.writeVarInt(value.playerId); buf.writeIdentifier(value.dimension); buf.writeUUID(value.sessionUuid);
            buf.writeVarLong(value.sequence); buf.writeUUID(value.hookUuid);
            buf.writeDouble(value.targetVelocity.x); buf.writeDouble(value.targetVelocity.y); buf.writeDouble(value.targetVelocity.z);
        }
    };

    @Override public Type<FlyingHookPullPayload> type() { return TYPE; }
}
