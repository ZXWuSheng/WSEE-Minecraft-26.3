package cn.wusheng.oatz;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record DoubleJumpPayload(double xVelocity, double zVelocity, boolean sprinting) implements CustomPacketPayload {
    public static final Type<DoubleJumpPayload> TYPE =
            new Type<>(Identifier.fromNamespaceAndPath(WseeMod.MOD_ID, "double_jump"));
    public static final StreamCodec<RegistryFriendlyByteBuf, DoubleJumpPayload> STREAM_CODEC =
            StreamCodec.composite(ByteBufCodecs.DOUBLE, DoubleJumpPayload::xVelocity,
                    ByteBufCodecs.DOUBLE, DoubleJumpPayload::zVelocity,
                    ByteBufCodecs.BOOL, DoubleJumpPayload::sprinting,
                    DoubleJumpPayload::new);

    @Override
    public Type<DoubleJumpPayload> type() {
        return TYPE;
    }
}
