package cn.wusheng.oatz.projectile;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.entity.ProjectileImpactEvent;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static cn.wusheng.oatz.registry.ModDamageTypes.EXPLOSIVE_ARROW_DAMAGE_TYPE;

/** Owns the delayed explosion state after an arrow impacts or is discarded. */
public final class ExplosiveArrowHandler {
    private static final String EXPLOSIVE_ARROW_BOSS_TAG = "wsee:explosive_arrow_boss";
    private final Map<UUID, ExplosiveArrowState> explosiveArrows = new HashMap<>();

    private static final class ExplosiveArrowState {
        private final UUID owner;
        private final int level;
        private final double attackDamage;
        private long detonationTick;
        private Vec3 lastPosition;
        private UUID attachedEntity;
        private boolean armed;
        private boolean detonated;

        private ExplosiveArrowState(UUID owner, int level, double attackDamage) {
            this.owner = owner;
            this.level = level;
            this.attackDamage = attackDamage;
        }
    }

    public void recordShot(AbstractArrow arrow, LivingEntity shooter, int level, double attackDamage) {
        explosiveArrows.put(arrow.getUUID(), new ExplosiveArrowState(shooter.getUUID(), level, attackDamage));
    }

    public boolean isRegistered(AbstractArrow arrow) {
        return explosiveArrows.containsKey(arrow.getUUID());
    }

    public void arm(AbstractArrow arrow, ProjectileImpactEvent event, ServerLevel level) {
        ExplosiveArrowState state = explosiveArrows.get(arrow.getUUID());
        if (state == null || state.armed) {
            return;
        }
        Vec3 impact = event.getRayTraceResult().getLocation();
        state.lastPosition = impact;
        if (event.getRayTraceResult() instanceof EntityHitResult entityHit
                && entityHit.getEntity() instanceof LivingEntity target) {
            state.attachedEntity = target.getUUID();
            state.lastPosition = target.position().add(0.0D, target.getBbHeight() * 0.5D, 0.0D);
        }
        state.armed = true;
        state.detonationTick = level.getGameTime() + 20L;
        level.sendParticles(ParticleTypes.FLAME, state.lastPosition.x, state.lastPosition.y,
                state.lastPosition.z, 2, 0.10D, 0.10D, 0.10D, 0.01D);
        level.playSound(null, arrow.blockPosition(), SoundEvents.FLINTANDSTEEL_USE,
                SoundSource.PLAYERS, 0.35F, 1.4F);
    }

    public void tickArrow(AbstractArrow arrow, ServerLevel level) {
        ExplosiveArrowState state = explosiveArrows.get(arrow.getUUID());
        if (state == null || !state.armed || state.detonated || arrow.isRemoved()) {
            return;
        }

        tickExplosiveState(state, level, arrow);
        if (state.detonated) {
            explosiveArrows.remove(arrow.getUUID());
        }
    }

    /** Entity-hit arrows may be discarded by vanilla immediately after impact. */
    public void tickDetachedStates(ServerLevel level) {
        long now = level.getGameTime();
        for (var entry : new ArrayList<>(explosiveArrows.entrySet())) {
            UUID arrowId = entry.getKey();
            ExplosiveArrowState state = entry.getValue();
            if (state.detonated) {
                explosiveArrows.remove(arrowId);
                continue;
            }
            Entity entity = level.getEntity(arrowId);
            if (entity instanceof AbstractArrow arrow && !arrow.isRemoved()) {
                continue;
            }
            if (!state.armed) {
                if (entity == null) {
                    explosiveArrows.remove(arrowId);
                }
                continue;
            }
            tickExplosiveState(state, level, null);
            if (state.detonated || now >= state.detonationTick) {
                explosiveArrows.remove(arrowId);
            }
        }
    }

    private void tickExplosiveState(ExplosiveArrowState state, ServerLevel level, AbstractArrow arrow) {
        if (state.detonated) {
            return;
        }

        Entity attached = state.attachedEntity == null ? null : level.getEntity(state.attachedEntity);
        if (attached != null && !attached.isRemoved()) {
            state.lastPosition = attached.position().add(0.0D, attached.getBbHeight() * 0.5D, 0.0D);
        } else if (state.lastPosition == null) {
            state.lastPosition = arrow != null ? arrow.position() : Vec3.ZERO;
        }

        long remaining = state.detonationTick - level.getGameTime();
        long particleTick = arrow != null ? arrow.tickCount : level.getGameTime();
        if (remaining > 0L && particleTick % (remaining <= 4L ? 1 : remaining <= 10L ? 2 : 5) == 0) {
            Vec3 position = state.lastPosition;
            level.sendParticles(ParticleTypes.FLAME, position.x, position.y, position.z,
                    remaining <= 4L ? 2 : 1, 0.10D, 0.10D, 0.10D, 0.01D);
        }
        if (remaining <= 0L) {
            state.detonated = true;
            detonateExplosiveArrow(state, level);
            if (arrow != null) {
                arrow.discard();
            }
        }
    }

    private void detonateExplosiveArrow(ExplosiveArrowState state, ServerLevel level) {
        Vec3 center = state.lastPosition == null ? Vec3.ZERO : state.lastPosition;
        double radius = switch (state.level) {
            case 1 -> 2.0D;
            case 2 -> 2.5D;
            default -> 3.0D;
        };
        double explosionPercent = switch (state.level) {
            case 1 -> 0.25D;
            case 2 -> 0.45D;
            default -> 0.65D;
        };
        double knockbackStrength = switch (state.level) {
            case 1 -> 0.25D;
            case 2 -> 0.35D;
            default -> 0.45D;
        };
        Entity owner = level.getEntity(state.owner);
        AABB box = new AABB(center, center).inflate(radius);
        for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, box,
                living -> living.isAlive() && !living.isSpectator() && living != owner)) {
            double distance = target.getBoundingBox().getCenter().distanceTo(center);
            if (distance > radius) {
                continue;
            }
            double distanceMultiplier = 1.0D - 0.65D * Mth.clamp(distance / radius, 0.0D, 1.0D);
            double targetMultiplier = target instanceof Player ? 0.50D : 1.0D;
            if (target.entityTags().contains(EXPLOSIVE_ARROW_BOSS_TAG)) {
                targetMultiplier *= 0.65D;
            }
            float damage = (float) Math.min(state.attackDamage * explosionPercent
                    * distanceMultiplier * targetMultiplier, Float.MAX_VALUE);
            if (damage > 0.0F) {
                DamageSource source = owner != null
                        ? level.damageSources().source(EXPLOSIVE_ARROW_DAMAGE_TYPE, owner)
                        : level.damageSources().source(EXPLOSIVE_ARROW_DAMAGE_TYPE);
                target.hurtServer(level, source, damage);
            }

            Vec3 offset = target.getBoundingBox().getCenter().subtract(center);
            Vec3 horizontal = new Vec3(offset.x, 0.0D, offset.z);
            if (horizontal.lengthSqr() < 1.0E-6D && owner != null) {
                horizontal = target.position().subtract(owner.position());
                horizontal = new Vec3(horizontal.x, 0.0D, horizontal.z);
            }
            if (horizontal.lengthSqr() > 1.0E-6D) {
                double resistance = Mth.clamp(target.getAttributeValue(Attributes.KNOCKBACK_RESISTANCE), 0.0D, 1.0D);
                double directTargetScale = state.attachedEntity != null
                        && state.attachedEntity.equals(target.getUUID()) ? 0.5D : 1.0D;
                double push = knockbackStrength * distanceMultiplier * directTargetScale * (1.0D - resistance);
                Vec3 direction = horizontal.normalize();
                Vec3 current = target.getDeltaMovement();
                target.setDeltaMovement(current.x + direction.x * push,
                        current.y + Math.min(0.12D, push * 0.25D), current.z + direction.z * push);
                target.syncVelocity = true;
            }
        }
        level.sendParticles(ParticleTypes.EXPLOSION, center.x, center.y, center.z,
                1, 0.0D, 0.0D, 0.0D, 0.0D);
        level.sendParticles(ParticleTypes.SMOKE, center.x, center.y, center.z,
                8 + state.level * 6, radius * 0.30D, radius * 0.25D, radius * 0.30D, 0.04D);
        level.playSound(null, BlockPos.containing(center), SoundEvents.GENERIC_EXPLODE.value(),
                SoundSource.PLAYERS, 0.65F, 1.15F);
    }
}
