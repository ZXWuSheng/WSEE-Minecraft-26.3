package cn.wusheng.oatz;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import java.util.UUID;

/** Server-authoritative Flying Hook anchor position for client rendering. */
public record FlyingHookAnchorPayload(int hookEntityId, UUID hookUuid, byte phase, int targetEntityId,
        double x, double y, double z, double offsetX, double offsetY, double offsetZ)
        implements CustomPacketPayload {
    public static final Type<FlyingHookAnchorPayload> TYPE =
            new Type<>(Identifier.fromNamespaceAndPath(WseeMod.MOD_ID, "flying_hook_anchor"));
    public static final StreamCodec<RegistryFriendlyByteBuf, FlyingHookAnchorPayload> STREAM_CODEC = new StreamCodec<>() {
        @Override public FlyingHookAnchorPayload decode(RegistryFriendlyByteBuf buf) {
            return new FlyingHookAnchorPayload(buf.readVarInt(), buf.readUUID(), buf.readByte(), buf.readVarInt(),
                    buf.readDouble(), buf.readDouble(), buf.readDouble(), buf.readDouble(), buf.readDouble(), buf.readDouble());
        }
        @Override public void encode(RegistryFriendlyByteBuf buf, FlyingHookAnchorPayload value) {
            buf.writeVarInt(value.hookEntityId); buf.writeUUID(value.hookUuid);
            buf.writeByte(value.phase); buf.writeVarInt(value.targetEntityId);
            buf.writeDouble(value.x); buf.writeDouble(value.y); buf.writeDouble(value.z);
            buf.writeDouble(value.offsetX); buf.writeDouble(value.offsetY); buf.writeDouble(value.offsetZ);
        }
    };

    @Override
    public Type<FlyingHookAnchorPayload> type() {
        return TYPE;
    }
}
