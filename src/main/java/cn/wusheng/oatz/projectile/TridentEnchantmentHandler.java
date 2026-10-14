package cn.wusheng.oatz.projectile;

import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.arrow.ThrownTrident;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.core.particles.ParticleTypes;
import net.neoforged.neoforge.event.entity.ProjectileImpactEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;

import static cn.wusheng.oatz.enchantment.EnchantmentUtil.enchantmentLevel;
import static cn.wusheng.oatz.enchantment.EnchantmentValues.surgeDamageMultiplier;
import static cn.wusheng.oatz.enchantment.EnchantmentValues.surgeRequiredWaterTicks;
import static cn.wusheng.oatz.registry.ModEnchantments.*;

/** Owns the state of a thrown trident across impact, damage, and return ticks. */
public final class TridentEnchantmentHandler {
    private static final int SURGE_HOLD_TICKS = 20;
    private final Map<UUID, SurgeState> surgeStates = new HashMap<>();

    private static final class SurgeState {
        private int surgeLevel;
        private int tetherLevel;
        private int reboundLevel;
        private int hydroshockLevel;
        private int waterFlightTicks;
        private boolean active;
        private boolean outbound = true;
        private boolean tetherTriggered;
        private boolean shockwaveTriggered;
        private long lastSurgeAdvanceTick = Long.MIN_VALUE;
        private final Set<UUID> returnHitEntities = new HashSet<>();
        private long activeUntil;

        private SurgeState(int surgeLevel, int tetherLevel, int reboundLevel, int hydroshockLevel) {
            this.surgeLevel = surgeLevel;
            this.tetherLevel = tetherLevel;
            this.reboundLevel = reboundLevel;
            this.hydroshockLevel = hydroshockLevel;
        }
    }
    public void onImpact(ThrownTrident trident, ServerLevel level, ProjectileImpactEvent event) {
        capture(trident, level);
        SurgeState state = surgeStates.get(trident.getUUID());
        if (state == null || !state.outbound || state.shockwaveTriggered || state.hydroshockLevel <= 0) {
            return;
        }
        // ProjectileImpactEvent is raised from inside the vanilla projectile
        // tick, before the next EntityTickEvent.Pre. Advance Surge here as
        // well so the impact tick itself cannot be lost at the water boundary.
        if (state.surgeLevel > 0 && isThrownTridentFlightState(trident)) {
            advanceSurgeWaterState(trident, level, state, tridentTouchesWater(trident, level)
                    || level.getFluidState(BlockPos.containing(event.getRayTraceResult().getLocation()))
                    .is(FluidTags.WATER));
        }
        Entity owner = trident.getOwner();
        if (!(owner instanceof LivingEntity)) {
            return;
        }
        Vec3 impact = event.getRayTraceResult().getLocation();
        if (!trident.isInWater()
                && !level.getFluidState(net.minecraft.core.BlockPos.containing(impact)).is(net.minecraft.tags.FluidTags.WATER)) {
            return;
        }
        state.shockwaveTriggered = true;
        triggerHydroshock(trident, level, impact, state.hydroshockLevel);
    }

    public void modifyIncomingDamage(LivingIncomingDamageEvent event) {
        if (event.getSource().getDirectEntity() instanceof ThrownTrident trident
                && trident.level() instanceof ServerLevel level
                && isThrownTridentFlightState(trident)) {
            capture(trident, level);
            SurgeState state = surgeStates.get(trident.getUUID());
            if (state != null && state.active && state.outbound && state.surgeLevel > 0) {
                event.setAmount(event.getAmount() * surgeDamageMultiplier(state.surgeLevel));
            }
        }
    }

    public void afterDamage(LivingDamageEvent.Post event) {
        if (event.getHealthDamage() > 0.0F
                && event.getSource().getDirectEntity() instanceof ThrownTrident trident
                && trident.level() instanceof ServerLevel level) {
            capture(trident, level);
            SurgeState state = surgeStates.get(trident.getUUID());
            if (state != null && state.active && state.outbound && state.surgeLevel > 0) {
                Vec3 motion = trident.getDeltaMovement();
                Vec3 horizontal = new Vec3(motion.x, 0.0D, motion.z);
                if (horizontal.lengthSqr() > 1.0E-6D) {
                    double knockback = switch (state.surgeLevel) {
                        case 1 -> 0.10D;
                        case 2 -> 0.20D;
                        default -> 0.30D;
                    };
                    Vec3 push = horizontal.normalize().scale(knockback);
                    LivingEntity target = event.getEntity();
                    Vec3 current = target.getDeltaMovement();
                    target.setDeltaMovement(current.x + push.x, current.y, current.z + push.z);
                    target.syncVelocity = true;
                }
                level.sendParticles(ParticleTypes.SPLASH, trident.getX(), trident.getY(), trident.getZ(),
                        4 + state.surgeLevel * 4, 0.25D, 0.25D, 0.25D, 0.10D);
                level.playSound(null, trident.blockPosition(), SoundEvents.GENERIC_SPLASH,
                        SoundSource.PLAYERS, 0.55F, 1.0F + state.surgeLevel * 0.05F);
            }
            if (state != null && state.outbound && state.tetherLevel > 0
                    && !state.tetherTriggered && event.getEntity().isAlive()
                    && trident.getOwner() instanceof LivingEntity shooter) {
                state.tetherTriggered = true;
                applyTether(event.getEntity(), shooter, level, state.tetherLevel);
                level.playSound(null, event.getEntity().blockPosition(), SoundEvents.PLAYER_SPLASH,
                        SoundSource.PLAYERS, 0.45F, 1.35F);
            }
        }
    }

    private void triggerHydroshock(ThrownTrident trident, ServerLevel level, Vec3 center, int levelValue) {
        double radius = switch (levelValue) {
            case 1 -> 2.0D;
            case 2 -> 3.0D;
            default -> 4.0D;
        };
        double verticalRange = switch (levelValue) {
            case 1 -> 2.0D;
            case 2 -> 2.5D;
            default -> 3.0D;
        };
        double strength = switch (levelValue) {
            case 1 -> 0.35D;
            case 2 -> 0.50D;
            default -> 0.65D;
        };
        double vertical = switch (levelValue) {
            case 1 -> 0.08D;
            case 2 -> 0.12D;
            default -> 0.16D;
        };
        Entity owner = trident.getOwner();
        AABB box = new AABB(center, center).inflate(radius, verticalRange, radius);
        level.getEntitiesOfClass(LivingEntity.class, box, living -> living.isAlive()
                        && !living.isSpectator() && living != owner)
                .forEach(living -> {
                    Vec3 offset = living.getBoundingBox().getCenter().subtract(center);
                    double horizontalLength = Math.sqrt(offset.x * offset.x + offset.z * offset.z);
                    AABB bounds = living.getBoundingBox();
                    double verticalDistance = center.y < bounds.minY ? bounds.minY - center.y
                            : center.y > bounds.maxY ? center.y - bounds.maxY : 0.0D;
                    if (horizontalLength > radius || verticalDistance > verticalRange) {
                        return;
                    }
                    Vec3 horizontal = new Vec3(offset.x, 0.0D, offset.z);
                    if (horizontalLength < 1.0E-6D && owner != null) {
                        horizontal = living.position().subtract(owner.position()).multiply(1.0D, 0.0D, 1.0D);
                        horizontalLength = horizontal.length();
                    }
                    if (horizontalLength < 1.0E-6D) {
                        horizontal = trident.getDeltaMovement().multiply(1.0D, 0.0D, 1.0D);
                        horizontalLength = horizontal.length();
                    }
                    if (horizontalLength < 1.0E-6D) {
                        return;
                    }
                    double resistance = Mth.clamp(living.getAttributeValue(Attributes.KNOCKBACK_RESISTANCE), 0.0D, 1.0D);
                    double scale = 1.0D - resistance;
                    Vec3 direction = horizontal.scale(1.0D / horizontalLength);
                    Vec3 current = living.getDeltaMovement();
                    living.setDeltaMovement(current.x + direction.x * strength * scale,
                            current.y + vertical * scale, current.z + direction.z * strength * scale);
                    living.syncVelocity = true;
                    if (living instanceof ServerPlayer player) {
                        player.connection.send(new ClientboundSetEntityMotionPacket(player));
                    }
                });
        int particles = switch (levelValue) {
            case 1 -> 10;
            case 2 -> 17;
            default -> 25;
        };
        level.sendParticles(ParticleTypes.SPLASH, center.x, center.y, center.z,
                particles, radius * 0.35D, 0.20D, radius * 0.35D, 0.18D);
        level.sendParticles(ParticleTypes.BUBBLE, center.x, center.y, center.z,
                Math.max(4, particles / 2), radius * 0.25D, 0.20D, radius * 0.25D, 0.08D);
        int wavePoints = 10 + levelValue * 4;
        for (int ring = 1; ring <= 2; ring++) {
            double ringRadius = radius * ring / 2.0D;
            for (int point = 0; point < wavePoints; point++) {
                double angle = (Math.PI * 2.0D * point / wavePoints) + (ring == 2 ? Math.PI / wavePoints : 0.0D);
                double x = center.x + Math.cos(angle) * ringRadius;
                double z = center.z + Math.sin(angle) * ringRadius;
                level.sendParticles(ParticleTypes.SPLASH, x, center.y + 0.08D, z,
                        1, Math.cos(angle) * 0.03D, 0.04D, Math.sin(angle) * 0.03D, 0.02D);
            }
        }
        level.playSound(null, trident.blockPosition(), SoundEvents.GENERIC_SPLASH,
                SoundSource.PLAYERS, 0.7F, 0.85F + levelValue * 0.08F);
    }

    public void tick(ThrownTrident trident, ServerLevel level) {
        ItemStack weapon = tridentItemStack(trident);
        Entity owner = trident.getOwner();
        if (!(owner instanceof LivingEntity shooter)) {
            surgeStates.remove(trident.getUUID());
            return;
        }
        int surgeLevel = enchantmentLevel(shooter, weapon, SURGE_ID);
        int tetherLevel = enchantmentLevel(shooter, weapon, TETHER_ID);
        int reboundLevel = enchantmentLevel(shooter, weapon, REBOUND_ID);
        int hydroshockLevel = enchantmentLevel(shooter, weapon, HYDROSHOCK_ID);
        SurgeState state = surgeStates.get(trident.getUUID());
        if (state == null && surgeLevel <= 0 && tetherLevel <= 0 && reboundLevel <= 0 && hydroshockLevel <= 0) {
            return;
        }
        if (trident.isRemoved()) {
            surgeStates.remove(trident.getUUID());
            return;
        }
        UUID id = trident.getUUID();
        state = surgeStates.computeIfAbsent(id,
                ignored -> new SurgeState(surgeLevel, tetherLevel, reboundLevel, hydroshockLevel));
        // Never overwrite a captured enchantment with zero when vanilla briefly
        // returns an empty stack during projectile state transitions.
        if (surgeLevel > 0) state.surgeLevel = surgeLevel;
        if (tetherLevel > 0) state.tetherLevel = tetherLevel;
        if (reboundLevel > 0) state.reboundLevel = reboundLevel;
        if (hydroshockLevel > 0) state.hydroshockLevel = hydroshockLevel;
        if (state.outbound && trident.noPhysics) {
            state.outbound = false;
            state.active = false;
            state.waterFlightTicks = 0;
        }
        if (!state.outbound) {
            tickRebound(trident, shooter, state, level);
            return;
        }
        if (state.surgeLevel <= 0) {
            return;
        }
        long now = level.getGameTime();
        boolean inWater = tridentTouchesWater(trident, level);
        if (state.active) {
            if (inWater) {
                state.activeUntil = now + SURGE_HOLD_TICKS;
            } else if (now >= state.activeUntil) {
                state.active = false;
                state.waterFlightTicks = 0;
            }
        } else if (inWater) {
            advanceSurgeWaterState(trident, level, state, true);
        } else {
            state.waterFlightTicks = 0;
        }
        if (state.active && trident.tickCount % 2 == 0) {
            int count = state.surgeLevel == 1 ? 1 : 1 + (state.surgeLevel / 3);
            level.sendParticles(ParticleTypes.BUBBLE, trident.getX(), trident.getY(), trident.getZ(),
                    count, 0.12D, 0.12D, 0.12D, 0.03D);
        } else if (!state.active && inWater && trident.tickCount % Math.max(2, 6 - state.surgeLevel) == 0) {
            level.sendParticles(ParticleTypes.BUBBLE, trident.getX(), trident.getY(), trident.getZ(),
                    state.surgeLevel, 0.10D, 0.10D, 0.10D, 0.02D);
        }
    }

    private static boolean isThrownTridentFlightState(ThrownTrident trident) {
        // noPhysics is set by vanilla only after a hit, when Loyalty begins
        // the return path. A melee trident never reaches this entity path.
        return !trident.isRemoved() && !trident.noPhysics;
    }

    private static boolean tridentTouchesWater(ThrownTrident trident, ServerLevel level) {
        if (trident.isInWater()) {
            return true;
        }
        // During the first tick after entering water, the entity fluid flag
        // can still be stale. Check the small voxel volume occupied by the
        // trident so a boundary crossing is observed reliably.
        AABB box = trident.getBoundingBox().inflate(0.05D);
        BlockPos min = BlockPos.containing(box.minX, box.minY, box.minZ);
        BlockPos max = BlockPos.containing(box.maxX, box.maxY, box.maxZ);
        for (int x = min.getX(); x <= max.getX(); x++) {
            for (int y = min.getY(); y <= max.getY(); y++) {
                for (int z = min.getZ(); z <= max.getZ(); z++) {
                    if (level.getFluidState(new BlockPos(x, y, z)).is(FluidTags.WATER)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private static void advanceSurgeWaterState(ThrownTrident trident, ServerLevel level,
            SurgeState state, boolean inWater) {
        if (state.surgeLevel <= 0 || !state.outbound || !isThrownTridentFlightState(trident)) {
            return;
        }
        long now = level.getGameTime();
        if (state.lastSurgeAdvanceTick == now) {
            return;
        }
        state.lastSurgeAdvanceTick = now;
        if (state.active) {
            if (inWater) {
                state.activeUntil = now + SURGE_HOLD_TICKS;
            } else if (now >= state.activeUntil) {
                state.active = false;
                state.waterFlightTicks = 0;
            }
            return;
        }
        if (!inWater) {
            state.waterFlightTicks = 0;
            return;
        }
        state.waterFlightTicks++;
        int required = surgeRequiredWaterTicks(state.surgeLevel);
        if (state.waterFlightTicks >= required) {
            state.active = true;
            state.activeUntil = now + SURGE_HOLD_TICKS;
            level.sendParticles(ParticleTypes.SPLASH, trident.getX(), trident.getY(), trident.getZ(),
                    4 + state.surgeLevel * 4, 0.25D, 0.25D, 0.25D, 0.12D);
            level.playSound(null, trident.blockPosition(), SoundEvents.PLAYER_SPLASH,
                    SoundSource.PLAYERS, 0.45F, 1.1F);
        }
    }

    public void capture(ThrownTrident trident, ServerLevel level) {
        Entity owner = trident.getOwner();
        if (!(owner instanceof LivingEntity shooter)) {
            return;
        }
        ItemStack weapon = tridentItemStack(trident);
        int surge = enchantmentLevel(shooter, weapon, SURGE_ID);
        int tether = enchantmentLevel(shooter, weapon, TETHER_ID);
        int rebound = enchantmentLevel(shooter, weapon, REBOUND_ID);
        int hydroshock = enchantmentLevel(shooter, weapon, HYDROSHOCK_ID);
        if (surge <= 0 && tether <= 0 && rebound <= 0 && hydroshock <= 0) {
            return;
        }
        SurgeState state = surgeStates.computeIfAbsent(trident.getUUID(),
                ignored -> new SurgeState(surge, tether, rebound, hydroshock));
        state.surgeLevel = Math.max(state.surgeLevel, surge);
        state.tetherLevel = Math.max(state.tetherLevel, tether);
        state.reboundLevel = Math.max(state.reboundLevel, rebound);
        state.hydroshockLevel = Math.max(state.hydroshockLevel, hydroshock);
    }

    private static ItemStack tridentItemStack(ThrownTrident trident) {
        ItemStack pickup = trident.getPickupItemStackOrigin();
        if (pickup != null && !pickup.isEmpty()) {
            return pickup;
        }
        ItemStack weapon = trident.getWeaponItem();
        return weapon == null ? ItemStack.EMPTY : weapon;
    }

    private static void applyTether(LivingEntity target, LivingEntity shooter, ServerLevel level, int levelValue) {
        Vec3 offset = shooter.getPosition(1.0F).subtract(target.getPosition(1.0F));
        Vec3 horizontal = new Vec3(offset.x, 0.0D, offset.z);
        double horizontalLength = horizontal.length();
        if (horizontalLength < 1.0E-6D) {
            return;
        }
        double pullStrength = switch (levelValue) {
            case 1 -> 0.35D;
            case 2 -> 0.50D;
            default -> 0.65D;
        };
        double resistance = Mth.clamp(target.getAttributeValue(Attributes.KNOCKBACK_RESISTANCE), 0.0D, 1.0D);
        double scale = 1.0D - resistance;
        Vec3 direction = horizontal.scale(1.0D / horizontalLength);
        double vertical = Mth.clamp(offset.y / Math.max(1.0D, horizontalLength), -0.25D, 0.25D);
        Vec3 current = target.getDeltaMovement();
        target.setDeltaMovement(current.x + direction.x * pullStrength * scale,
                current.y + vertical * scale, current.z + direction.z * pullStrength * scale);
        target.syncVelocity = true;
        int particles = switch (levelValue) {
            case 1 -> 5;
            case 2 -> 8;
            default -> 12;
        };
        level.sendParticles(ParticleTypes.DRIPPING_WATER, target.getX(), target.getY() + target.getBbHeight() * 0.5D,
                target.getZ(), particles, 0.25D, 0.35D, 0.25D, 0.04D);
    }

    private void tickRebound(ThrownTrident trident, LivingEntity shooter, SurgeState state, ServerLevel level) {
        if (state.reboundLevel <= 0 || !(shooter instanceof Player)
                || EnchantmentHelper.getItemEnchantmentLevel(
                        level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT)
                                .getOrThrow(Enchantments.LOYALTY), tridentItemStack(trident)) <= 0) {
            return;
        }
        Vec3 motion = trident.getDeltaMovement();
        if (motion.lengthSqr() < 1.0E-6D) {
            return;
        }
        AABB path = trident.getBoundingBox().expandTowards(motion).inflate(0.35D);
        int maxHits = state.reboundLevel;
        if (state.returnHitEntities.size() >= maxHits) {
            return;
        }
        level.getEntitiesOfClass(LivingEntity.class, path, target -> target != shooter
                        && target.isAlive() && !target.isSpectator()
                        && !state.returnHitEntities.contains(target.getUUID()))
                .stream()
                .sorted(Comparator.comparingDouble(target -> target.distanceToSqr(trident)))
                .limit(maxHits - state.returnHitEntities.size())
                .forEach(target -> {
                    DamageSource source = level.damageSources().trident(trident, shooter);
                    float normalDamage = EnchantmentHelper.modifyDamage(level, tridentItemStack(trident), target, source, 8.0F);
                    float returnMultiplier = switch (state.reboundLevel) {
                        case 1 -> 0.30F;
                        case 2 -> 0.45F;
                        default -> 0.60F;
                    };
                    if (target.hurtServer(level, source, normalDamage * returnMultiplier)) {
                        state.returnHitEntities.add(target.getUUID());
                        level.sendParticles(ParticleTypes.SPLASH, target.getX(), target.getY() + target.getBbHeight() * 0.5D,
                                target.getZ(), 4 + state.reboundLevel * 3, 0.20D, 0.30D, 0.20D, 0.08D);
                        level.sendParticles(ParticleTypes.CRIT, target.getX(), target.getY() + target.getBbHeight() * 0.5D,
                                target.getZ(), 2 + state.reboundLevel, 0.15D, 0.20D, 0.15D, 0.02D);
                        level.playSound(null, target.blockPosition(), SoundEvents.TRIDENT_RETURN,
                                SoundSource.PLAYERS, 0.45F, 1.15F);
                    }
                });
    }

}
