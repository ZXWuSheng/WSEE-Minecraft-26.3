package cn.wusheng.oatz;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import java.util.UUID;

/** An action applies only to this cast and its expected phase. */
public record FlyingHookUsePayload(UUID hookUuid, byte phase, boolean offhand) implements CustomPacketPayload {
    public static final Type<FlyingHookUsePayload> TYPE =
            new Type<>(Identifier.fromNamespaceAndPath(WseeMod.MOD_ID, "flying_hook_use"));
    public static final StreamCodec<RegistryFriendlyByteBuf, FlyingHookUsePayload> STREAM_CODEC = new StreamCodec<>() {
        @Override public FlyingHookUsePayload decode(RegistryFriendlyByteBuf buf) {
            return new FlyingHookUsePayload(buf.readUUID(), buf.readByte(), buf.readBoolean());
        }
        @Override public void encode(RegistryFriendlyByteBuf buf, FlyingHookUsePayload value) {
            buf.writeUUID(value.hookUuid); buf.writeByte(value.phase); buf.writeBoolean(value.offhand);
        }
    };
    @Override public Type<FlyingHookUsePayload> type() { return TYPE; }
}
