package cn.wusheng.oatz.movement;

import cn.wusheng.oatz.MovementImpulsePayload;
import cn.wusheng.oatz.FlyingHookPullPayload;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import java.util.Map;
import java.util.UUID;
import java.util.WeakHashMap;
import java.util.function.BiConsumer;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

/** Shared server authorization; the loader supplies only the S2C transport. */
public final class ServerMotion {
    private static final Map<ServerPlayer, Session> SESSIONS = new WeakHashMap<>();
    private static BiConsumer<ServerPlayer, CustomPacketPayload> sender;

    private static final class Session {
        final Identifier dimension;
        final UUID id = UUID.randomUUID();
        long sequence;
        Vec3 observation;
        Vec3 pending = Vec3.ZERO;

        Session(Identifier dimension) { this.dimension = dimension; }

        void observe(ServerPlayer player) {
            Vec3 known = player.getKnownMovement();
            // A newly accepted position packet includes prior applied forces.
            // 保留尚未反映到移动记录的授权增量；不能仅因经过一个 tick 就遗忘，
            // 否则网络延迟或没有位置反馈时会重复追加拉力并突破速度上限。
            if (known != observation) pending = Vec3.ZERO;
            observation = known;
        }
    }

    private ServerMotion() {}

    public static void setSender(BiConsumer<ServerPlayer, CustomPacketPayload> transport) {
        sender = transport;
    }

    private static Session session(ServerPlayer player) {
        Identifier dimension = player.level().dimension().identifier();
        Session state = SESSIONS.get(player);
        if (state == null || !state.dimension.equals(dimension)) {
            state = new Session(dimension);
            SESSIONS.put(player, state);
        }
        state.observe(player);
        return state;
    }

    /** Accepted client displacement estimates radial speed without replacing its inertia. */
    public static Vec3 basis(Player player) {
        if (!(player instanceof ServerPlayer serverPlayer)) return player.getDeltaMovement();
        Session state = session(serverPlayer);
        return state.observation.add(state.pending);
    }

    public static void applyImpulse(Player player, Vec3 delta) {
        applyImpulse(player, delta, -1.0D);
    }

    public static void applyImpulse(Player player, Vec3 delta, double radialLimit) {
        applyImpulse(player, delta, radialLimit, 0.0D);
    }

    public static void applyDoubleJump(Player player) {
        applyImpulse(player, new Vec3(0.0D, DoubleJumpController.VERTICAL_IMPULSE, 0.0D),
                -1.0D, DoubleJumpController.MAX_FALL_COMPENSATION);
    }

    private static void applyImpulse(Player player, Vec3 delta, double radialLimit, double fallCompensation) {
        if (!(player instanceof ServerPlayer serverPlayer) || sender == null || !finite(delta)
                || !Double.isFinite(radialLimit) || !DoubleJumpController.validCompensationAllowance(fallCompensation)
                || delta.equals(Vec3.ZERO)) return;
        Session state = session(serverPlayer);
        double currentY = state.observation.add(state.pending).y;
        Vec3 realized = delta.add(0.0D, DoubleJumpController.fallCompensation(currentY, fallCompensation), 0.0D);
        state.pending = state.pending.add(realized);
        player.setDeltaMovement(player.getDeltaMovement().add(realized));
        // Do not set or clear syncVelocity: the vanilla tracker uses it to send
        // an absolute velocity to self, and existing external flags stay owned
        // by their original producer.
        sender.accept(serverPlayer, new MovementImpulsePayload(player.getId(), state.dimension, state.id,
                ++state.sequence, delta, radialLimit, fallCompensation));
    }

    public static boolean finite(Vec3 value) {
        return Double.isFinite(value.x) && Double.isFinite(value.y) && Double.isFinite(value.z);
    }

    public static void applyDirectPull(Player player, UUID hookUuid, Vec3 target) {
        if (!(player instanceof ServerPlayer serverPlayer) || sender == null || hookUuid == null
                || !finite(target) || target.lengthSqr() > MovementMath.MAX_PULL_SPEED * MovementMath.MAX_PULL_SPEED + 1.0E-9D) return;
        Session state = session(serverPlayer);
        Vec3 basis = state.observation.add(state.pending);
        state.pending = state.pending.add(MovementMath.directPull(basis, target).subtract(basis));
        player.setDeltaMovement(MovementMath.directPull(player.getDeltaMovement(), target));
        // A separate steering receipt preserves live client Y; never send an
        // absolute vanilla motion packet computed from stale server velocity.
        sender.accept(serverPlayer, new FlyingHookPullPayload(player.getId(), state.dimension, state.id,
                ++state.sequence, hookUuid, target));
    }

    public static void clear(Player player) { SESSIONS.remove(player); }
    public static void clearAll() { SESSIONS.clear(); }
}
