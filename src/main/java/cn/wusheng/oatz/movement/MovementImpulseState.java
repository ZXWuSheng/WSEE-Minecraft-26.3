package cn.wusheng.oatz.movement;

import cn.wusheng.oatz.MovementImpulsePayload;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

/** Client receipt of server-authorized motion; no eligibility or prediction here. */
public final class MovementImpulseState {
    private Object playerIdentity, levelIdentity;
    private Identifier dimension;
    private UUID session;
    private long sequence;
    private final Set<UUID> retired = new HashSet<>();

    public boolean apply(Player player, MovementImpulsePayload payload) {
        if (player == null || !player.isAlive() || player.isRemoved() || player.getId() != payload.playerId()
                || !player.level().dimension().identifier().equals(payload.dimension())
                || !ServerMotion.finite(payload.delta()) || !Double.isFinite(payload.radialLimit())
                || payload.sequence() <= 0) return false;
        if (!accept(player, player.level(), payload.dimension(), payload.sessionUuid(), payload.sequence())) return false;
        Vec3 current = player.getDeltaMovement();
        player.setDeltaMovement(current.add(approvedDelta(current, payload)));
        return true;
    }

    /** Only reduce this authorized increment; existing radial/tangent motion stays intact. */
    public static Vec3 approvedDelta(Vec3 current, MovementImpulsePayload payload) {
        Vec3 delta = payload.delta();
        if (!ServerMotion.finite(current) || !ServerMotion.finite(delta) || !Double.isFinite(payload.radialLimit()))
            return Vec3.ZERO;
        if (payload.radialLimit() < 0.0D) return delta;
        double length = delta.length();
        if (length == 0.0D || !Double.isFinite(length)) return Vec3.ZERO;
        Vec3 axis = delta.scale(1.0D / length);
        double allowed = Math.max(0.0D, payload.radialLimit() - current.dot(axis));
        return delta.scale(Math.min(1.0D, allowed / length));
    }

    public boolean accept(Object player, Object level, Identifier packetDimension, UUID packetSession, long packetSequence) {
        if (player == null || level == null || packetDimension == null || packetSession == null || packetSequence <= 0)
            return false;
        if (playerIdentity != player || levelIdentity != level || !packetDimension.equals(dimension)) {
            clear();
            playerIdentity = player;
            levelIdentity = level;
            dimension = packetDimension;
        }
        if (!packetSession.equals(session)) {
            if (retired.contains(packetSession)) return false;
            if (session != null) retired.add(session);
            session = packetSession;
            sequence = 0;
        }
        if (packetSequence <= sequence) return false;
        sequence = packetSequence;
        return true;
    }

    public void clear() {
        playerIdentity = null;
        levelIdentity = null;
        dimension = null;
        session = null;
        sequence = 0;
        retired.clear();
    }
}
