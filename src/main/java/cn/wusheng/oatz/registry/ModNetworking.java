package cn.wusheng.oatz.registry;

import cn.wusheng.oatz.ClientHooks;
import cn.wusheng.oatz.DoubleJumpPayload;
import cn.wusheng.oatz.DoubleJumpVelocityPayload;
import cn.wusheng.oatz.FlyingHookAnchorPayload;
import cn.wusheng.oatz.ShieldBashPayload;
import cn.wusheng.oatz.SwordAuraPayload;
import cn.wusheng.oatz.TrackingTargetPayload;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

import java.util.function.BiConsumer;
import java.util.function.Consumer;

/** Preserves payload order and thread handoff while keeping gameplay in its existing handlers. */
public final class ModNetworking {
    private ModNetworking() {
    }

    @FunctionalInterface
    public interface DoubleJumpHandler {
        void handle(ServerPlayer player, boolean sprinting, double xVelocity, double zVelocity);
    }

    public static void register(RegisterPayloadHandlersEvent event,
            Consumer<ServerPlayer> shieldBash,
            Consumer<ServerPlayer> swordAura,
            DoubleJumpHandler doubleJump,
            BiConsumer<FlyingHookAnchorPayload, Player> flyingHookAnchor) {
        var registrar = event.registrar("1");
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
                        doubleJump.handle(player, payload.sprinting(), payload.xVelocity(), payload.zVelocity());
                    }
                }));
        registrar.playToClient(TrackingTargetPayload.TYPE, TrackingTargetPayload.STREAM_CODEC,
                (payload, context) -> context.enqueueWork(() -> ClientHooks.handleTrackingTarget(payload)));
        registrar.playToClient(FlyingHookAnchorPayload.TYPE, FlyingHookAnchorPayload.STREAM_CODEC,
                (payload, context) -> context.enqueueWork(() ->
                        flyingHookAnchor.accept(payload, context.player())));
        registrar.playToClient(DoubleJumpVelocityPayload.TYPE, DoubleJumpVelocityPayload.STREAM_CODEC,
                (payload, context) -> context.enqueueWork(() -> ClientHooks.handleDoubleJumpVelocity(payload)));
    }
}
