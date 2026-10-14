package cn.wusheng.oatz.movement;

import cn.wusheng.oatz.FlyingHookAnchorPayload;
import cn.wusheng.oatz.access.FlyingHookAccess;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.FishingHook;
import net.minecraft.world.phys.Vec3;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** Presentation only: clients never choose anchors, phases, eligibility or player forces. */
public final class FlyingHookClientState {
    private record Anchor(Vec3 position, int targetId, Vec3 offset) {}
    private final Map<UUID, Anchor> anchors = new HashMap<>();

    public void update(FlyingHookAnchorPayload payload, Player receiver) {
        if (receiver == null) return;
        Entity entity = receiver.level().getEntity(payload.hookEntityId());
        if (!(entity instanceof FishingHook hook) || !hook.getUUID().equals(payload.hookUuid())
                || payload.phase() < FlyingHookController.ANCHORED || payload.phase() > FlyingHookController.REELING
                || !Double.isFinite(payload.x()) || !Double.isFinite(payload.y()) || !Double.isFinite(payload.z())
                || !Double.isFinite(payload.offsetX()) || !Double.isFinite(payload.offsetY()) || !Double.isFinite(payload.offsetZ())) return;
        Vec3 anchor = new Vec3(payload.x(), payload.y(), payload.z());
        anchors.put(hook.getUUID(), new Anchor(anchor, payload.targetEntityId(),
                new Vec3(payload.offsetX(), payload.offsetY(), payload.offsetZ())));
        ((FlyingHookAccess) hook).wsee$setPhase(payload.phase());
        holdAtAnchor(hook, anchor);
    }

    public boolean tickHook(FishingHook hook) {
        byte phase = ((FlyingHookAccess) hook).wsee$getPhase();
        if (phase == FlyingHookController.INACTIVE) { remove(hook); return false; }
        hook.baseTick();
        if (phase == FlyingHookController.FLYING) {
            Vec3 current = MovementMath.flyingVelocity(hook.getDeltaMovement(), hook.getGravity());
            hook.setDeltaMovement(current);
            hook.move(MoverType.SELF, current);
            hook.setDeltaMovement(current.scale(0.92D));
        } else {
            Anchor state = anchors.get(hook.getUUID());
            if (state != null) {
                Entity target = state.targetId == 0 ? null : hook.level().getEntity(state.targetId);
                Vec3 anchor = target == null ? state.position : target.position().add(state.offset);
                holdAtAnchor(hook, anchor);
            }
            hook.setDeltaMovement(Vec3.ZERO);
        }
        return true;
    }

    private static void holdAtAnchor(FishingHook hook, Vec3 anchor) {
        hook.getInterpolation().cancel();
        hook.setPos(anchor);
        hook.setDeltaMovement(Vec3.ZERO);
        hook.setOldPosAndRot();
    }

    public void remove(FishingHook hook) { anchors.remove(hook.getUUID()); }
    public void clearAll() { anchors.clear(); }
}
