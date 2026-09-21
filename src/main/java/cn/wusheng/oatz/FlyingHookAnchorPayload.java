package cn.wusheng.oatz;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/** Server-authoritative Flying Hook anchor position for client rendering. */
public record FlyingHookAnchorPayload(int hookEntityId, double x, double y, double z)
        implements CustomPacketPayload {
    public static final Type<FlyingHookAnchorPayload> TYPE =
            new Type<>(Identifier.fromNamespaceAndPath(WseeMod.MOD_ID, "flying_hook_anchor"));
    public static final StreamCodec<RegistryFriendlyByteBuf, FlyingHookAnchorPayload> STREAM_CODEC =
            StreamCodec.composite(ByteBufCodecs.VAR_INT, FlyingHookAnchorPayload::hookEntityId,
                    ByteBufCodecs.DOUBLE, FlyingHookAnchorPayload::x,
                    ByteBufCodecs.DOUBLE, FlyingHookAnchorPayload::y,
                    ByteBufCodecs.DOUBLE, FlyingHookAnchorPayload::z,
                    FlyingHookAnchorPayload::new);

    @Override
    public Type<FlyingHookAnchorPayload> type() {
        return TYPE;
    }
}
