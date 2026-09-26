package cn.wusheng.oatz;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/** Server-confirmed vertical velocity for the local double jump. */
public record DoubleJumpVelocityPayload(double xVelocity, double yVelocity, double zVelocity,
        boolean sprinting) implements CustomPacketPayload {
    public static final Type<DoubleJumpVelocityPayload> TYPE =
            new Type<>(Identifier.fromNamespaceAndPath(WseeMod.MOD_ID, "double_jump_velocity"));
    public static final StreamCodec<RegistryFriendlyByteBuf, DoubleJumpVelocityPayload> STREAM_CODEC =
            StreamCodec.composite(ByteBufCodecs.DOUBLE, DoubleJumpVelocityPayload::xVelocity,
                    ByteBufCodecs.DOUBLE, DoubleJumpVelocityPayload::yVelocity,
                    ByteBufCodecs.DOUBLE, DoubleJumpVelocityPayload::zVelocity,
                    ByteBufCodecs.BOOL, DoubleJumpVelocityPayload::sprinting,
                    DoubleJumpVelocityPayload::new);

    @Override
    public Type<DoubleJumpVelocityPayload> type() {
        return TYPE;
    }
}
