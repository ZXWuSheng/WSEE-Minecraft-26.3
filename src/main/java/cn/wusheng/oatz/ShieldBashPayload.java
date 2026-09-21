package cn.wusheng.oatz;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record ShieldBashPayload() implements CustomPacketPayload {
    public static final Type<ShieldBashPayload> TYPE = new Type<>(Identifier.fromNamespaceAndPath(WseeMod.MOD_ID, "shield_bash"));
    public static final StreamCodec<RegistryFriendlyByteBuf, ShieldBashPayload> STREAM_CODEC =
            StreamCodec.unit(new ShieldBashPayload());

    @Override
    public Type<ShieldBashPayload> type() {
        return TYPE;
    }
}
