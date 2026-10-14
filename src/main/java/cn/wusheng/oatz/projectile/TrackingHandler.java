package cn.wusheng.oatz.projectile;

import cn.wusheng.oatz.TrackingTargetPayload;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** Owns one locked target per tracked projectile and its shooter-local indicator. */
public final class TrackingHandler {
    private final Map<UUID, TrackingState> trackingStates = new HashMap<>();

    private static final class TrackingState {
        private final UUID owner;
        private final int level;
        private final Vec3 initialDirection;
        private final ResourceKey<Level> dimension;
        private UUID target;
        private final int targetEntityId;
        private int ticks;
        private boolean active = true;

        private TrackingState(UUID owner, int level, Vec3 initialDirection, ResourceKey<Level> dimension,
                UUID target, int targetEntityId) {
            this.owner = owner;
            this.level = level;
            this.initialDirection = initialDirection;
            this.dimension = dimension;
            this.target = target;
            this.targetEntityId = targetEntityId;
        }
    }

    public void acquireTarget(AbstractArrow arrow, LivingEntity shooter, int enchantmentLevel,
            ServerLevel level) {
        Vec3 motion = arrow.getDeltaMovement();
        if (motion.lengthSqr() < 1.0E-6D) {
            return;
        }
        Vec3 initial = motion.normalize();
        double range = switch (enchantmentLevel) {
            case 1 -> 24.0D;
            case 2 -> 48.0D;
            case 3 -> 72.0D;
            default -> 72.0D;
        };
        double lockAngle = switch (enchantmentLevel) {
            case 1 -> 25.0D;
            case 2 -> 35.0D;
            default -> 50.0D;
        };
        double minDot = Math.cos(lockAngle * Mth.DEG_TO_RAD);
        Vec3 origin = arrow.position();
        LivingEntity selected = level.getEntitiesOfClass(LivingEntity.class,
                        shooter.getBoundingBox().inflate(range), target -> isTrackingCandidate(shooter, target))
                .stream()
                .filter(target -> {
                    Vec3 targetPoint = target.getPosition(1.0F).add(0.0D, target.getBbHeight() * 0.60D, 0.0D);
                    Vec3 toTarget = targetPoint.subtract(origin);
                    return toTarget.lengthSqr() > 1.0E-6D && toTarget.lengthSqr() <= range * range
                            && initial.dot(toTarget.normalize()) >= minDot
                            && hasTrackingLineOfSight(level, origin, targetPoint, arrow);
                })
                .min(Comparator.comparingDouble(target -> {
                    Vec3 toTarget = target.getPosition(1.0F).add(0.0D, target.getBbHeight() * 0.60D, 0.0D)
                            .subtract(origin).normalize();
                    double anglePenalty = 1.0D - initial.dot(toTarget);
                    return anglePenalty * 100.0D + target.distanceToSqr(origin);
                }))
                .orElse(null);
        if (selected != null) {
            trackingStates.put(arrow.getUUID(),
                    new TrackingState(shooter.getUUID(), enchantmentLevel, initial, level.dimension(), selected.getUUID(),
                            selected.getId()));
            sendTrackingIndicator(level, shooter, selected.getId(), true);
            level.sendParticles(ParticleTypes.ENCHANT, arrow.getX(), arrow.getY(), arrow.getZ(),
                    2 + enchantmentLevel, 0.10D, 0.10D, 0.10D, 0.02D);
            level.sendParticles(ParticleTypes.END_ROD, selected.getX(), selected.getY() + selected.getBbHeight() * 0.6D,
                    selected.getZ(), 4, 0.25D, selected.getBbHeight() * 0.35D, 0.25D, 0.01D);
        }
    }

    private void clearTrackingState(UUID arrowId, ServerLevel level) {
        TrackingState state = trackingStates.remove(arrowId);
        if (state == null) {
            return;
        }
        Entity owner = level.getEntity(state.owner);
        if (owner instanceof ServerPlayer shooter) {
            shooter.connection.send(new TrackingTargetPayload(state.targetEntityId, false));
        }
    }

    private static void sendTrackingIndicator(ServerLevel level, LivingEntity shooter, int targetEntityId,
            boolean glowing) {
        if (shooter instanceof ServerPlayer player) {
            player.connection.send(new TrackingTargetPayload(targetEntityId, glowing));
        }
    }

    private static boolean isTrackingCandidate(LivingEntity shooter, LivingEntity target) {
        if (target == shooter || !target.isAlive() || target.isSpectator() || target instanceof ArmorStand
                || !target.isPickable() || shooter.isAlliedTo(target)) {
            return false;
        }
        if (target instanceof Player playerTarget && shooter instanceof Player playerShooter
                && !playerShooter.canHarmPlayer(playerTarget)) {
            return false;
        }
        return true;
    }

    private static boolean hasTrackingLineOfSight(ServerLevel level, Vec3 from, Vec3 to, Entity ignored) {
        BlockHitResult hit = level.clip(new ClipContext(from, to, ClipContext.Block.COLLIDER,
                ClipContext.Fluid.NONE, ignored));
        return hit.getType() == HitResult.Type.MISS || hit.getLocation().distanceToSqr(to) < 0.01D;
    }

    public void tick(AbstractArrow arrow, ServerLevel level) {
        TrackingState state = trackingStates.get(arrow.getUUID());
        if (state == null) {
            return;
        }
        if (!state.active || !state.dimension.equals(level.dimension()) || arrow.isRemoved()
                || state.ticks >= 40) {
            clearTrackingState(arrow.getUUID(), level);
            return;
        }
        state.ticks++;
        Entity ownerEntity = level.getEntity(state.owner);
        Entity targetEntity = level.getEntity(state.target);
        if (!(ownerEntity instanceof LivingEntity owner) || !(targetEntity instanceof LivingEntity target)
                || !target.isAlive() || !isTrackingCandidate(owner, target)) {
            state.active = false;
            clearTrackingState(arrow.getUUID(), level);
            return;
        }
        Vec3 currentVelocity = arrow.getDeltaMovement();
        double speed = currentVelocity.length();
        if (speed < 1.0E-6D) {
            clearTrackingState(arrow.getUUID(), level);
            return;
        }
        Vec3 currentDirection = currentVelocity.scale(1.0D / speed);
        Vec3 targetPoint = target.getPosition(1.0F).add(0.0D, target.getBbHeight() * 0.60D, 0.0D);
        Vec3 desired = targetPoint.subtract(arrow.position());
        if (desired.lengthSqr() < 1.0E-6D || !hasTrackingLineOfSight(level, arrow.position(), targetPoint, arrow)) {
            clearTrackingState(arrow.getUUID(), level);
            return;
        }
        double initialLimit = Math.cos(70.0D * Mth.DEG_TO_RAD);
        if (state.initialDirection.dot(desired.normalize()) < initialLimit) {
            clearTrackingState(arrow.getUUID(), level);
            return;
        }
        double maxTurn = switch (state.level) {
            case 1 -> 3.0D;
            case 2 -> 5.0D;
            default -> 7.0D;
        } * Mth.DEG_TO_RAD;
        Vec3 desiredDirection = desired.normalize();
        double angle = Math.acos(Mth.clamp(currentDirection.dot(desiredDirection), -1.0D, 1.0D));
        double ratio = angle <= maxTurn ? 1.0D : maxTurn / angle;
        Vec3 adjusted = currentDirection.scale(1.0D - ratio).add(desiredDirection.scale(ratio)).normalize();
        arrow.setDeltaMovement(adjusted.scale(speed));
        arrow.syncVelocity = true;
        if (arrow.tickCount % 5 == 0) {
            level.sendParticles(ParticleTypes.ENCHANT, arrow.getX(), arrow.getY(), arrow.getZ(),
                    1, 0.03D, 0.03D, 0.03D, 0.0D);
        }
    }
}
