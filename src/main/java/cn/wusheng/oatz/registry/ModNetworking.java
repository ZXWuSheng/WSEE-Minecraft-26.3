package cn.wusheng.oatz.registry;

import cn.wusheng.oatz.ClientHooks;
import cn.wusheng.oatz.DoubleJumpPayload;
import cn.wusheng.oatz.FlyingHookAnchorPayload;
import cn.wusheng.oatz.FlyingHookUsePayload;
import cn.wusheng.oatz.MovementImpulsePayload;
import cn.wusheng.oatz.movement.ServerMotion;
import cn.wusheng.oatz.ShieldBashPayload;
import cn.wusheng.oatz.SwordAuraPayload;
import cn.wusheng.oatz.TrackingTargetPayload;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.function.BiConsumer;
import java.util.function.Consumer;

/** Preserves payload order and thread handoff while keeping gameplay in its existing handlers. */
public final class ModNetworking {
    private ModNetworking() {
    }

    public static void register(RegisterPayloadHandlersEvent event,
            Consumer<ServerPlayer> shieldBash,
            Consumer<ServerPlayer> swordAura,
            Consumer<ServerPlayer> doubleJump,
            BiConsumer<FlyingHookAnchorPayload, Player> flyingHookAnchor,
            BiConsumer<ServerPlayer, FlyingHookUsePayload> flyingHookUse) {
        ServerMotion.setSender((player, payload) -> PacketDistributor.sendToPlayer(player, payload));
        var registrar = event.registrar("4");
        registrar.playToServer(ShieldBashPayload.TYPE, ShieldBashPayload.STREAM_CODEC,
                (payload, context) -> context.enqueueWork(() -> {
                    if (context.player() instanceof ServerPlayer player) {
                        shieldBash.accept(player);
                    }
                }));
        registrar.playToServer(SwordAuraPayload.TYPE, SwordAuraPayload.STREAM_CODEC,
                (payload, context) -> context.enqueueWork(() -> {
                    if (context.player() instanceof ServerPlayer player) {
                        swordAura.accept(player);
                    }
                }));
        registrar.playToServer(DoubleJumpPayload.TYPE, DoubleJumpPayload.STREAM_CODEC,
                (payload, context) -> context.enqueueWork(() -> {
                    if (context.player() instanceof ServerPlayer player) {
                        doubleJump.accept(player);
                    }
                }));
        registrar.playToServer(FlyingHookUsePayload.TYPE, FlyingHookUsePayload.STREAM_CODEC,
                (payload, context) -> context.enqueueWork(() -> {
                    if (context.player() instanceof ServerPlayer player) {
                        flyingHookUse.accept(player, payload);
                    }
                }));
        registrar.playToClient(TrackingTargetPayload.TYPE, TrackingTargetPayload.STREAM_CODEC,
                (payload, context) -> context.enqueueWork(() -> ClientHooks.handleTrackingTarget(payload)));
        registrar.playToClient(MovementImpulsePayload.TYPE, MovementImpulsePayload.STREAM_CODEC,
                (payload, context) -> context.enqueueWork(() -> ClientHooks.handleMovementImpulse(payload)));
        registrar.playToClient(FlyingHookAnchorPayload.TYPE, FlyingHookAnchorPayload.STREAM_CODEC,
                (payload, context) -> context.enqueueWork(() ->
                        flyingHookAnchor.accept(payload, context.player())));
    }
}
