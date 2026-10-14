package cn.wusheng.oatz;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/** Request only: the server reads current motion and equipment itself. */
public record DoubleJumpPayload() implements CustomPacketPayload {
    public static final Type<DoubleJumpPayload> TYPE =
            new Type<>(Identifier.fromNamespaceAndPath(WseeMod.MOD_ID, "double_jump"));
    public static final StreamCodec<RegistryFriendlyByteBuf, DoubleJumpPayload> STREAM_CODEC =
            StreamCodec.unit(new DoubleJumpPayload());

    @Override
    public Type<DoubleJumpPayload> type() {
        return TYPE;
    }
}
