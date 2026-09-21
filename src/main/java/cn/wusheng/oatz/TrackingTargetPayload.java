package cn.wusheng.oatz;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/** Server-to-client lock indicator for the shooter only. */
public record TrackingTargetPayload(int entityId, boolean glowing) implements CustomPacketPayload {
    public static final Type<TrackingTargetPayload> TYPE =
            new Type<>(Identifier.fromNamespaceAndPath(WseeMod.MOD_ID, "tracking_target"));
    public static final StreamCodec<RegistryFriendlyByteBuf, TrackingTargetPayload> STREAM_CODEC =
            StreamCodec.composite(ByteBufCodecs.VAR_INT, TrackingTargetPayload::entityId,
                    ByteBufCodecs.BOOL, TrackingTargetPayload::glowing, TrackingTargetPayload::new);

    @Override
    public Type<TrackingTargetPayload> type() {
        return TYPE;
    }
}
