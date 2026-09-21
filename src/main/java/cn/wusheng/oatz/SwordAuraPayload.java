package cn.wusheng.oatz;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/** Requests a sword-aura release for an attack that did not hit an entity. */
public record SwordAuraPayload() implements CustomPacketPayload {
    public static final Type<SwordAuraPayload> TYPE =
            new Type<>(Identifier.fromNamespaceAndPath(WseeMod.MOD_ID, "sword_aura"));
    public static final StreamCodec<RegistryFriendlyByteBuf, SwordAuraPayload> STREAM_CODEC =
            StreamCodec.unit(new SwordAuraPayload());

    @Override
    public Type<SwordAuraPayload> type() {
        return TYPE;
    }
}
